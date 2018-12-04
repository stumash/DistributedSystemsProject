package group25.Server.Common;

import group25.Server.Interface.*;
import group25.Utils.CrashMode;
import group25.Utils.XMLPersistor;
import static group25.Utils.AnsiColors.RED;
import static group25.Utils.AnsiColors.BLUE;
import static group25.Utils.AnsiColors.GREEN;

import java.util.*;

import javax.transaction.InvalidTransactionException;

import java.rmi.RemoteException;

public abstract class AbstractRMHashMapManager {
    private String m_name = "";
    protected RMHashMap globalState = new RMHashMap();
    protected HashMap<Integer, RMHashMap> transactionStates = new HashMap<>();
    protected XMLPersistor xmlPersistor = new XMLPersistor();
    protected final String filename1, filename2, pointerFile, logFile;
    protected String currentCommitFile;
    protected FairWaitInterruptibleLock globalLock = new FairWaitInterruptibleLock();
    protected IMiddlewareResourceManager middlewareRM;

    private CrashMode crashMode = CrashMode.NO_CRASH;

    public AbstractRMHashMapManager(String p_name, String filename1, String filename2, String pointerFile, String logFile,
            IMiddlewareResourceManager middlewareRM) {
        this.m_name = p_name;
        this.filename1 = filename1;
        this.filename2 = filename2;
        this.pointerFile = pointerFile;
        this.logFile = logFile;
        this.middlewareRM = middlewareRM;
        this.currentCommitFile = filename2;
    }

    public void recover() {
        currentCommitFile = xmlPersistor.readObject(pointerFile);
        if (currentCommitFile == null) {
            currentCommitFile = filename2; // first commit should be to other file, filename1
            globalState = new RMHashMap();
        } else {
            globalState = xmlPersistor.readObject(currentCommitFile);
        }

        Integer transactionVotedYes = xmlPersistor.readObject(logFile);
        if (transactionVotedYes != null && transactionVotedYes != -1) {
            int xid = transactionVotedYes;
            String workingFile = currentCommitFile.equals(filename1)? filename2: filename1;
            transactionStates.put(xid, xmlPersistor.readObject(workingFile));
            globalLock.lock(xid);
            try {
                if (middlewareRM.commited(xid)) {
                    System.out.println("middleware says to commit recovered transaction so committing");
                    doCommit(xid);
                } else {
                    System.out.println("middleware doesn't know about transaction so aborting");
                    abort(xid);
                }
            } catch (Exception e) {
                System.out.println("Failed to complete recovery on doCommit/abort "+xid);
            }
        } else {
            System.out.println("did not record voting yes");
        }
    }

    public void crashResourceManager(CrashMode cm) {
        System.out.println("crash mode " +  cm);
        if (cm.toString().substring(0,2).equals("TM")) {
            System.out.println(RED.colorString("ERROR: ")+"Invalid crash mode for RM: "+cm.toString());
            return;
        }
        System.out.println("setting crash mode");
        crashMode = cm;
    }

    private void crashIf(CrashMode cm) {
        if (crashMode == cm) {
            System.out.println(BLUE.colorString("CRASH: ")+cm.toString());
            shutdown();
        }
    }

