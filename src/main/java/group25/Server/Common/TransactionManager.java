package group25.Server.Common;

import group25.Server.Interface.*;
import group25.Server.LockManager.DeadlockException;
import group25.Server.LockManager.LockManager;
import group25.Server.LockManager.TransactionLockObject.LockType;
import group25.Utils.CrashMode;
import static group25.Utils.AnsiColors.RED;
import static group25.Utils.AnsiColors.BLUE;
import static group25.Utils.AnsiColors.GREEN;


import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.transaction.InvalidTransactionException;

public class TransactionManager implements Remote
{
    private final long TRANSACTION_MAX_AGE_MILLIS = 60*1000;

    private LockManager lockManager;
    private ICarResourceManager carRM;
    private IFlightResourceManager flightRM;
    private IRoomResourceManager roomRM;
    private ICustomerResourceManager customerRM;
    private int transactionCounter = 0;

    private HashMap<Integer, Semaphore> voteReplyWaitMap = new HashMap<>();

    private CrashMode crashMode = CrashMode.NO_CRASH;

    private HashMap<Integer, ArrayList<Name_RM_Vote>> resourceManagerRecorder = new HashMap<>();

    private HashMap<Integer, Long> transactionAges = new HashMap<>();

    public TransactionManager(
            ICarResourceManager carRM,
            IFlightResourceManager flightRM,
            IRoomResourceManager roomRM,
            ICustomerResourceManager customerRM
    ) {
        lockManager = new LockManager();
        this.carRM = carRM;
        this.flightRM = flightRM;
        this.roomRM = roomRM;
        this.customerRM = customerRM;

        setUpTimeToLiveThread();
    }

    private void crashIf(CrashMode cm) {
        if (crashMode == cm) {
            System.out.println(BLUE.colorString("CRASH: ")+cm.toString());
            shutdown();
        }
    }

    public void crashMiddleware(int mode) {
        CrashMode cm = CrashMode.values()[mode];
        if (cm.toString().substring(0,2).equals("RM")) {
            System.out.println(RED.colorString("Error: ")+"Invalid crash mode chosen for TM: "+cm.toString());
            return;
        }
        crashMode = cm;
        System.out.println("new crash mode" + crashMode);
    }

    public void crashResourceManager(String rmName, int mode) throws RemoteException {
        CrashMode cm = CrashMode.values()[mode];
        if (cm.toString().substring(0,2).equals("TM")) {
            System.out.println(RED.colorString("Error: ")+"Invalid crash mode chosen for RM: "+cm.toString());
            return;
        }
        switch(rmName) {
            case "car":
                ((IAbstractRMHashMapManager) carRM).crashResourceManager(cm);
                break;
            case "flight":
                ((IAbstractRMHashMapManager) flightRM).crashResourceManager(cm);
                break;
            case "room":
                ((IAbstractRMHashMapManager) roomRM).crashResourceManager(cm);
                break;
            case "customer":
                ((IAbstractRMHashMapManager) customerRM).crashResourceManager(cm);
                break;
            default:
                System.out.println(RED.colorString("Error: ")+"Invalid RM name in TransactionManager::crashResourceManager()");
                break;
        }
    }

