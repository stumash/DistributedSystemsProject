package group25.Server.TCP;

import java.rmi.RemoteException;
import java.util.*;

import javax.transaction.InvalidTransactionException;

import group25.Server.Interface.*;
import group25.Server.Common.Trace;

public class ProxyMiddlewareResourceManager extends AbstractProxyObject implements IResourceManager {

    public ProxyMiddlewareResourceManager(String hostname, int port, String boundName) {
        super(hostname, port, boundName);
    }

    /**
     * Add seats to a flight.
     * <p>
     * In general this will be used to create a new
     * flight, but it should be possible to add seats to an existing flight.
     * Adding to an existing flight should overwrite the current price of the
     * available seats.
     *
     * @return Success
     */
    public boolean addFlight(int id, int flightNumber, int flightSeats, int flightPrice) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "addFlight";
        message.methodArgs = new Object[]{id, flightNumber, flightSeats, flightPrice};
        message.methodArgTypes = new Class[]{int.class, int.class, int.class, int.class};
        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() addFlight -> failed");
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
            System.out.println("about to send addCars message!!!!!!!");
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() addCars -> failed");
            throw new RemoteException("");
        }
        return recvMessage.requestSuccessful;
    }

    /**
     * Add room at a location.
     * <p>
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     *
     * @return Success
     */
    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "addRooms";
        message.methodArgs = new Object[]{id, location, numRooms, price};
        message.methodArgTypes = new Class[]{int.class, String.class, int.class, int.class};
        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() addRooms -> failed");
            throw new RemoteException("");
        }
        return recvMessage.requestSuccessful;
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
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() newCustomer -> failed");
            throw new RemoteException("");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyMiddlewareResourceManager::newCustomer(" + id + ") -> requestedValue is null");
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
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() newCustomer -> failed");
            throw new RemoteException("");
        }
        return recvMessage.requestSuccessful;
    }

    /**
     * Delete the flight.
     * <p>
     * deleteFlight implies whole deletion of the flight. If there is a
     * reservation on the flight, then the flight cannot be deleted
     *
     * @return Success
     */
    public boolean deleteFlight(int id, int flightNumber) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "deleteFlight";
        message.methodArgs = new Object[]{id, flightNumber};
        message.methodArgTypes = new Class[]{int.class, int.class};
        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() deleteFlight -> failed");
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
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() deleteCars -> failed");
            throw new RemoteException("");
        }
        return recvMessage.requestSuccessful;
    }

    /**
     * Delete all rooms at a location.
     * <p>
     * It may not succeed if there are reservations for this location.
     *
     * @return Success
     */
    public boolean deleteRooms(int id, String location) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "deleteRooms";
        message.methodArgs = new Object[]{id, location};
        message.methodArgTypes = new Class[]{int.class, String.class};
        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() deleteRooms -> failed");
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
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() deleteCustomer -> failed");
            throw new RemoteException("");
        }
        return recvMessage.requestSuccessful;
    }

    /**
     * Query the status of a flight.
     *
     * @return Number of empty seats
     */
    public int queryFlight(int id, int flightNumber) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "queryFlight";
        message.methodArgs = new Object[]{id, flightNumber};
        message.methodArgTypes = new Class[]{int.class, int.class};
        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() queryFlight -> failed");
            throw new RemoteException("");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyMiddlewareResourceManager::queryFlight(" + id + "," + flightNumber + ") -> requestedValue is null");
            throw new RemoteException("Oh no!");
        } else {
            return (Integer) recvMessage.requestedValue;
        }
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
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() queryCars -> failed");
            throw new RemoteException("");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyMiddlewareResourceManager::queryCars(" + id + "," + location + ") -> requestedValue is null");
            throw new RemoteException("Oh no!");
        } else {
            return (Integer) recvMessage.requestedValue;
        }
    }

    /**
     * Query the status of a room location.
     *
     * @return Number of available rooms at this location
     */
    public int queryRooms(int id, String location) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "queryRooms";
        message.methodArgs = new Object[]{id, location};
        message.methodArgTypes = new Class[]{int.class, String.class};
        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() queryRooms -> failed");
            throw new RemoteException("");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyMiddlewareResourceManager::queryRooms(" + id + "," + location + ") -> requestedValue is null");
            throw new RemoteException("Oh no!");
        } else {
            return (Integer) recvMessage.requestedValue;
        }
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
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() queryCustomerInfo -> failed");
            throw new RemoteException("");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyMiddlewareResourceManager::queryCustomerInfo(" + id + "," + customerID + ") -> requestedValue is null");
            throw new RemoteException("Oh no!");
        } else {
            return (String) recvMessage.requestedValue;
        }
    }

    /**
     * Query the status of a flight.
     *
     * @return Price of a seat in this flight
     */
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "queryFlightPrice";
        message.methodArgs = new Object[]{id, flightNumber};
        message.methodArgTypes = new Class[]{int.class, int.class};
        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() queryFlightPrice -> failed");
            throw new RemoteException("");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyMiddlewareResourceManager::queryFlightPrice(" + id + "," + flightNumber + ") -> requestedValue is null");
            throw new RemoteException("Oh no!");
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
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() queryCarsPrice -> failed");
            throw new RemoteException("");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyMiddlewareResourceManager::queryCarsPrice(" + id + "," + location + ") -> requestedValue is null");
            throw new RemoteException("Oh no!");
        } else {
            return (Integer) recvMessage.requestedValue;
        }
    }

    /**
     * Query the status of a room location.
     *
     * @return Price of a room
     */
    public int queryRoomsPrice(int id, String location) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "queryRoomsPrice";
        message.methodArgs = new Object[]{id, location};
        message.methodArgTypes = new Class[]{int.class, String.class};
        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() queryRoomsPrice -> failed");
            throw new RemoteException("");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyMiddlewareResourceManager::queryRoomsPrice(" + id + "," + location + ") -> requestedValue is null");
            throw new RemoteException("Oh no!");
        } else {
            return (Integer) recvMessage.requestedValue;
        }
    }

    /**
     * Reserve a seat on this flight.
     *
     * @return Success
     */
    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "reserveFlight";
        message.methodArgs = new Object[]{id, customerID, flightNumber};
        message.methodArgTypes = new Class[]{int.class, int.class, int.class};
        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() reserveFlight -> failed");
            throw new RemoteException("");
        }
        return recvMessage.requestSuccessful;
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
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() reserveCar -> failed");
            throw new RemoteException("");
        }
        return recvMessage.requestSuccessful;
    }

    /**
     * Reserve a room at this location.
     *
     * @return Success
     */
    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "reserveRoom";
        message.methodArgs = new Object[]{id, customerID, location};
        message.methodArgTypes = new Class[]{int.class, int.class, String.class};
        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() reserveRoom -> failed");
            throw new RemoteException("");
        }
        return recvMessage.requestSuccessful;
    }

    /**
     * Reserve a bundle for the trip.
     *
     * @return Success
     */
    public boolean bundle(int id, int customerID, Vector<Integer> flightNumbers, String location, boolean car, boolean room) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "bundle";
        message.methodArgs = new Object[]{id, customerID, flightNumbers, location, car, room};
        message.methodArgTypes = new Class[]{int.class, int.class, Vector.class, String.class, boolean.class, boolean.class};
        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyMiddlewareResourceManager::sendAndReceiveMessage() bundle -> failed");
            throw new RemoteException("");
        }
        return recvMessage.requestSuccessful;
    }

    /**
     * Convenience for probing the resource manager.
     *
     * @return Name
     */
    public AbstractProxyObject makeProxyObject(String p_hostname, int p_port, String p_boundName) {
        return new ProxyMiddlewareResourceManager(p_hostname, p_port, p_boundName);
    }

    public String getName() throws RemoteException {
        return "Y U DO THIS?";
    }

    @Override
    public int start() {
        return 0;
    }

    @Override
    public boolean commit(int xid) throws InvalidTransactionException {
        return false;
    }

    @Override
    public boolean abort(int xid) throws InvalidTransactionException {
		return false;
	}

    @Override
    public void shutdown() throws RemoteException {

    }
}
