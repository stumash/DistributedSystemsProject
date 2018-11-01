package group25.Server.Common;

import group25.Server.Interface.IResourceManager;
import group25.Server.Interface.ICarResourceManager;
import group25.Server.Interface.IFlightResourceManager;
import group25.Server.Interface.IRoomResourceManager;
import group25.Server.Interface.ICustomerResourceManager;
import group25.Server.LockManager.DeadlockException;
import group25.Server.LockManager.LockManager;
import group25.Server.LockManager.RedundantLockRequestException;
import group25.Server.TCP.AbstractProxyObject;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public class TransactionManager implements Remote, IResourceManager
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


    @Override // do we need this?
    public AbstractProxyObject makeProxyObject(String hostname, int port, String boundName) throws RemoteException {
        // ArrayList<Runnable> undoTasks = new ArrayList<>();
        // try {
        //     undoTasks.add(() -> {
        //         carRM.writeData(readData());
        //     });
        //     wrietDAta();
        // } catch (DeadlockException e) {
        //     abortUndo(undoTasks);
        // } catch (RedundantLockRequestException e) {

        // }
        return null;
    }

    /**
     * Abort transactoin associated with xid by
     * - unlocking all associated locks
     * - restoring all data to associated beforeImages(s)
     * @param xid
     */
    private void abort(int xid) {
        ArrayList<BeforeImage> beforeImages = writeRecorder.get(xid);
        for (BeforeImage beforeImage : beforeImages) {
            beforeImage.restore(xid);
        }
        // unlock AFTER rewrites
        // do unlockAll();
        lockManager.UnlockAll(xid);
    }

    @Override
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException {
        return false;
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
        // either make a car object, or implement a "make key" method that will return the same exact key for a real object
        // this will be m_data when we create lock objects through lockManager
        // String data = carRm.makeKey(location) -> "car-location"
        // try {
            // Boolean doLock = Lock(id, data, LockType.WriteLock);
            // // if (doLock) {
            //     carRM.addCars(id, location, numCars, price);
            // }
        // }
        // catch(DeadlockException) {
            // unlockAll(id)
            // for all (RM that XID uses) {
                // restoreBeforeImage();
            //}
        // } catch(RedundantLockRequestException) {
        //     // doLock still equals true
        // }

        return false;
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
        return false;
    }

    @Override
    public int newCustomer(int id) throws RemoteException {
        return 0;
    }

    @Override
    public boolean newCustomer(int id, int cid) throws RemoteException {
        return false;
    }

    @Override
    public boolean deleteFlight(int id, int flightNum) throws RemoteException {
        return false;
    }

    @Override
    public boolean deleteCars(int id, String location) throws RemoteException {
        return false;
    }

    @Override
    public boolean deleteRooms(int id, String location) throws RemoteException {
        return false;
    }

    @Override
    public boolean deleteCustomer(int id, int customerID) throws RemoteException {
        return false;
    }

    @Override
    public int queryFlight(int id, int flightNumber) throws RemoteException {
        return 0;
    }

    @Override
    public int queryCars(int id, String location) throws RemoteException {
        return 0;
    }

    @Override
    public int queryRooms(int id, String location) throws RemoteException {
        return 0;
    }

    @Override
    public String queryCustomerInfo(int id, int customerID) throws RemoteException {
        return null;
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
        return 0;
    }

    @Override
    public int queryCarsPrice(int id, String location) throws RemoteException {
        return 0;
    }

    @Override
    public int queryRoomsPrice(int id, String location) throws RemoteException {
        return 0;
    }

    @Override
    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException {
        return false;
    }

    @Override
    public boolean reserveCar(int id, int customerID, String location) throws RemoteException {
        return false;
    }

    @Override
    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {
        return false;
    }

    @Override
    public boolean bundle(int id, int customerID, Vector<Integer> flightNumbers, String location, boolean car,
            boolean room) throws RemoteException {
        return false;
    }

    @Override
	public String getName() throws RemoteException {
		return null;
	}
}

class BeforeImage {
    AbstractRMHashMapManager rm;
    ReservableItem beforeImage;
    BeforeImage(AbstractRMHashMapManager rm, ReservableItem beforeImage) {
        this.rm = rm;
        this.beforeImage = beforeImage;
    }
    void restore(int xid) {
        rm.writeData(xid, beforeImage.getKey(), beforeImage);
    }
}