package Server.TCP;

import Server.Common.*;
import Server.Interface.*;

import java.rmi.RemoteException;


public class ProxyCustomerResourceManager extends AbstractProxyObject implements ICustomerResourceManager {

    public ProxyCustomerResourceManager(String hostname, int port, String boundName) {
        super(hostname, port, boundName);
    }

    /**
     * Add customer.
     *
     * @return Unique customer identifier
     */
    public int newCustomer(int id) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "newCustomer";
        message.methodArgs = new Object[]{id};
        message.methodArgTypes = new Class[]{int.class};
        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyCustomerResourceManager::sendAndReceiveMessage() newCustomer -> failed");
            throw new RemoteException("");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyCustomerResourceManager::newCustomer(" + id + ") -> requestedValue is null");
            throw new RemoteException("Oh no!");
        } else {
            return (Integer) recvMessage.requestedValue;
        }
    }

    /**
     * Add customer with id.
     *
     * @return Success
     */
    public boolean newCustomer(int id, int customerID) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "newCustomer";
        message.methodArgs = new Object[]{id, customerID};
        message.methodArgTypes = new Class[]{int.class, int.class};
        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyCustomerResourceManager::sendAndReceiveMessage() newCustomer(id, customerID)-> failed");
            throw new RemoteException("");
        }
        return recvMessage.requestSuccessful;
    }

    /**
     * Delete a customer and associated reservations.
     *
     * @return Success
     */
    public boolean deleteCustomer(int id, int customerID) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "deleteCustomer";
        message.methodArgs = new Object[]{id, customerID};
        message.methodArgTypes = new Class[]{int.class, int.class};

        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyCustomerResourceManager::sendAndReceiveMessage() deleteCustomer-> failed");
            throw new RemoteException("");
        }
        return recvMessage.requestSuccessful;
    }

    /**
     * Query the customer reservations.
     *
     * @return A formatted bill for the customer
     */
    public String queryCustomerInfo(int id, int customerID) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "queryCustomerInfo";
        message.methodArgs = new Object[]{id, customerID};
        message.methodArgTypes = new Class[]{int.class, int.class};

        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyCustomerResourceManager::sendAndReceiveMessage() queryCustomerInfo -> failed");
            throw new RemoteException("");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyCustomerResourceManager::queryCustomer(" + id + ") -> requestedValue is null");
            throw new RemoteException("Oh no!");
        } else {
            return (String) recvMessage.requestedValue;
        }
    }

    /**
     * Convenience for probing the resource manager.
     *
     * @return Name
     */
    public String getName() throws RemoteException {
        return "Y U DO THIS?";
    }

    /**
     * Get a customer
     *
     * @return the customer
     */
    public ICustomer getCustomer(int xid, int customerID) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "getCustomer";
        message.methodArgs = new Object[]{xid, customerID};
        message.methodArgTypes = new Class[]{int.class, int.class};

        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyCustomerResourceManager::sendAndReceiveMessage() getCustomer-> failed");
            throw new RemoteException("");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyCustomerResourceManager::getCustomer(" + xid + "," + customerID + ") -> requestedValue is null");
            throw new RemoteException("Oh no!");
        } else {
            return (ICustomer) recvMessage.requestedValue;
        }
    }

    public AbstractProxyObject makeProxyObject(String p_hostname, int p_port, String p_boundName) {
        return new ProxyCustomerResourceManager(p_hostname, p_port, p_boundName);
    }
}
