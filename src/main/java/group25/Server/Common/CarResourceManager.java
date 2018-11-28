package group25.Server.Common;

import group25.Server.Interface.*;
import group25.Server.TCP.*;

import java.rmi.RemoteException;
import java.util.concurrent.locks.ReentrantLock;

public abstract class CarResourceManager extends AbstractRMHashMapManager implements ICarResourceManager, ICustomerReservationManager {
    // Create a new car location or add cars to an existing location
    // NOTE: if price <= 0 and the location already exists, it maintains its current price
    private String m_name = "";
    protected ICustomerResourceManager customerRM;
    ReentrantLock commitLock = new ReentrantLock(true);

    public CarResourceManager(String p_name) {
        m_name = p_name;
    }

    public boolean vote(int xid) { // TODO TODO TODO TODO
        try {
            commitLock.lock();

        } catch (Exception e) {
            commitLock.unlock();
        }

        return true;
    }

    public boolean doCommit(int xid) { // TODO TODO TODO TODO
        try {

        } catch (Exception e) {
            return false;
        } finally {
            if (commitLock.isHeldByCurrentThread()) {
                commitLock.unlock();
            }
        }
        return true;
    }

    public boolean addCars(int xid, String location, int count, int price) throws RemoteException {
        Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
        Car curObj = (Car) readData(xid, Car.getKey(location));
        if (curObj == null) {
            // Car location doesn't exist yet, add it
            Car newObj = new Car(location, count, price);
            writeData(xid, newObj.getKey(), newObj);
            Trace.info("RM::addCars(" + xid + ") created new location " + location + ", count=" + count + ", price=$" + price);
        } else {
            // Add count to existing car location and update price if greater than zero
            curObj.setCount(curObj.getCount() + count);
            if (price > 0) {
                curObj.setPrice(price);
            }
            writeData(xid, curObj.getKey(), curObj);
            Trace.info("RM::addCars(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
        }
        return true;
    }

    // Adds car reservation to this customer
    public boolean reserveCar(int xid, int customerID, String location) throws RemoteException {
        return reserveItem(xid, customerID, Car.getKey(location), location);
    }

    // Delete cars at a location
    public boolean deleteCars(int xid, String location) throws RemoteException {
        return deleteItem(xid, Car.getKey(location));
    }

    // Returns the number of cars available at a location
    public int queryCars(int xid, String location) throws RemoteException {
        return queryNum(xid, Car.getKey(location));
    }

    // Returns price of cars at this location
    public int queryCarsPrice(int xid, String location) throws RemoteException {
        return queryPrice(xid, Car.getKey(location));
    }

    public boolean reserveItem(int xid, int customerID, String key, String location) throws RemoteException {
        Trace.info("RM::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called");
        // Read customer object if it exists (and read lock it)
        ICustomer customer = null;
        try {
            customer = customerRM.getCustomer(xid, customerID);
            if (customer == null) {
                Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
                return false;
            }
        } catch (RemoteException e) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist--remote exception");
            return false;
        }

        // Check if the item is available
        ReservableItem item = (ReservableItem) readData(xid, key);
        if (item == null) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
            return false;
        } else if (item.getCount() == 0) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
            return false;
        } else {
            customer.reserve(key, location, item.getPrice());
            customerRM.writeData(xid, customer.getKey(), (RMItem) customer);
            // Decrease the number of available items in the storage
            item.setCount(item.getCount() - 1);
            item.setReserved(item.getReserved() + 1);
            writeData(xid, item.getKey(), item);

            Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
            return true;
        }
    }

    public String getName() throws RemoteException {
        return m_name;
    }

    public AbstractProxyObject makeProxyObject(String hostname, int port, String boundName) {
        return new ProxyCarResourceManager(hostname, port, boundName);
    }
}
