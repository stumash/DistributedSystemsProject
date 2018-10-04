package Server.TCP;

import java.rmi.RemoteException;

public interface IProxiable {
    public AbstractProxyObject makeProxyObject(String hostname, int port, String boundName)
            throws RemoteException;
}
