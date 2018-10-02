package Server.TCP;

import java.util.HashMap;
import java.net.*;

public class TCPProxyObjectServer
{
  private String hostname;
  private String port;

  // map names to the proxy and real object pairs for all object to serve
  private HashMap<String, PairProxyReal> nameToObjectPair;
  private ServerSocket listenerSocket;
  private long boundCustomerCount;

  public TCPProxyObjectServer(String hostname, int port)
  {
    this.hostname = hostname;
    this.port = port;

    nameToObjectPair = new HashMap<String, PairProxyReal>();
    listenerSocket = new ServerSocket(port);
    boundCustomerCount = 0;
  }

  public synchronized boolean bind(String objectName, Proxiable object)
  {
    AbstractProxyObject proxyObject = object.makeProxyObject(hostname, port, objectName);
    nameToObjectPair.put(objectName, new PairRealProxy(object, proxyObject));
  }

  public synchronized AbstractProxyObject getProxy(String objectName)
  {
    return nameToObjectPair.get(objectName).proxyObject;
  }

  public synchronized IProxiable getReal(String objectName)
  {
    return nameToObjectPair.get(objectName).realObject;
  }

  public void runServer()
  {
    new Thread(() -> {
      while(true) {
        Socket connectionSocket = listenerSocket.accept();

        new Thread(() -> {
          ObjectInputStream objectInputStream =
            new ObjectInputStream(connectionSocket.getInputStream());
          ObjectOutputStream objectOutputStream =
            new ObjectOutputStream(connectionSocket.getOutputStream());

          Message inputMessage = null;
          try {
            inputMessage = (Message)objectInputStream.readObject();
          } catch (Exception e) {
            Trace.info("ProxyServer::(Message)readObject() -> invalid Message, failed to read object");
            return;
          }

          Message outputMessage;
          if (!(inputMessage instanceof ProxyMethodCallMessage)) {
            // if it's not a ProxyMethodCallMessage, then it's just a method to request a proxy object
            outputMessage = new Message();
            try {
              outputMessage.requestedValue = getProxy(inputMessage.proxyObjectBoundName);
              outputMessage.requestSuccessful = true;
            } catch (Exception e) {
              Trace.info("ProxyServer::getProxy(" + inputMessage.proxyObjectBoundName + ") -> proxy object not found");
              outputMessage.requestSuccessful = false;
            }
          }
          else if (inputMessage instanceof ProxyMethodCallMessage) {
            outputMessage = new ProxyMethodCallMessage();
            IProxiable realObject = null;
            try {
              realObject = getReal(inputMessage.proxyObjectBoundName);
            } catch (Exception e) {
              Trace.info("ProxyServer::getReal(" + inputMessage.proxyObjectBoundName + ") -> real object not found");
              outputMessage.requestSuccessful = false;
              objectOutputStream.writeObject(outputMessage);
              return;
            }

            Class cls = realObject.getClass();
            Method m = cls.getMethod(inputMessage.methodName, inputMessage.methodArgTypes);
            Object[] castedArgs = new Object[inputMessage.methodArgs.length];
            for (int i = 0; i < castedArgs.length; i++) {
              castedArgs[i] = (inputMessage.methodArgTypes[i])inputMessage.methodArgs[i];
            }
            Object outputObject = m.invoke(realObject, castedArgs);

            outputMessage.requestedValue = outputObject;
            outputMessage.requestSuccessful = true;

            if (outputObject instanceof Customer) {
              Customer customer = (Customer)outputObject;
              AbsractProxyObject proxyCustomer;
              try {
                proxyCustomer = getProxy(customer.getKey());
              } catch (Exception e) {
                bind(customer.getKey(), customer);
                proxyCustomer = getProxy(customer.getKey());
              }

              outputObject = proxyObject;
              outputMessage.requestedValue = outputObject;
              outputMessage.requestedValueIsCustomer = true;
            }
          }

          objectOutputStream.writeObject(outputMessage);
        }).start();
      }
    }).start();
  }
}

class PairRealProxy
{
  AbstractProxyObject proxyObject;
  IProxiable realObject;

  PairProxyReal(IProxiable realObject, AbstractProxyObject proxyObject)
  {
    this.proxyObject = proxyObject;
    this.realObject = realObject;
  }
}
