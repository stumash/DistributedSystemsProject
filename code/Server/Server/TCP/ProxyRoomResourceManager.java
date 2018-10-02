public class ProxyRoomResourceManager extends AbstractProxyObject implements IRoomResourceManager
{
  public ProxyRoomResourceManager(String hostname, int port, String boundName)
  {
    super(hostname, port, boundName);
  }
  public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException
  {
    Message message = new ProxyMethodCallMessage();
    message.proxyObjectBoundName = this.boundName;
    message.methodName = "addRooms";
    message.methodArgs = new String[] {Integer.toString(id), location, Integer.toString(numRooms), Integer.toString(price)};
    message.methodArgTypes = new Class[] {Integer.class, String.class, Integer.class, Integer.class};
    Message recvMessage = sendAndReceiveMessage(message);
    return recvMessage.requestSuccessful;
  }
  /**
  * Delete all rooms at a location.
  *
  * It may not succeed if there are reservations for this location.
  *
  * @return Success
  */
  public boolean deleteRooms(int id, String location) throws RemoteException
  {
    Message message = new ProxyMethodCallMessage();
    message.proxyObjectBoundName = this.boundName;
    message.methodName = "deleteRooms";
    message.methodArgs = new String[] {Integer.toString(id), location};
    message.methodArgTypes = new Class[] {Integer.class, String.class};

    Message recvMessage = sendAndReceiveMessage(message);
    return recvMessage.requestSuccessful;
  }
  /**
  * Query the status of a room location.
  *
  * @return Number of available rooms at this location
  */
  public int queryRooms(int id, String location) throws RemoteException
  {
    Message message = new ProxyMethodCallMessage();
    message.proxyObjectBoundName = this.boundName;
    message.methodName = "queryRooms";
    message.methodArgs = new String[] {Integer.toString(id), location};
    message.methodArgTypes = new Class[] {Integer.class, String.class};

    Message recvMessage = sendAndReceiveMessage(message);
    if (recvMessage.requestedValue == null) {
      Trace.info("ProxyRoomResourceManager::queryRooms(" + id + "," + location ") -> requestedValue is null");
      throw RemoteException;
    } else {
      return (Integer)recvMessage.requestedValue;
    }
  }

  /**
  * Query the status of a room location.
  *
  * @return Price of a room
  */
  public int queryRoomsPrice(int id, String location) throws RemoteException
  {
    Message message = new ProxyMethodCallMessage();
    message.proxyObjectBoundName = this.boundName;
    message.methodName = "queryRoomsPrice";
    message.methodArgs = new String[] {Integer.toString(id), location};
    message.methodArgTypes = new Class[] {Integer.class, String.class};

    Message recvMessage = sendAndReceiveMessage(message);
    if (recvMessage.requestedValue == null) {
      Trace.info("ProxyRoomsResourceManager::queryRoomsPrice(" + id + "," + location ") -> requestedValue is null");
      throw RemoteException;
    } else {
      return (Integer)recvMessage.requestedValue;
    }
  }
  /**
  * Reserve a room at this location.
  *
  * @return Success
  */
  public boolean reserveRoom(int id, int customerID, String location) throws RemoteException
  {
    Message message = new ProxyMethodCallMessage();
    message.proxyObjectBoundName = this.boundName;
    message.methodName = "reserveRoom";
    message.methodArgs = new String[] {Integer.toString(id), Integer.toString(customerID), location};
    message.methodArgTypes = new Class[] {Integer.class, Integer.class, String.class};

    Message recvMessage = sendAndReceiveMessage(message);
    return recvMessage.requestSuccessful;
  }
  /**
  * Convenience for probing the resource manager.
  *
  * @return Name
  */
  public String getName() throws RemoteException
  {
    // throws RemoteException? TODO
  }

  public AbstractProxyObject makeProxyObject(String p_hostname, int p_port, String p_boundName)
  {
    return new ProxyRoomResourceManager(p_hostname, p_port, p_boundName);
  }
}
