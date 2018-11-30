package group25.Server.Common;

import group25.Server.Interface.*;
import group25.Server.LockManager.DeadlockException;
import group25.Server.LockManager.LockManager;
import group25.Server.LockManager.TransactionLockObject.LockType;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

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

    private HashMap<Integer, ArrayList<BeforeImage>> writeRecorder = new HashMap<>();
    // create some data structure that can map XID to RM's it uses, as well as before images. This is how we maintain active transactions
    // keep an array list of these objects
    // on commit, remove this transaction. On abort, keep it. 

    private HashMap<Integer, ArrayList<Pair<String, IAbstractRMHashMapManager>>> resourceManagerRecorder = new HashMap<>();

    private HashMap<Integer, Long> transactionAges = new HashMap<>();
    // for each transaction that is currently active, store the most recent time an action was performed as part of the transaction.
    // this will be useful for 'time-to-live' calculations. when the time associated to the transaction is more than TRANSACTION_MAX_AGE_MILLIS old,
    // we will abort the transaction from a separate thread.

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
        resourceManagerRecorder.put(transactionCounter, new ArrayList<Pair<String, IAbstractRMHashMapManager>>());
        synchronized(transactionAges) {
            transactionAges.put(transactionCounter, System.currentTimeMillis());
        }
        return transactionCounter;
    }



    public synchronized boolean commit(int xid) throws InvalidTransactionException, RemoteException {
        synchronized(transactionAges) {
            if (transactionAges.remove(xid) == null) throw new InvalidTransactionException();
        }

        // HERE LIES 2PC
        ArrayList<Pair<String, IAbstractRMHashMapManager>> resourceManagers;
        synchronized(resourceManagerRecorder) {
            resourceManagers = resourceManagerRecorder.get(xid);
        }
        synchronized(resourceManagers) {
            boolean anyNoVotes = false;
            for (Pair<String, IAbstractRMHashMapManager> rmPair : resourceManagers) {
                boolean votedYes = rmPair.t2.vote(xid);
                if (!votedYes) {
                    anyNoVotes = true;
                    break;
                }
            }
            if (anyNoVotes) {
                abort(xid);
            }
        }
        synchronized(resourceManagers) {
            for (Pair<String, IAbstractRMHashMapManager> rmPair : resourceManagers) {
                rmPair.t2.doCommit(xid);
            }
        }
        // HERE ENDS 2PC

        lockManager.UnlockAll(xid);
        resourceManagerRecorder.remove(xid);
        return true;
    }

    /**
     * Abort transaction associated with xid by
     * - unlocking all associated locks
     * - restoring all data to associated beforeImages(s)
     * 
     * @param xid
     */
    public boolean abort(int xid) throws InvalidTransactionException, RemoteException  {
        synchronized(transactionAges) {
            if (transactionAges.remove(xid) == null) throw new InvalidTransactionException();
        }
        synchronized(resourceManagerRecorder) {
            ArrayList<Pair<String, IAbstractRMHashMapManager>> resourceManagers = resourceManagerRecorder.get(xid);
            for (Pair<String, IAbstractRMHashMapManager> rmPair : resourceManagers) {
                rmPair.t2.abort(xid);
            }
            resourceManagerRecorder.remove(xid);
        }
        lockManager.UnlockAll(xid);
        Trace.info("TransactionRM::abort("+xid+")");
        return true;
    }

    /**
     * record the before images of data before updating it. only do so for data
     * whose before image is not already recorded.
     * 
     * @param xid
     * @param dataKey
     * @throws RemoteException
     */
    private void setUpBeforeImage(int xid, IAbstractRMHashMapManager rm, String dataKey) throws RemoteException {
        synchronized(writeRecorder) {
            ArrayList<BeforeImage> beforeImagesForXid = writeRecorder.get(xid);
            // flights been deleted, this will return null
            RMItem rItem = (RMItem) rm.readData(xid, dataKey); // this returns a clone
            // make a new before image. and this won't be stored because the rItem is new!
            BeforeImage beforeImage = new BeforeImage(rm, dataKey, rItem);
            if (beforeImagesForXid.indexOf(beforeImage) == -1) { // not already stored
                beforeImagesForXid.add(beforeImage);
            }
        } 
    }

    private void addResourceManagerToTransaction(int xid, IAbstractRMHashMapManager rm) throws RemoteException {
        synchronized(resourceManagerRecorder) {
            ArrayList<Pair<String, IAbstractRMHashMapManager>> rmsForXid = resourceManagerRecorder.get(xid);
            Pair<String, IAbstractRMHashMapManager> pair = new Pair<>(rm.getName(), rm);
            if (!rmsForXid.contains(pair)) {
                rmsForXid.add(pair);
                rmsForXid.sort(new Comparator<Pair<String, IAbstractRMHashMapManager>>() {
                    @Override
                    public int compare(Pair<String, IAbstractRMHashMapManager> p1, Pair<String, IAbstractRMHashMapManager> p2) {
                        return p1.t1.compareTo(p2.t1);
                    }
                });
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
            abort(xid);
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
            abort(xid);
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
            abort(xid);
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
            abort(xid);
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
            abort(xid);
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
                    abort(xid);
                    return false;
                }
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) flightRM);
                return flightRM.deleteFlight(xid, flightNum);
            }
        } catch (DeadlockException e) {
            abort(xid);
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
                    abort(xid);
                    return false;
                }
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) carRM);
                return carRM.deleteCars(xid, location);
            }
        } catch (DeadlockException e) {
            abort(xid);
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
                    abort(xid);
                    return false;
                }
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) roomRM);
                return roomRM.deleteRooms(xid, location);
            }
        } catch (DeadlockException e) {
            abort(xid);
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
                    abort(xid);
                    return false;
                }
                addResourceManagerToTransaction(xid, (IAbstractRMHashMapManager) customerRM);
                return customerRM.deleteCustomer(xid, customerID);
            }
        } catch (DeadlockException e) {
            abort(xid);
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
            abort(xid);
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
            abort(xid);
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
            abort(xid);
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
            abort(xid);
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
            abort(xid);
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
            abort(xid);
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
            abort(xid);
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
            abort(xid);
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
            abort(xid);
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
            abort(xid);
            Trace.info("TransactionRM::reserveRoom("+xid+","+dataKeyRoom+","+dataKeyCustomer+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::reserveRoom("+xid+","+dataKeyRoom+","+dataKeyCustomer+") - DeadlockException!");
        }
        return false;
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

class Pair<T1, T2> {
    T1 t1;
    T2 t2;
    Pair(T1 t1, T2 t2) {
        this.t1 = t1;
        this.t2 = t2;
    }
    public boolean equals(Pair<T1, T2> other) {
        return t1.equals(other.t1);
    }
}