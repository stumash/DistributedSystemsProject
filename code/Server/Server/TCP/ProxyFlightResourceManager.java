public class ProxyFlightResourceManager extends AbstractProxyObject implements IFlightResourceManager
{
  public ProxyFlightResourceManager(String hostname, int port, String boundName)
  {
    super(hostname, port, boundName);
  }

  public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException {
    Message message = new ProxyMethodCallMessage();
    message.proxyObjectBoundName = this.boundName;
    message.methodName = "addFlight";
    message.methodArgs = new String[] {Integer.toString(id), Integer.toString(flightNum), Integer.toString(flightSeats), Integer.toString(flightPrice)};
    message.methodArgTypes = new Class[] {Integer.class, Integer.class, Integer.class, Integer.class};

    Message recvMessage = sendAndReceiveMessage(message);
    return recvMessage.requestSuccessful;
  }
/**
 * Delete the flight.
 *
 * deleteFlight implies whole deletion of the flight. If there is a
 * reservation on the flight, then the flight cannot be deleted
 *
 * @return Success
 */
public boolean deleteFlight(int id, int flightNum) throws RemoteException
{
  Message message = new ProxyMethodCallMessage();
  message.proxyObjectBoundName = this.boundName;
  message.methodName = "deleteFlight";
  message.methodArgs = new String[] {Integer.toString(id), Integer.toString(flightNum)};
  message.methodArgTypes = new Class[] {Integer.class, Integer.class};

  Message recvMessage = sendAndReceiveMessage(message);
  return recvMessage.requestSuccessful;
}
/**
 * Query the status of a flight.
 *
 * @return Number of empty seats
 */
public int queryFlight(int id, int flightNumber) throws RemoteException
{
  Message message = new ProxyMethodCallMessage();
  message.proxyObjectBoundName = this.boundName;
  message.methodName = "queryFlight";
  message.methodArgs = new String[] {Integer.toString(id), Integer.toString(flightNum)};
  message.methodArgTypes = new Class[] {Integer.class, Integer.class};

  Message recvMessage = sendAndReceiveMessage(message);
  if (recvMessage.requestedValue == null) {
    Trace.info("ProxyFlightResourceManager::queryFlight(" + id + "," + flightNum ") -> requestedValue is null");
    throw RemoteException;
  } else {
    return (Integer)recvMessage.requestedValue;
  }
}

/**
 * Query the status of a flight.
 *
 * @return Price of a seat in this flight
 */
public int queryFlightPrice(int id, int flightNumber) throws RemoteException
{
  Message message = new ProxyMethodCallMessage();
  message.proxyObjectBoundName = this.boundName;
  message.methodName = "queryFlightPrice";
  message.methodArgs = new String[] {Integer.toString(id), Integer.toString(flightNum)};
  message.methodArgTypes = new Class[] {Integer.class, Integer.class};

  Message recvMessage = sendAndReceiveMessage(message);
  if (recvMessage.requestedValue == null) {
    Trace.info("ProxyFlightResourceManager::queryFlightPrice(" + id + "," + location ") -> requestedValue is null");
    throw RemoteException;
  } else {
    return (Integer)recvMessage.requestedValue;
  }
}
/**
 * Reserve a seat on this flight.
 *
 * @return Success
 */
public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException
{
  Message message = new ProxyMethodCallMessage();
  message.proxyObjectBoundName = this.boundName;
  message.methodName = "reserveFlight";
  message.methodArgs = new String[] {Integer.toString(id), Integer.toString(customerID), Integer.toString(flightNum)};
  message.methodArgTypes = new Class[] {Integer.class, Integer.class, String.class};

  Message recvMessage = sendAndReceiveMessage(message);
  return recvMessage.requestSuccessful;
}

/**
 * Convenience for probing the resource manager.
 *
 * @return Name
 */
public String getName() throws RemoteException {
  // throws RemoteException? TODO
}

  public AbstractProxyObject makeProxyObject(String p_hostname, int p_port, String p_boundName)
  {
    return new ProxyFlightResourceManager(p_hostname, p_port, p_boundName);
  }
}