    private void setUpTimeToLiveThread() {
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        sleep(TRANSACTION_MAX_AGE_MILLIS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    synchronized(transactionAges) {
                        for (Integer xid : transactionAges.keySet()) {
                            if (System.currentTimeMillis() - transactionAges.get(xid)  > TRANSACTION_MAX_AGE_MILLIS) {
                                try {
                                    abort(xid);
                                } catch (RemoteException e) {
                                    // do nothing
                                }
                            }
                        }
                    }
                }
            }
        }.start();
    }

    private boolean updateTransactionAge(int xid) {
        synchronized(transactionAges) {
            if (!transactionAges.containsKey(xid)) {
                return false;
            }
            transactionAges.put(xid, System.currentTimeMillis());
        }
        return true;
    }

    public synchronized int start() throws RemoteException {
        transactionCounter++;
        resourceManagerRecorder.put(transactionCounter, new ArrayList<>());
        synchronized(transactionAges) {
            transactionAges.put(transactionCounter, System.currentTimeMillis());
        }
        return transactionCounter;
    }

    public synchronized boolean commit(int xid) throws InvalidTransactionException, RemoteException {
        synchronized(transactionAges) {
            if (transactionAges.get(xid) == null) {
                throw new InvalidTransactionException();
            }
        }

        // ------------- HERE LIES 2PC -------------
        ArrayList<Name_RM_Vote> resourceManagers;
        synchronized(resourceManagerRecorder) {
            resourceManagers = resourceManagerRecorder.get(xid);    
        }

        crashIf(CrashMode.TM_BEFORE_VOTE_REQUEST);
        synchronized(resourceManagers) {
            for (Name_RM_Vote name_rm_vote : resourceManagers) {
                try {
                    name_rm_vote.rm.vote(xid);
                } catch (RemoteException e) {
                    // if RM doesn't receive vote() call, it's down and therefore has lost all transaction state
                    // So, just abort.
                    System.out.println("Could not send vote request to " + name_rm_vote.rmName + ".Aborting.");
                    abort(xid);
                    return false;
                }
            }   
        }
        
        crashIf(CrashMode.TM_BEFORE_ANY_VOTE_REPLIES);
        Semaphore sem = new Semaphore(1);
        synchronized(voteReplyWaitMap) {
            voteReplyWaitMap.put(xid, sem);
        }
        try {
            sem.acquire(); // wait to be awoken by receiveVote()
            boolean receivedAllVotes = sem.tryAcquire(25, TimeUnit.SECONDS); // all participants must respond within 25 seconds
            if (!receivedAllVotes) {
                System.out.println("Timed out waiting for votes. Aborting");
                abort(xid, true);
            }
        } catch (Exception e) { /* do nothing*/ }

        synchronized(voteReplyWaitMap) {
            // if aborted at this point, map will not contain key xid
            if (!voteReplyWaitMap.containsKey(xid)) {
                return false;
            } else {
                return true;
            }
        }
    }
    
    public void receiveVote(int xid, boolean voteYes, String rmName) throws InvalidTransactionException {      
        synchronized(transactionAges) {
            if (transactionAges.get(xid) == null) {
                throw new InvalidTransactionException();
            }
        }

        if (!voteYes) {
            try {
                // TODO log ABORT
                System.out.println("Received NO vote from " + rmName + ". Aborting");
                crashIf(CrashMode.TM_BEFORE_SENDING_DECISION);
                abort(xid, true);
                return;
            } catch (InvalidTransactionException e) {
                return;
            } catch (RemoteException e) { /* do nothing */ }
        }
    
        new Thread(() -> {
            ArrayList<Name_RM_Vote> resourceManagers;
            synchronized(resourceManagerRecorder) {
                resourceManagers = resourceManagerRecorder.get(xid);
            }
    
            Semaphore sem = null; // this semaphore lets commit() complete
            synchronized(voteReplyWaitMap) {
                sem = voteReplyWaitMap.get(xid);
            }
    
            synchronized(resourceManagers) {
                int numRMs = resourceManagers.size();
                int yesCount = 0;
                for (Name_RM_Vote name_rm_vote : resourceManagers) {
                    if (name_rm_vote.rmName.equals(rmName)) {
                        name_rm_vote.votedYes = true;
                    }
                    if (name_rm_vote.votedYes != null && name_rm_vote.votedYes) {
                        yesCount++;
                        if (yesCount == 2) {
                            crashIf(CrashMode.TM_BEFORE_SOME_VOTE_REPLIES); // crash at second yesVote recieved
                        }
                    }
                }
                if (yesCount == numRMs) {
                    System.out.println(GREEN.colorString("Success: ") + "received all yesses.");
                    crashIf(CrashMode.TM_BEFORE_DECIDING);
                    // TODO log COMMIT. 
                    // This is the point of no return.
                    synchronized(transactionAges) {
                        // finally kill transaction state
                        if (transactionAges.remove(xid) == null) {
                            System.out.println("Transaction not found - Whoopsie");
                            return; // probably shouldn't happen
                        }
                    }
                    crashIf(CrashMode.TM_BEFORE_SENDING_DECISION);
    
                    for (int i=0; i<resourceManagers.size(); i++) {
                        if (i == resourceManagers.size() - 1) {
                            crashIf(CrashMode.TM_BEFORE_SENDING_LAST_DECISION);                    
                        }
    
                        Name_RM_Vote name_rm_vote = resourceManagers.get(i);
                        try {
                            name_rm_vote.rm.doCommit(xid);
                        } catch (RemoteException e) {
                            System.out.println("Could not send commit decision to " + name_rm_vote.rmName + ". Sending commit decision again.");
                            new Thread(() -> {
                                long startTime = System.currentTimeMillis();
                                boolean commited = false;
                                while (System.currentTimeMillis() - startTime < 10 * 1000) {
                                    try {
                                        name_rm_vote.rm.doCommit(xid);
                                        commited = true;
                                        break;
                                    } catch (RemoteException re) { 
                                        System.out.println("Could not send commit decision to " + name_rm_vote.rmName + ". Sending commit decision again.");
                                    }
                                    try {
                                        Thread.sleep(2000);
                                    } catch (InterruptedException e1) { /* do nothing */ }
                                }
                                if (!commited) {
                                    System.out.println("Could not send commit decision to " + name_rm_vote + ". Giving up.");
                                }
                            }).start();
                        } 
                    }
                    crashIf(CrashMode.TM_AFTER_SENDING_ALL_DECISIONS);
                    lockManager.UnlockAll(xid);
                    resourceManagerRecorder.remove(xid);
                    sem.release();
                }
            }
            //  ------------- HERE ENDS 2PC -------------
            return;
        }).start();
    }

  
    private boolean abort(int xid) throws InvalidTransactionException, RemoteException  {
        ArrayList<Name_RM_Vote> resourceManagers = null;
        Semaphore sem = null;

        synchronized(resourceManagerRecorder) {
            resourceManagers = resourceManagerRecorder.get(xid);
        }
        synchronized(resourceManagers) {
            for (int i=0; i<resourceManagers.size(); i++) {
                if (i == resourceManagers.size() - 1) {
                    crashIf(CrashMode.TM_BEFORE_SENDING_LAST_DECISION);                    
                }
                Name_RM_Vote name_rm_vote = resourceManagers.get(i);
                try {
                    name_rm_vote.rm.abort(xid);
                } catch (Exception e) {
                    System.out.println("abort call failed on " + name_rm_vote.rmName);
                }
            }
            crashIf(CrashMode.TM_AFTER_SENDING_ALL_DECISIONS);
        }
        synchronized(resourceManagerRecorder) {
            resourceManagerRecorder.remove(xid); // this ensures that the commit call will return false
        }
        synchronized(voteReplyWaitMap) {
            sem = voteReplyWaitMap.get(xid);
            voteReplyWaitMap.remove(xid);
            if (sem != null) {
                sem.release();
            }
        }
        lockManager.UnlockAll(xid);
        Trace.info("TransactionRM::abort("+xid+")");
        return true;
    }

    public boolean abort(int xid, boolean fromCommit) throws InvalidTransactionException, RemoteException {
        if (fromCommit) {
            return abort(xid);
        } else {
            synchronized(transactionAges) {
                if (transactionAges.remove(xid) == null) throw new InvalidTransactionException();
            }
            return abort(xid);
        }
    }

    private void addResourceManagerToTransaction(int xid, IAbstractRMHashMapManager rm) throws RemoteException {
        synchronized(resourceManagerRecorder) {
            ArrayList<Name_RM_Vote> rmsForXid = resourceManagerRecorder.get(xid);
            Name_RM_Vote pair = new Name_RM_Vote(rm.getName(), rm);
            if (rmsForXid.indexOf(pair) == -1) {
                rmsForXid.add(pair);
            }
        }
    }

    // before any write, we should create a key that will represent the data we are writing
    public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice)
            throws RemoteException, DeadlockException, InvalidTransactionException {
        if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKey = Flight.getKey(flightNum);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) flightRM);
                return flightRM.addFlight(xid, flightNum, flightSeats, flightPrice);
            }
        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::addFlight("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::addFlight("+xid+","+dataKey+") - DeadlockException!");
        }
        return false;
    }

    public boolean addCars(int xid, String location, int numCars, int price)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKey = Car.getKey(location);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) carRM);
                return carRM.addCars(xid, location, numCars, price);
            }
        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::addCars("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::addCars("+xid+","+dataKey+") - DeadlockException!");
        }
        return false;
    }

    public boolean addRooms(int xid, String location, int numRooms, int price)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKey = Room.getKey(location);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) roomRM);
                return roomRM.addRooms(xid, location, numRooms, price);
            }
        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::addRooms("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::addRooms("+xid+","+dataKey+") - DeadlockException!");
        }
        return false;
    }

    public int newCustomer(int xid)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        int cid = customerRM.getNewCustomerId(xid);
        String dataKey = Customer.getKey(cid);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) customerRM);
                if (customerRM.newCustomer(xid, cid)) {
                    return cid;
                }
            }
        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::newCustomer("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::newCustomer("+xid+","+dataKey+") - DeadlockException!");
        }
        return -1;
    }

    public boolean newCustomer(int xid, int cid)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKey = Customer.getKey(cid);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) customerRM);
                return customerRM.newCustomer(xid, cid);
            }
        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::newCustomer("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::newCustomer("+xid+","+dataKey+") - DeadlockException!");
        }
        return false;
    }

    public boolean deleteFlight(int xid, int flightNum)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKey = Flight.getKey(flightNum);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                if (flightRM.readData(xid, dataKey) == null) { // deleting an item that isn't there
                    abort(xid, false);
                    return false;
                }
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) flightRM);
                return flightRM.deleteFlight(xid, flightNum);
            }
        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::addFlight("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::deleteFlight("+xid+","+dataKey+") - DeadlockException!");
        }
        return false;
    }

    public boolean deleteCars(int xid, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKey = Car.getKey(location);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                if (carRM.readData(xid, dataKey) == null) { // deleting an item that isn't there
                    abort(xid, false);
                    return false;
                }
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) carRM);
                return carRM.deleteCars(xid, location);
            }
        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::deleteCars("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::deleteCars("+xid+","+dataKey+") - DeadlockException!");
        }
        return false;
    }

    public boolean deleteRooms(int xid, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKey = Room.getKey(location);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                if (roomRM.readData(xid, dataKey) == null) { // deleting an item that isn't there
                    abort(xid, false);
                    return false;
                }
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) roomRM);
                return roomRM.deleteRooms(xid, location);
            }
        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::deleteRooms("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::deleteRooms("+xid+","+dataKey+") - DeadlockException!");
        }
        return false;
    }

    public boolean deleteCustomer(int xid, int customerID)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKey = Customer.getKey(customerID);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                if (customerRM.readData(xid, dataKey) == null) { // deleting an item that isn't there
                    abort(xid, false);
                    return false;
                }
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) customerRM);
                return customerRM.deleteCustomer(xid, customerID);
            }
        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::deleteCustomer("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::deleteCustomer("+xid+","+dataKey+") - DeadlockException!");
        }
        return false;
    }

    public int queryFlight(int xid, int flightNumber)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKey = Flight.getKey(flightNumber);
        try {
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_READ);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_READ+") - Bad input!");
            } else {
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) flightRM);
                return flightRM.queryFlight(xid, flightNumber);
            }

        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::queryFlight("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::queryFlight("+xid+","+dataKey+") - DeadlockException!");
        }
        return -1;
    }

    public int queryCars(int xid, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKey = Car.getKey(location);
        try {
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_READ);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_READ+") - Bad input!");
            } else {
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) carRM);
                return carRM.queryCars(xid, location);
            }

        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::queryCars("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::queryCars("+xid+","+dataKey+") - DeadlockException!");
        }
        return -1;
    }

    public int queryRooms(int xid, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKey = Room.getKey(location);
        try {
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_READ);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_READ+") - Bad input!");
            } else {
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) roomRM);
                return roomRM.queryRooms(xid, location);
            }

        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::queryRooms("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::queryRooms("+xid+","+dataKey+") - DeadlockException!");
        }
        return -1;
    }

    public String queryCustomerInfo(int xid, int customerID)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKey = Customer.getKey(customerID);
        try {
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_READ);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_READ+") - Bad input!");
            } else {
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) customerRM);
                return customerRM.queryCustomerInfo(xid, customerID);
            }

        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::queryCustomerInfo("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::queryCustomerInfo("+xid+","+dataKey+") - DeadlockException!");
        }
        return "";
    }

    public int queryFlightPrice(int xid, int flightNumber)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKey = Flight.getKey(flightNumber);
        try {
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_READ);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_READ+") - Bad input!");
            } else {
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) flightRM);
                return flightRM.queryFlightPrice(xid, flightNumber);
            }
        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::queryFlightPrice("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::queryFlightPrice("+xid+","+dataKey+") - DeadlockException!");
        }
        return -1;
    }

    public int queryCarsPrice(int xid, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKey = Car.getKey(location);
        try {
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_READ);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_READ+") - Bad input!");
            } else {
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) carRM);
                return carRM.queryCarsPrice(xid, location);
            }
        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::queryCarsPrice("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::queryCarsPrice("+xid+","+dataKey+") - DeadlockException!");
        }
        return -1;
    }

    public int queryRoomsPrice(int xid, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKey = Room.getKey(location);
        try {
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_READ);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_READ+") - Bad input!");
            } else {
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) roomRM);
                return roomRM.queryRoomsPrice(xid, location);
            }
        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::queryRoomsPrice("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::queryRoomsPrice("+xid+","+dataKey+") - DeadlockException!");
        }
        return -1;
    }

    public boolean reserveFlight(int xid, int customerID, int flightNumber)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKeyFlight = Flight.getKey(flightNumber);
        String dataKeyCustomer = Customer.getKey(customerID);
        try { 
            boolean gotLockFlight = lockManager.Lock(xid, dataKeyFlight, LockType.LOCK_WRITE);
            boolean gotLockCustomer = lockManager.Lock(xid, dataKeyCustomer, LockType.LOCK_WRITE);
            if (!(gotLockFlight && gotLockCustomer)) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKeyFlight+","+LockType.LOCK_WRITE+") - Bad input!\nOR\n");
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKeyCustomer+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) flightRM);
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) customerRM);
                return flightRM.reserveFlight(xid, customerID, flightNumber);  // TODO: reserveXXX() methods SHOULD ABORT ON FAILURE!!!
            }
        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::reserveFlight("+xid+","+dataKeyFlight+","+dataKeyCustomer+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::reserveFlight("+xid+","+dataKeyFlight+","+dataKeyCustomer+") - DeadlockException!");
        }
        return false;
    }

    public boolean reserveCar(int xid, int customerID, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKeyCar = Car.getKey(location);
        String dataKeyCustomer = Customer.getKey(customerID);
        try { 
            boolean gotLockFlight = lockManager.Lock(xid, dataKeyCar, LockType.LOCK_WRITE);
            boolean gotLockCustomer = lockManager.Lock(xid, dataKeyCustomer, LockType.LOCK_WRITE);
            if (!(gotLockFlight && gotLockCustomer)) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKeyCar+","+LockType.LOCK_WRITE+") - Bad input!\nOR\n");
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKeyCustomer+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) carRM);
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) customerRM);
                return carRM.reserveCar(xid, customerID, location); // TODO: reserveXXX() methods SHOULD ABORT ON FAILURE!!!
            }
        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::reserveCar("+xid+","+dataKeyCar+","+dataKeyCustomer+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::reserveCar("+xid+","+dataKeyCar+","+dataKeyCustomer+") - DeadlockException!");
        }
        return false;
    }

    public boolean reserveRoom(int xid, int customerID, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException {
		if (!updateTransactionAge(xid)) throw new InvalidTransactionException();
        String dataKeyRoom = Room.getKey(location);
        String dataKeyCustomer = Customer.getKey(customerID);
        try { 
            boolean gotLockFlight = lockManager.Lock(xid, dataKeyRoom, LockType.LOCK_WRITE);
            boolean gotLockCustomer = lockManager.Lock(xid, dataKeyCustomer, LockType.LOCK_WRITE);
            if (!(gotLockFlight && gotLockCustomer)) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKeyRoom+","+LockType.LOCK_WRITE+") - Bad input!\nOR\n");
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKeyCustomer+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) flightRM);
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) customerRM);
                return roomRM.reserveRoom(xid, customerID, location); // TODO: reserveXXX() methods SHOULD ABORT ON FAILURE!!!
            }
        } catch (DeadlockException e) {
            abort(xid, false);
            Trace.info("TransactionRM::reserveRoom("+xid+","+dataKeyRoom+","+dataKeyCustomer+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::reserveRoom("+xid+","+dataKeyRoom+","+dataKeyCustomer+") - DeadlockException!");
        }
        return false;
    }

    public void shutdown() {
        System.out.println(BLUE.colorString("Coordinator shutting down"));
        System.exit(1);
    }

    public boolean shutdownAllResourceManagers(){
        try {
            carRM.shutdown();
        } catch (RemoteException e) {
            // do nothing
        }
        try {
            flightRM.shutdown();
        } catch (RemoteException e) {
            // do nothing
        }
        try {
            roomRM.shutdown();
        } catch (RemoteException e) {
            // do nothing
        }
        try {
            customerRM.shutdown();

        } catch (RemoteException e) {
            // do nothing
        }
        return true;
    }
}

class BeforeImage {
    IAbstractRMHashMapManager rm;
    String dataKey;
    RMItem rItem;

    BeforeImage(IAbstractRMHashMapManager rm, String dataKey, RMItem rItem) {
        this.rm = rm;
        this.dataKey = dataKey;
        this.rItem = rItem;
    }

    void restore(int xid) throws RemoteException {
        if (rItem == null) {
            rm.removeData(xid, dataKey);
        } else {
            rm.writeData(xid, dataKey, rItem);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BeforeImage)) {
            return false;
        }
        BeforeImage bOther = (BeforeImage) other;
        boolean isEqual = bOther.dataKey.equals(this.dataKey);
        return isEqual;
    }
}

class Name_RM_Vote {
    String rmName;
    IAbstractRMHashMapManager rm;
    Boolean votedYes;

    Name_RM_Vote(String rmName, IAbstractRMHashMapManager rm) {
        this.rmName = rmName;
        this.rm = rm;
        votedYes = null;
    }
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Name_RM_Vote)) {
            return false;
        }
        Name_RM_Vote pOther = (Name_RM_Vote) other;
        return rmName.equals(pOther.rmName);
    }
}