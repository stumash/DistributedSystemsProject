package group25.Server.Interface;

import group25.Server.Common.Customer;
import group25.Server.TCP.IProxiable;
import group25.Server.Interface.ICustomer;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;

public interface ICustomerResourceManager extends Remote, IProxiable, IAbstractRMHashMapManager {
    /**
     * Add customer.
     *
     * @return Unique customer identifier
     */
    public int newCustomer(int id)
            throws RemoteException;

    /**
     * Add customer with id.
     *
     * @return Success
     */
    public boolean newCustomer(int id, int cid)
            throws RemoteException;

    /**
     * Delete a customer and associated reservations.
     *
     * @return Success
     */
    public boolean deleteCustomer(int id, int customerID)
            throws RemoteException;

    /**
     * Query the customer reservations.
     *
     * @return A formatted bill for the customer
     */
    public String queryCustomerInfo(int id, int customerID)
            throws RemoteException;

    /**
     * Convenience for probing the resource manager.
     *
     * @return Name
     */
    public String getName()
            throws RemoteException;

    /**
     * Get a customer
     *
     * @return the customer
     */
    public ICustomer getCustomer(int xid, int customerID)
            throws RemoteException;


    public int getNewCustomerId(int xid)
        throws RemoteException;


    public void shutdown()
        throws RemoteException;
}
