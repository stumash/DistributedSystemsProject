// -------------------------------
// Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import java.util.*;

import Server.Interface.ICustomer;
import Server.TCP.AbstractProxyObject;
import Server.TCP.ProxyCustomer;

public class Customer extends RMItem implements ICustomer {
    private int m_ID;
    private RMHashMap m_reservations;

    public Customer(int id) {
        super();
        m_reservations = new RMHashMap();
        m_ID = id;
    }

    public boolean setID(int id) {
        m_ID = id;
        return true;
    }

    public int getID() {
        return m_ID;
    }

    public boolean reserve(String key, String location, int price) {
        synchronized (this) {
            ReservedItem reservedItem = getReservedItem(key);
            if (reservedItem == null) {
                // Customer doesn't already have a reservation for this resource, so create a new one now
                reservedItem = new ReservedItem(key, location, 1, price);
            } else {
                reservedItem.setCount(reservedItem.getCount() + 1);
                // NOTE: latest price overrides existing price
                reservedItem.setPrice(price);
            }
            System.out.println("I JUST PUT INTO HASHMAP");
            m_reservations.put(reservedItem.getKey(), reservedItem);
            System.out.println("BSLD" + m_reservations.keySet().size());
            return true;
        }
    }

    public ReservedItem getReservedItem(String key) {
        return (ReservedItem) m_reservations.get(key);
    }

    public String getBill() {
        // System.out.println("GEtting bill!!!!!");
        String s = "Bill for customer " + m_ID + "\n";
        // System.out.println("CUSTOMER KEY SET LENGTH" + m_reservations.keySet());
        for (String key : m_reservations.keySet()) {
            // System.out.println("INSIDE FOR LOOP");
            ReservedItem item = (ReservedItem) m_reservations.get(key);
            System.out.println("ITEM KEY: " + item.getReservableItemKey());
            s += +item.getCount() + " " + item.getReservableItemKey() + " $" + item.getPrice() + "\n";
        }
        return s;
    }

    public String toString() {
        String ret = "--- BEGIN CUSTOMER key='";
        ret += getKey() + "', id='" + getID() + "', reservations=>\n" + m_reservations.toString() + "\n";
        ret += "--- END CUSTOMER ---";
        return ret;
    }

    public static String getKey(int customerID) {
        String s = "customer-" + customerID;
        return s.toLowerCase();
    }

    public String getKey() {
        return Customer.getKey(getID());
    }

    public RMHashMap getReservations() {
        return m_reservations;
    }

    public Object clone() {
        Customer obj = (Customer) super.clone();
        obj.m_ID = m_ID;
        obj.m_reservations = (RMHashMap) m_reservations.clone();
        return obj;
    }

    public AbstractProxyObject makeProxyObject(String hostname, int port, String boundName) {
        return new ProxyCustomer(hostname, port, boundName);
    }

}
