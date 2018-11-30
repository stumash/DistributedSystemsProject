package group25.Server.Common;

import group25.Server.Interface.*;
import group25.Utils.XMLPersistor;

import java.util.*;
import java.rmi.RemoteException;
import java.io.*;

public abstract class AbstractRMHashMapManager {
    private String m_name = "";
    protected RMHashMap globalState;
    protected HashMap<Integer, RMHashMap> transactionStates = new HashMap<>();
    protected XMLPersistor xmlPersistor = new XMLPersistor(RMHashMap.class);
    protected final String filename1, filename2, pointerFile;
    protected String currentCommitFile;
    protected FairWaitInterruptibleLock globalLock = new FairWaitInterruptibleLock();

    // TODO: give lock to correct transaction on wakeup from failure
    public AbstractRMHashMapManager(String p_name, String filename1, String filename2, String pointerFile) {
        System.out.println(filename1);
        this.m_name = p_name;
        this.filename1 = filename1;
        this.filename2 = filename2;
        this.pointerFile = pointerFile;

        currentCommitFile = xmlPersistor.readObject(pointerFile);
        if (currentCommitFile == null) {
            currentCommitFile = filename1;
            globalState = new RMHashMap();
        } else {
            globalState = xmlPersistor.readObject(currentCommitFile);
        }
    }

    public boolean vote(int xid) throws RemoteException {
        if (!transactionExists(xid)) return false;

        // get global lock
        boolean gotLock = globalLock.lock(xid);
        if (!gotLock) return false;

        updateThenPersistGlobalState(xid);
        return true;
    }

    public boolean doCommit(int xid) throws RemoteException {
        // update pointer file
        if (currentCommitFile.equals(filename1)) {
            xmlPersistor.writeObject(filename2, pointerFile);
            currentCommitFile = filename2;
        } else {
            xmlPersistor.writeObject(filename1, pointerFile);
            currentCommitFile = filename1;
        }

        // destroy transaction-specific state
        removeTransactionState(xid);

        // unlock
        globalLock.unlock(xid);

        return true;
    }

    public boolean abort(int xid) throws RemoteException {
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

            for (String i : m_data.keySet()) {
                globalState.put(i, m_data.get(i));
            }

            // remove the transaction-specific state
            removeTransactionState(xid);

            // write to file
            if  (currentCommitFile.equals(filename1)) {
                System.out.println("commit file is " + filename1);
                xmlPersistor.writeObject(globalState, filename2);
            } else if  (currentCommitFile.equals(filename2)) {
                System.out.println("commit file is " + filename2);
                xmlPersistor.writeObject(globalState, filename1);
            } else {
                System.out.println("fuck off");
            }
        }
    }

    public RMItem readData(int xid, String key) {
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
    public void writeData(int xid, String key, RMItem value) {
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

    public boolean deleteItem(int xid, String key) {
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
    public int queryNum(int xid, String key) {
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
    public int queryPrice(int xid, String key) {
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
        System.exit(0);
    }
}
