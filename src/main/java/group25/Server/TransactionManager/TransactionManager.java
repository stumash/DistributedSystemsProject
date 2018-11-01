package group25.Server.TransactionManager;

import group25.Server.Interface.IResourceManager;
import group25.Server.Interface.ICarResourceManager;
import group25.Server.Interface.IFlightResourceManager;
import group25.Server.Interface.IRoomResourceManager;
import group25.Server.Interface.ICustomerResourceManager;
import group25.Server.LockManager.LockManager;
import group25.Server.TCP.AbstractProxyObject;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

public class TransactionManager  implements Remote, IResourceManager
{
    private LockManager lockManager;
    private ICarResourceManager carRM;
    private IFlightResourceManager flightRM;
    private IRoomResourceManager roomRM;
    private ICustomerResourceManager customerRM;

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
    }

    @Override
    public AbstractProxyObject makeProxyObject(String hostname, int port, String boundName) throws RemoteException {
        return null;
    }

    @Override
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException {
        return false;
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
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