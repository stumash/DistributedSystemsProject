package group25.Server.Common;

import group25.Server.Interface.IResourceManager;
import group25.Server.Interface.ICarResourceManager;
import group25.Server.Interface.IFlightResourceManager;
import group25.Server.Interface.IRoomResourceManager;
import group25.Server.Interface.ICustomerResourceManager;
import group25.Server.LockManager.DeadlockException;
import group25.Server.LockManager.LockManager;
import group25.Server.LockManager.RedundantLockRequestException;
import group25.Server.LockManager.TransactionLockObject.LockType;
import group25.Server.TCP.AbstractProxyObject;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.sql.rowset.spi.SyncResolver;

public class TransactionManager implements Remote
{
    private LockManager lockManager;
    private ICarResourceManager carRM;
    private IFlightResourceManager flightRM;
    private IRoomResourceManager roomRM;
    private ICustomerResourceManager customerRM;
    private int transactionCounter;

    private HashMap<Integer, ArrayList<BeforeImage>> writeRecorder = new HashMap<>();
    // create some data structure that can map XID to RM's it uses, as well as before images. This is how we maintain active transactions
    // keep an array list of these objects
    // on commit, remove this transaction. On abort, keep it. 
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
        transactionCounter = 0;
    }

    /**
     * Abort transaction associated with xid by
     * - unlocking all associated locks
     * - restoring all data to associated beforeImages(s)
     * 
     * @param xid
     */
    private void abort(int xid)  {
        synchronized(writeRecorder) {
            ArrayList<BeforeImage> beforeImages = writeRecorder.get(xid);
            for (BeforeImage beforeImage : beforeImages) {
                beforeImage.restore(xid);
            }
            writeRecorder.remove(xid);
        }
        lockManager.UnlockAll(xid);
        Trace.info("TransactionRM::abort("+xid+")");
    }

    /**
     * record the before images of data before updating it. only do so for data whose before image
     * is not already recorded.
     * 
     * @param xid
     * @param dataKey
     */
    private void setUpBeforeImage(int xid, AbstractRMHashMapManager rm, String dataKey) {
        synchronized(writeRecorder) {
            ArrayList<BeforeImage> beforeImagesForXid = writeRecorder.get(xid);
            ReservableItem rItem = (ReservableItem) rm.readData(xid, dataKey); // this returns a clone
            BeforeImage beforeImage = new BeforeImage(rm, rItem);
            if (beforeImagesForXid.indexOf(beforeImage) == -1) { // not already stored
                beforeImagesForXid.add(beforeImage);
            }
        }
        
    }

    // before any write, we should create a key that will represent the data we are writing
    public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException, DeadlockException {
        String dataKey = Flight.getKey(flightNum);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                setUpBeforeImage(xid, (AbstractRMHashMapManager) flightRM, dataKey);
                return flightRM.addFlight(xid, flightNum, flightSeats, flightPrice);
            }
        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::addFlight("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::addFlight("+xid+","+dataKey+") - DeadlockException!");
        }
        return false;
    }

    public boolean addCars(int xid, String location, int numCars, int price) throws RemoteException, DeadlockException {
        String dataKey = Car.getKey(location);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                setUpBeforeImage(xid, (AbstractRMHashMapManager) carRM, dataKey);
                return carRM.addCars(xid, location, numCars, price);
            }
        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::addCars("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::addCars("+xid+","+dataKey+") - DeadlockException!");
        }
        return false;
    }

    public boolean addRooms(int xid, String location, int numRooms, int price) throws RemoteException, DeadlockException {
        String dataKey = Room.getKey(location);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                setUpBeforeImage(xid, (AbstractRMHashMapManager) roomRM, dataKey);
                return roomRM.addRooms(xid, location, numRooms, price);
            }
        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::addRooms("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::addRooms("+xid+","+dataKey+") - DeadlockException!");
        }
        return false;
    }

    public int newCustomer(int xid) throws RemoteException, DeadlockException {
        int cid = customerRM.getNewCustomerId(xid);
        String dataKey = Customer.getKey(cid);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                setUpBeforeImage(xid, (AbstractRMHashMapManager) customerRM, dataKey);
                if (newCustomer(xid, cid)) {
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

    public boolean newCustomer(int xid, int cid) throws RemoteException, DeadlockException {
        String dataKey = Customer.getKey(cid);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                setUpBeforeImage(xid, (AbstractRMHashMapManager) customerRM, dataKey);
                return newCustomer(xid, cid);
            }
        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::newCustomer("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::newCustomer("+xid+","+dataKey+") - DeadlockException!");
        }
        return false;
    }

    public boolean deleteFlight(int xid, int flightNum) throws RemoteException, DeadlockException {
        String dataKey = Flight.getKey(flightNum);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                setUpBeforeImage(xid, (AbstractRMHashMapManager) flightRM, dataKey);
                return flightRM.deleteFlight(xid, flightNum);
            }
        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::addFlight("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::deleteFlight("+xid+","+dataKey+") - DeadlockException!");
        }
        return false;
    }

    public boolean deleteCars(int xid, String location) throws RemoteException, DeadlockException {
        String dataKey = Car.getKey(location);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                setUpBeforeImage(xid, (AbstractRMHashMapManager) carRM, dataKey);
                return carRM.deleteCars(xid, location);
            }
        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::deleteCars("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::deleteCars("+xid+","+dataKey+") - DeadlockException!");
        }
        return false;
    }

    public boolean deleteRooms(int xid, String location) throws RemoteException, DeadlockException {
        String dataKey = Room.getKey(location);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                setUpBeforeImage(xid, (AbstractRMHashMapManager) roomRM, dataKey);
                return roomRM.deleteRooms(xid, location);
            }
        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::deleteRooms("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::deleteRooms("+xid+","+dataKey+") - DeadlockException!");
        }
        return false;
    }

    public boolean deleteCustomer(int xid, int customerID) throws RemoteException, DeadlockException {
        String dataKey = Customer.getKey(customerID);
        try { 
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_WRITE);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                setUpBeforeImage(xid, (AbstractRMHashMapManager) customerRM, dataKey);
                return customerRM.deleteCustomer(xid, customerID);
            }
        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::deleteCustomer("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::deleteCustomer("+xid+","+dataKey+") - DeadlockException!");
        }
        return false;
    }

    public int queryFlight(int xid, int flightNumber) throws RemoteException, DeadlockException {
        String dataKey = Flight.getKey(flightNumber);
        try {
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_READ);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_READ+") - Bad input!");
            } else {
                return flightRM.queryFlight(xid, flightNumber);
            }

        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::queryFlight("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::queryFlight("+xid+","+dataKey+") - DeadlockException!");
        }
        return -1;
    }

    public int queryCars(int xid, String location) throws RemoteException, DeadlockException {
        String dataKey = Car.getKey(location);
        try {
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_READ);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_READ+") - Bad input!");
            } else {
                return carRM.queryCars(xid, location);
            }

        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::queryCars("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::queryCars("+xid+","+dataKey+") - DeadlockException!");
        }
        return -1;
    }

    public int queryRooms(int xid, String location) throws RemoteException, DeadlockException {
        String dataKey = Room.getKey(location);
        try {
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_READ);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_READ+") - Bad input!");
            } else {
                return roomRM.queryRooms(xid, location);
            }

        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::queryRooms("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::queryRooms("+xid+","+dataKey+") - DeadlockException!");
        }
        return -1;
    }

    public String queryCustomerInfo(int xid, int customerID) throws RemoteException, DeadlockException {
        String dataKey = Customer.getKey(customerID);
        try {
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_READ);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_READ+") - Bad input!");
            } else {
                return customerRM.queryCustomerInfo(xid, customerID);
            }

        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::queryCustomerInfo("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::queryCustomerInfo("+xid+","+dataKey+") - DeadlockException!");
        }
        return "";
    }

    public int queryFlightPrice(int xid, int flightNumber) throws RemoteException, DeadlockException {
        String dataKey = Flight.getKey(flightNumber);
        try {
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_READ);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_READ+") - Bad input!");
            } else {
                return flightRM.queryFlightPrice(xid, flightNumber);
            }
        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::queryFlightPrice("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::queryFlightPrice("+xid+","+dataKey+") - DeadlockException!");
        }
        return -1;
    }

    public int queryCarsPrice(int xid, String location) throws RemoteException, DeadlockException {
        String dataKey = Car.getKey(location);
        try {
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_READ);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_READ+") - Bad input!");
            } else {
                return carRM.queryCarsPrice(xid, location);
            }
        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::queryCarsPrice("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::queryCarsPrice("+xid+","+dataKey+") - DeadlockException!");
        }
        return -1;
    }

    public int queryRoomsPrice(int xid, String location) throws RemoteException, DeadlockException {
        String dataKey = Room.getKey(location);
        try {
            boolean gotLock = lockManager.Lock(xid, dataKey, LockType.LOCK_READ);
            if (!gotLock) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKey+","+LockType.LOCK_READ+") - Bad input!");
            } else {
                return roomRM.queryRoomsPrice(xid, location);
            }
        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::queryRoomsPrice("+xid+","+dataKey+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::queryRoomsPrice("+xid+","+dataKey+") - DeadlockException!");
        }
        return -1;
    }

    public boolean reserveFlight(int xid, int customerID, int flightNumber) throws RemoteException, DeadlockException {
        String dataKeyFlight = Flight.getKey(flightNumber);
        String dataKeyCustomer = Customer.getKey(customerID);
        try { 
            boolean gotLockFlight = lockManager.Lock(xid, dataKeyFlight, LockType.LOCK_WRITE);
            boolean gotLockCustomer = lockManager.Lock(xid, dataKeyCustomer, LockType.LOCK_WRITE);
            if (!(gotLockFlight && gotLockCustomer)) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKeyFlight+","+LockType.LOCK_WRITE+") - Bad input!\nOR\n");
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKeyCustomer+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                setUpBeforeImage(xid, (AbstractRMHashMapManager) flightRM, dataKeyFlight);
                setUpBeforeImage(xid, (AbstractRMHashMapManager) customerRM, dataKeyCustomer);
                return flightRM.reserveFlight(xid, customerID, flightNumber);
            }
        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::reserveFlight("+xid+","+dataKeyFlight+","+dataKeyCustomer+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::reserveFlight("+xid+","+dataKeyFlight+","+dataKeyCustomer+") - DeadlockException!");
        }
        return false;
    }

    public boolean reserveCar(int xid, int customerID, String location) throws RemoteException, DeadlockException {
        String dataKeyCar = Car.getKey(location);
        String dataKeyCustomer = Customer.getKey(customerID);
        try { 
            boolean gotLockFlight = lockManager.Lock(xid, dataKeyCar, LockType.LOCK_WRITE);
            boolean gotLockCustomer = lockManager.Lock(xid, dataKeyCustomer, LockType.LOCK_WRITE);
            if (!(gotLockFlight && gotLockCustomer)) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKeyCar+","+LockType.LOCK_WRITE+") - Bad input!\nOR\n");
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKeyCustomer+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                setUpBeforeImage(xid, (AbstractRMHashMapManager) carRM, dataKeyCar);
                setUpBeforeImage(xid, (AbstractRMHashMapManager) customerRM, dataKeyCustomer);
                return carRM.reserveCar(xid, customerID, location);
            }
        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::reserveCar("+xid+","+dataKeyCar+","+dataKeyCustomer+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::reserveCar("+xid+","+dataKeyCar+","+dataKeyCustomer+") - DeadlockException!");
        }
        return false;
    }

    public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException, DeadlockException {
        String dataKeyRoom = Room.getKey(location);
        String dataKeyCustomer = Customer.getKey(customerID);
        try { 
            boolean gotLockFlight = lockManager.Lock(xid, dataKeyRoom, LockType.LOCK_WRITE);
            boolean gotLockCustomer = lockManager.Lock(xid, dataKeyCustomer, LockType.LOCK_WRITE);
            if (!(gotLockFlight && gotLockCustomer)) {
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKeyRoom+","+LockType.LOCK_WRITE+") - Bad input!\nOR\n");
                Trace.info("TransactionRM::lockManager.Lock("+xid+","+dataKeyCustomer+","+LockType.LOCK_WRITE+") - Bad input!");
            } else {
                setUpBeforeImage(xid, (AbstractRMHashMapManager) flightRM, dataKeyRoom);
                setUpBeforeImage(xid, (AbstractRMHashMapManager) customerRM, dataKeyCustomer);
                return roomRM.reserveRoom(xid, customerID, location);
            }
        } catch (DeadlockException e) {
            abort(xid);
            Trace.info("TransactionRM::reserveRoom("+xid+","+dataKeyRoom+","+dataKeyCustomer+") - DeadlockException!");
            throw new DeadlockException(xid, "TransactionRM::reserveRoom("+xid+","+dataKeyRoom+","+dataKeyCustomer+") - DeadlockException!");
        }
        return false;
    }
}

class BeforeImage {
    AbstractRMHashMapManager rm;
    ReservableItem rItem;

    BeforeImage(AbstractRMHashMapManager rm, ReservableItem rItem) {
        this.rm = rm;
        this.rItem = rItem;
    }

    void restore(int xid) {
        if (rItem == null) {
            rm.removeData(xid, rItem.getKey());
        } else {
            rm.writeData(xid, rItem.getKey(), rItem);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BeforeImage)) {
            return false;
        }
        BeforeImage bOther = (BeforeImage) other;
        return bOther.rm == this.rm && bOther.rItem.getKey() == this.rItem.getKey();
    }
}