import java.rmi.RemoteException;

public class ProxyCarResourceManager extends AbstractProxyObject implements ICarResourceManager
{
  public ProxyCarResourceManager(String hostname, int port, String boundName)
  {
    super(hostname, port, boundName);
  }

  /**
   * Reserve a car at this location.
   *
   * @return Success
   */
  public boolean reserveCar(int id, int customerID, String location) throws RemoteException
  {
    Message message = new ProxyMethodCallMessage();
    message.proxyObjectBoundName = this.boundName;
    message.methodName = "reserveCar";
    message.methodArgs = new String[] {Integer.toString(id), Integer.toString(customerID), location};
    message.methodArgTypes = new Class[] {Integer.class, Integer.class, String.class};

    Message recvMessage = sendAndReceiveMessage(message);
    return recvMessage.requestSuccessful;
  }

  /**
   * Add car at a location.
   *
   * This should look a lot like addFlight, only keyed on a string location
   * instead of a flight number.
   *
   * @return Success
   */
  public boolean addCars(int id, String location, int numCars, int price) throws RemoteException
  {
    Message message = new ProxyMethodCallMessage();
    message.proxyObjectBoundName = this.boundName;
    message.methodName = "addCars";
    message.methodArgs = new String[] {Integer.toString(id), location, Integer.toString(numCars), Integer.toString(price)};
    message.methodArgTypes = new Class[] {Integer.class, String.class, Integer.class, Integer.class};

    Message recvMessage = sendAndReceiveMessage(message);
    return recvMessage.requestSuccessful;
  }


  /**
   * Delete all cars at a location.
   *
   * It may not succeed if there are reservations for this location
   *
   * @return Success
   */
  public boolean deleteCars(int id, String location) throws RemoteException
  {
    Message message = new ProxyMethodCallMessage();
    message.proxyObjectBoundName = this.boundName;
    message.methodName = "deleteCars";
    message.methodArgs = new String[] {Integer.toString(id), location};
    message.methodArgTypes = new Class[] {Integer.class, String.class};

    Message recvMessage = sendAndReceiveMessage(message);
    return recvMessage.requestSuccessful;
  }


  /**
   * Query the status of a car location.
   *
   * @return Number of available cars at this location
   */
  public int queryCars(int id, String location) throws RemoteException
  {
    Message message = new ProxyMethodCallMessage();
    message.proxyObjectBoundName = this.boundName;
    message.methodName = "queryCars";
    message.methodArgs = new String[] {Integer.toString(id), location};
    message.methodArgTypes = new Class[] {Integer.class, String.class};

    Message recvMessage = sendAndReceiveMessage(message);
    if (recvMessage.requestedValue == null) {
      Trace.info("ProxyCarResourceManager::queryCars(" + id + "," + location ") -> requestedValue is null");
      throw RemoteException;
    } else {
      return (Integer)recvMessage.requestedValue;
    }
  }




  /**
   * Query the status of a car location.
   *
   * @return Price of car
   */
  public int queryCarsPrice(int id, String location) throws RemoteException
  {
    Message message = new ProxyMethodCallMessage();
    message.proxyObjectBoundName = this.boundName;
    message.methodName = "queryCarsPrice";
    message.methodArgs = new String[] {Integer.toString(id), location};
    message.methodArgTypes = new Class[] {Integer.class, String.class};

    Message recvMessage = sendAndReceiveMessage(message);
    if (recvMessage.requestedValue == null) {
      Trace.info("ProxyCarResourceManager::queryCarsPrice(" + id + "," + location ") -> requestedValue is null");
      throw RemoteException;
    } else {
      return (Integer)recvMessage.requestedValue;
    }
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
    return new ProxyCarResourceManager(p_hostname, p_port, p_boundName);
  }
}
