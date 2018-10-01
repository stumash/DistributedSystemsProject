import java.util.HashMap;
import java.net.*;

public class TCPProxyObjectServer
{
  // map names to the proxy and real object pairs for all object to serve
  private HashMap<String, PairProxyReal> nameToObjectPair;
  private ServerSocket listenerSocket;
  private long boundCustomerCount;

  public TCPProxyObjectServer(int port)
  {
    nameToObjectPair = new HashMap<String, PairProxyReal>();
    listenerSocket = new ServerSocket(port);
    boundCustomerCount = 0;
  }

  public synchronized boolean bind(String objectName, Proxiable object)
  {
    ProxyObject proxyObject = object.makeProxyObject();
    nameToObjectPair.put(objectName, new PairRealProxy(object, proxyObject));
  }

  public synchronized ProxyObject getProxy(String objectName)
  {
    return nameToObjectPair.get(objectName).proxyObject;
  }

  public void runServer()
  {
    Thread t = new Thread()
    {
      while(true) {
        Socket connectionSocket = listenerSocket.accept();

        (new Thread() {
          BufferedReader inFromClient =
            new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
          DataOutputStream outToClient =
            new DataOutputStream(connectionSocket.getOutputStream());

          // data = parseData(inFromClient);
          // if (data == 'getAlreadyBoundProxy') {
          //  ouputProxyOjbect(proxyObject, outToClient);
          // } else  if (data == 'callMethdod: objectName,methodName,args') {
          //  realObject = nameToObjectPair.get(objectName).realObject;
          //  realObject.methodName(args);
          // } else if (data == 'getNotYetBoundProxy,args') {
          //  Customer = findCustomerBy(args);
          //  name = makeCustomerName();
          //  bind(name, Customer);
          //  ouputProxyOjbect(nameToObjectPair.get(name).roxyObject); // proxyObject knows name that it's bound to
          // }
        }
      }
    }).start();
  }
}

class PairRealProxy {
  ProxyObject proxyObject;
  Proxiable realObject;

  PairProxyReal(Proxiable realObject, ProxyObject proxyObject)
  {
    this.proxyObject = proxyObject;
    this.realObject = realObject;
  }
}
