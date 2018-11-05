package group25.Server.Common;

import group25.Server.Interface.*;
import group25.Server.TCP.*;

import java.util.*;
import java.rmi.RemoteException;
import java.io.*;

public abstract class RoomResourceManager extends AbstractRMHashMapManager implements IRoomResourceManager, ICustomerReservationManager {
    // Create a new room location or add rooms to an existing location
    // NOTE: if price <= 0 and the room location already exists, it maintains its current price
    private String m_name = "";
    protected ICustomerResourceManager customerRM;

    public RoomResourceManager(String p_name) {
        m_name = p_name;
    }

    public boolean addRooms(int xid, String location, int count, int price) throws RemoteException {
        Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
        Room curObj = (Room) readData(xid, Room.getKey(location));
        if (curObj == null) {
            // Room location doesn't exist yet, add it
            Room newObj = new Room(location, count, price);
            writeData(xid, newObj.getKey(), newObj);
            Trace.info("RM::addRooms(" + xid + ") created new room location " + location + ", count=" + count + ", price=$" + price);
        } else {
            // Add count to existing object and update price if greater than zero
            curObj.setCount(curObj.getCount() + count);
            if (price > 0) {
                curObj.setPrice(price);
            }
            writeData(xid, curObj.getKey(), curObj);
            Trace.info("RM::addRooms(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
        }
        return true;
    }

    // Returns the amount of rooms available at a location
    public int queryRooms(int xid, String location) throws RemoteException {
        return queryNum(xid, Room.getKey(location));
    }

    // Returns room price at this location
    public int queryRoomsPrice(int xid, String location) throws RemoteException {
        return queryPrice(xid, Room.getKey(location));
    }

    // Delete rooms at a location
    public boolean deleteRooms(int xid, String location) throws RemoteException {
        return deleteItem(xid, Room.getKey(location));
    }

    // Adds room reservation to this customer
    public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException {
        return reserveItem(xid, customerID, Room.getKey(location), location);
    }

    public String getName() throws RemoteException {
        return m_name;
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

    public AbstractProxyObject makeProxyObject(String hostname, int port, String boundName) {
        return new ProxyRoomResourceManager(hostname, port, boundName);
    }
}
