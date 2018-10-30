package group25.Server.Common;

import group25.Server.Interface.*;
import group25.Server.TCP.*;

import java.util.*;
import java.rmi.RemoteException;
import java.io.*;

public abstract class FlightResourceManager extends AbstractRMHashMapManager implements IFlightResourceManager, ICustomerReservationManager {
    // Create a new flight, or add seats to existing flight
    // NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
    private String m_name = "";
    protected ICustomerResourceManager customerRM;

    public FlightResourceManager(String p_name) {
        m_name = p_name;
    }

    public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException {
        Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
        Flight curObj = (Flight) readData(xid, Flight.getKey(flightNum));
        if (curObj == null) {
            // Doesn't exist yet, add it
            Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
            writeData(xid, newObj.getKey(), newObj);
            Trace.info("RM::addFlight(" + xid + ") created new flight " + flightNum + ", seats=" + flightSeats + ", price=$" + flightPrice);
        } else {
            // Add seats to existing flight and update the price if greater than zero
            curObj.setCount(curObj.getCount() + flightSeats);
            if (flightPrice > 0) {
                curObj.setPrice(flightPrice);
            }
            writeData(xid, curObj.getKey(), curObj);
            Trace.info("RM::addFlight(" + xid + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount() + ", price=$" + flightPrice);
        }
        return true;
    }

    // Deletes flight
    public boolean deleteFlight(int xid, int flightNum) throws RemoteException {
        return deleteItem(xid, Flight.getKey(flightNum));
    }

    // Adds flight reservation to this customer
    public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException {
        return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
    }

    // Returns the number of empty seats in this flight
    public int queryFlight(int xid, int flightNum) throws RemoteException {
        return queryNum(xid, Flight.getKey(flightNum));
    }

    // Returns price of a seat in this flight
    public int queryFlightPrice(int xid, int flightNum) throws RemoteException {
        return queryPrice(xid, Flight.getKey(flightNum));
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
            //writeData(xid, customer.getKey(), customer); TODO: why do we need this line

            // Decrease the number of available items in the storage
            item.setCount(item.getCount() - 1);
            item.setReserved(item.getReserved() + 1);
            writeData(xid, item.getKey(), item);

            Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
            return true;
        }
    }

    public AbstractProxyObject makeProxyObject(String hostname, int port, String boundName) {
        return new ProxyFlightResourceManager(hostname, port, boundName);
    }
}