    public void vote(int xid) throws RemoteException {
        System.out.println(getName() + " got vote request.");
        xmlPersistor.writeObject(-1, logFile);

        // do this all in a new thread since we want vote() to return immediately in the TM
        new Thread(() -> {
            crashIf(CrashMode.RM_BEFORE_DECIDING_VOTE);
            if (!transactionExists(xid)) {
                try {
                    // TODO log abort
                    crashIf(CrashMode.RM_AFTER_DECIDING_VOTE);
                    System.out.println("Transaction not found. Voting no.");
                    middlewareRM.receiveVote(xid, false, this.m_name);
                    crashIf(CrashMode.RM_AFTER_VOTING);
                } catch (RemoteException e) {
                    System.out.println("Could not send vote to TM");
                }
                return;
            }
    
            boolean gotLock = globalLock.lock(xid);
            if (!gotLock) {
                try {
                    // TODO log abort
                    crashIf(CrashMode.RM_AFTER_DECIDING_VOTE);
                    abort(xid);
                    System.out.println("Could not get global RM persistence lock. Voting no.");
                    middlewareRM.receiveVote(xid, false, this.m_name);
                    crashIf(CrashMode.RM_AFTER_VOTING);
                } catch (RemoteException e) {
                    System.out.println("Could not send vote to TM");
                }
                return;
            }
            
            // all good, we will vote YES
            updateThenPersistGlobalState(xid);

            // TODO log YES
            xmlPersistor.writeObject(xid, logFile);
            crashIf(CrashMode.RM_AFTER_DECIDING_VOTE);

            // uncertainty phase. Send Yes, and wait for decision
            boolean sentVote = false;
            new Thread(() -> {
                System.out.println(GREEN.colorString("Voting yes."));
                try {
                    middlewareRM.receiveVote(xid, true, this.m_name);
                    xmlPersistor.writeObject(xid, logFile);
                    crashIf(CrashMode.RM_AFTER_VOTING);
                    return;
                } catch (InvalidTransactionException ite) {
                    System.out.println("TM says invalid transaction. Abort.");
                    try {
                        abort(xid);
                        return;
                    } catch (RemoteException e) { /* can't happen, do nothing */ }
                } catch (RemoteException re) {
                    System.out.println("Could not send vote to Coordinator. Sending again.");
                    while (true) { // keep sending indefinitely
                        try {
                            System.out.println(GREEN.colorString("Try again to vote yes."));
                            middlewareRM.receiveVote(xid, true, this.m_name);
                            xmlPersistor.writeObject(xid, logFile);
                            crashIf(CrashMode.RM_AFTER_VOTING);
                            return;
                        } catch (InvalidTransactionException ite) {
                            System.out.println("Invalid transaction! Abort.");
                            try {
                                abort(xid);
                                return;
                            } catch (RemoteException e) { }
                        } catch (RemoteException re2) {
                            System.out.println("Could not send vote request to Coordinator. Sending again.");
                        }
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e1) { /* do nothing */}
                    }
                }
            }).start();
        }).start();
    }

    public boolean doCommit(int xid) throws RemoteException {
        new Thread(() -> {
            crashIf(CrashMode.RM_AFTER_RECEIVING_DECISION);
            System.out.println(m_name + " Committing");
            // update pointer file
            if (currentCommitFile.equals(filename1)) {
                xmlPersistor.writeObject(filename2, pointerFile);
                currentCommitFile = filename2;
            } else {
                xmlPersistor.writeObject(filename1, pointerFile);
                currentCommitFile = filename1;
            }

            xmlPersistor.writeObject(-1, logFile);

            // destroy transaction-specific state
            removeTransactionState(xid);

            // unlock
            globalLock.unlock(xid);
        }).start();
        
        return true;
    }

    public boolean abort(int xid) throws RemoteException {
        new Thread(() -> {
            xmlPersistor.writeObject(-1, logFile);
            System.out.println(m_name + " aborting");
            crashIf(CrashMode.RM_AFTER_RECEIVING_DECISION);
            if (globalLock.getLockOwner() == xid) {
                synchronized(globalState) {
                    removeTransactionState(xid);
                    globalState = xmlPersistor.readObject(currentCommitFile);
                    globalLock.unlock(xid);
                }
            } else {
                removeTransactionState(xid);
                globalLock.interruptWaiter(xid);
            }
        }).start();
        
        return true;
    }

    // get state for a particular transaction
    public RMHashMap getTransactionState(int xid) {
        synchronized(transactionStates) {
            return transactionStates.get(xid);
        }
    }

    public boolean transactionExists(int xid) {
        synchronized(transactionStates) {
            return transactionStates.containsKey(xid);
        }
    }

    public void removeTransactionState(int xid) {
        synchronized(transactionStates) {
            transactionStates.remove(xid);
        }
    }

    public void updateThenPersistGlobalState(int xid) {
        synchronized(globalState) {
            // update global state to contain all changes made to transaction-specific state
            RMHashMap m_data = getTransactionState(xid);
            if (m_data == null) return;

            for (String i : globalState.keySet()) {
                if (!m_data.containsKey(i)) {
                    globalState.remove(i);
                }
            }
            for (String i : m_data.keySet()) {
                globalState.put(i, m_data.get(i));
            }

            // remove the transaction-specific state
            removeTransactionState(xid);

            // write to file
            if  (currentCommitFile.equals(filename1)) {
                xmlPersistor.writeObject(globalState, filename2);
            } else if  (currentCommitFile.equals(filename2)) {
                xmlPersistor.writeObject(globalState, filename1);
            } 
        }
    }

    public RMItem readData(int xid, String key) throws UnsupportedOperationException {
        if (globalLock.getLockOwner() == xid)
            throw new UnsupportedOperationException();

        RMHashMap m_data = getTransactionState(xid);
        if (m_data == null) {
            synchronized(globalState) {
                synchronized(transactionStates) {
                    transactionStates.put(xid, globalState.clone());
                }
            }
            m_data = getTransactionState(xid);
        }
        synchronized (m_data) {
            RMItem item = m_data.get(key);
            if (item != null) {
                return (RMItem) item.clone();
            }
            return null;
        }
    }

    // Writes a data item
    public void writeData(int xid, String key, RMItem value) throws UnsupportedOperationException {
        if (globalLock.getLockOwner() == xid)
            throw new UnsupportedOperationException();

        RMHashMap m_data = getTransactionState(xid);
        if (m_data == null) {
            synchronized(globalState) {
                synchronized(transactionStates) {
                    transactionStates.put(xid, globalState.clone());
                }
            }
        }
        m_data = getTransactionState(xid);
        synchronized (m_data) {
            m_data.put(key, value);
        }
    }

    // Remove the item out of storage
    public void removeData(int xid, String key) {
        RMHashMap m_data = getTransactionState(xid);
        synchronized (m_data) {
            m_data.remove(key);
        }
    }

    public boolean deleteItem(int xid, String key) throws UnsupportedOperationException {
        if (globalLock.getLockOwner() == xid)
            throw new UnsupportedOperationException();

        Trace.info("RM::deleteItem(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(xid, key);
        // Check if there is such an item in the storage
        if (curObj == null) {
            Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
            return false;
        } else {
            if (curObj.getReserved() == 0) {
                removeData(xid, curObj.getKey());
                Trace.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
                return true;
            } else {
                Trace.info("RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
                return false;
            }
        }
    }

    // Query the number of available seats/rooms/cars
    public int queryNum(int xid, String key) throws UnsupportedOperationException {
        Trace.info("RM::queryNum(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(xid, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getCount();
        }
        Trace.info("RM::queryNum(" + xid + ", " + key + ") returns count=" + value);
        return value;
    }

    // Query the price of an item
    public int queryPrice(int xid, String key) throws UnsupportedOperationException {
        Trace.info("RM::queryPrice(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(xid, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getPrice();
        }
        Trace.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
        return value;
    }

    public String getName() throws RemoteException {
        return m_name;
    }

    public void shutdown() {
        System.out.println(BLUE.colorString(m_name + " is shutting down."));
        System.exit(0);
    }
    
}
