package Server.TCP;

import java.rmi.RemoteException;

import Server.Interface.*;
import Server.Common.Trace;

public class ProxyRoomResourceManager extends AbstractProxyObject implements IRoomResourceManager {
    public ProxyRoomResourceManager(String hostname, int port, String boundName) {
        super(hostname, port, boundName);
    }

    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "addRooms";
        message.methodArgs = new Object[]{new Integer(id), location, new Integer(numRooms), new Integer(price)};
        message.methodArgTypes = new Class[]{Integer.class, String.class, Integer.class, Integer.class};
        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyRoomResourceManager::sendAndReceiveMessage() in addRooms -> failed");
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
        message.methodArgs = new Object[]{new Integer(id), location};
        message.methodArgTypes = new Class[]{Integer.class, String.class};

        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyRoomResourceManager::sendAndReceiveMessage() in deleteRooms -> failed");
            throw new RemoteException("");
        }
        return recvMessage.requestSuccessful;
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
        message.methodArgs = new Object[]{new Integer(id), location};
        message.methodArgTypes = new Class[]{Integer.class, String.class};

        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyRoomResourceManager::sendAndReceiveMessage() in queryRooms -> failed");
            throw new RemoteException("");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyRoomResourceManager::queryRooms(" + id + "," + location + ") -> requestedValue is null");
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
        message.methodArgs = new Object[]{new Integer(id), location};
        message.methodArgTypes = new Class[]{Integer.class, String.class};

        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyRoomResourceManager::sendAndReceiveMessage() in queryRoomsPrice -> failed");
            throw new RemoteException("");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyRoomsResourceManager::queryRoomsPrice(" + id + "," + location + ") -> requestedValue is null");
            throw new RemoteException("Oh no!");
        } else {
            return (Integer) recvMessage.requestedValue;
        }
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
        message.methodArgs = new Object[]{new Integer(id), new Integer(customerID), location};
        message.methodArgTypes = new Class[]{Integer.class, Integer.class, String.class};

        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyRoomResourceManager::sendAndReceiveMessage() in reserveRoom -> failed");
            throw new RemoteException("");
        }
        return recvMessage.requestSuccessful;
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
        return new ProxyRoomResourceManager(p_hostname, p_port, p_boundName);
    }
}
