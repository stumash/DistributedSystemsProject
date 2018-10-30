package group25.Server.TCP;

import java.rmi.RemoteException;
import java.io.*;
import java.util.*;

import group25.Server.Interface.*;
import group25.Server.Common.Trace;

public class ProxyCarResourceManager extends AbstractProxyObject implements ICarResourceManager {
    public ProxyCarResourceManager(String hostname, int port, String boundName) {
        super(hostname, port, boundName);
    }

    /**
     * Reserve a car at this location.
     *
     * @return Success
     */
    public boolean reserveCar(int id, int customerID, String location) throws RemoteException {

        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "reserveCar";
        message.methodArgs = new Object[]{id, customerID, location};
        message.methodArgTypes = new Class[]{int.class, int.class, String.class};
        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyCarResourceManager::sendAndReceiveMessage() in reserveCar -> failed");
            throw new RemoteException("");
        }
        return recvMessage.requestSuccessful;
    }

    /**
     * Add car at a location.
     * <p>
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     *
     * @return Success
     */
    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "addCars";
        message.methodArgs = new Object[]{id, location, numCars, price};
        message.methodArgTypes = new Class[]{int.class, String.class, int.class, int.class};

        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyCarResourceManager::sendAndReceiveMessage() in addCars  -> failed");
            throw new RemoteException("");
        }
        return recvMessage.requestSuccessful;
    }


    /**
     * Delete all cars at a location.
     * <p>
     * It may not succeed if there are reservations for this location
     *
     * @return Success
     */
    public boolean deleteCars(int id, String location) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "deleteCars";
        message.methodArgs = new Object[]{id, location};
        message.methodArgTypes = new Class[]{int.class, String.class};

        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyCarResourceManager::sendAndReceiveMessage() in deleteCars -> failed");
            throw new RemoteException("");
        }
        return recvMessage.requestSuccessful;
    }


    /**
     * Query the status of a car location.
     *
     * @return Number of available cars at this location
     */
    public int queryCars(int id, String location) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "queryCars";
        message.methodArgs = new Object[]{id, location};
        message.methodArgTypes = new Class[]{int.class, String.class};

        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyCarResourceManager::sendAndReceiveMessage() in queryCars -> failed");
            throw new RemoteException("");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyCarResourceManager::queryCars(" + id + "," + location + ") -> requestedValue is null");
            throw new RemoteException("Could not query cars");
        } else {
            return (Integer) recvMessage.requestedValue;
        }
    }


    /**
     * Query the status of a car location.
     *
     * @return Price of car
     */
    public int queryCarsPrice(int id, String location) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "queryCarsPrice";
        message.methodArgs = new Object[]{id, location};
        message.methodArgTypes = new Class[]{int.class, String.class};

        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyCarResourceManager::sendAndReceiveMessage() in queryCarsPrice-> failed");
            throw new RemoteException("");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyCarResourceManager::queryCarsPrice(" + id + "," + location + ") -> requestedValue is null");
            throw new RemoteException("Oh no!");
        } else {
            return (Integer) recvMessage.requestedValue;
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


    public AbstractProxyObject makeProxyObject(String p_hostname, int p_port, String p_boundName) {
        return new ProxyCarResourceManager(p_hostname, p_port, p_boundName);
    }
}
