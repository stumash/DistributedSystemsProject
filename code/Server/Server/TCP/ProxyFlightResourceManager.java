package Server.TCP;

import java.rmi.RemoteException;

import Server.Interface.*;
import Server.Common.Trace;

public class ProxyFlightResourceManager extends AbstractProxyObject implements IFlightResourceManager
{
  public ProxyFlightResourceManager(String hostname, int port, String boundName)
  {
    super(hostname, port, boundName);
  }

  public boolean addFlight(int id, int flightNumber, int flightSeats, int flightPrice) throws RemoteException {
    ProxyMethodCallMessage message = new ProxyMethodCallMessage();
    message.proxyObjectBoundName = this.boundName;
    message.methodName = "addFlight";
    message.methodArgs = new Object[] {new Integer(id), new Integer(flightNumber), new Integer(flightSeats), new Integer(flightPrice)};
    message.methodArgTypes = new Class[] {Integer.class, Integer.class, Integer.class, Integer.class};

    Message recvMessage = null;
    try {
      recvMessage = sendAndReceiveMessage(message);
    } catch (Exception e) {
      Trace.info("ProxyFlightResourceManager::sendAndReceiveMessage() in addFlight -> failed");
      throw new RemoteException("");
    }
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
  public boolean deleteFlight(int id, int flightNumber) throws RemoteException
  {
    ProxyMethodCallMessage message = new ProxyMethodCallMessage();
    message.proxyObjectBoundName = this.boundName;
    message.methodName = "deleteFlight";
    message.methodArgs = new Object[] {new Integer(id), new Integer(flightNumber)};
    message.methodArgTypes = new Class[] {Integer.class, Integer.class};

    Message recvMessage = null;
    try {
      recvMessage = sendAndReceiveMessage(message);
    } catch (Exception e) {
      Trace.info("ProxyFlightResourceManager::sendAndReceiveMessage() in deleteFlight -> failed");
      throw new RemoteException("");
    }
    return recvMessage.requestSuccessful;
  }
  /**
   * Query the status of a flight.
   *
   * @return Number of empty seats
   */
  public int queryFlight(int id, int flightNumber) throws RemoteException
  {
    ProxyMethodCallMessage message = new ProxyMethodCallMessage();
    message.proxyObjectBoundName = this.boundName;
    message.methodName = "queryFlight";
    message.methodArgs = new Object[] {new Integer(id), new Integer(flightNumber)};
    message.methodArgTypes = new Class[] {Integer.class, Integer.class};

    Message recvMessage = null;
    try {
      recvMessage = sendAndReceiveMessage(message);
    } catch (Exception e) {
      Trace.info("ProxyFlightResourceManager::sendAndReceiveMessage() in queryFlight -> failed");
      throw new RemoteException("");
    }
    if (recvMessage.requestedValue == null) {
      Trace.info("ProxyFlightResourceManager::queryFlight(" + id + "," + flightNumber +") -> requestedValue is null");
      throw new RemoteException("Oh no!");
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
    ProxyMethodCallMessage message = new ProxyMethodCallMessage();
    message.proxyObjectBoundName = this.boundName;
    message.methodName = "queryFlightPrice";
    message.methodArgs = new Object[] {new Integer(id), new Integer(flightNumber)};
    message.methodArgTypes = new Class[] {Integer.class, Integer.class};

    Message recvMessage = null;
    try {
      recvMessage = sendAndReceiveMessage(message);
    } catch (Exception e) {
      Trace.info("ProxyFlightResourceManager::sendAndReceiveMessage() in queryFlightPrice -> failed");
      throw new RemoteException("");
    }
    if (recvMessage.requestedValue == null) {
      Trace.info("ProxyFlightResourceManager::queryFlightPrice(" + id + "," + flightNumber +") -> requestedValue is null");
      throw new RemoteException("Oh no!");
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
    ProxyMethodCallMessage message = new ProxyMethodCallMessage();
    message.proxyObjectBoundName = this.boundName;
    message.methodName = "reserveFlight";
    message.methodArgs = new Object[] {new Integer(id), new Integer(customerID), new Integer(flightNumber)};
    message.methodArgTypes = new Class[] {Integer.class, Integer.class, String.class};

    Message recvMessage = null;
    try {
      recvMessage = sendAndReceiveMessage(message);
    } catch (Exception e) {
      Trace.info("ProxyFlightResourceManager::sendAndReceiveMessage() in reserveFlight -> failed");
      throw new RemoteException("");
    }
    return recvMessage.requestSuccessful;
  }

  /**
   * Convenience for probing the resource manager.
   *
   * @return Name
   */
  public String getName() throws RemoteException
  {
    return "Y U DO THIS?";
  }

  public AbstractProxyObject makeProxyObject(String p_hostname, int p_port, String p_boundName)
  {
    return new ProxyFlightResourceManager(p_hostname, p_port, p_boundName);
  }
}
