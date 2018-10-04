package Server.TCP;

public interface IProxiable {
    public AbstractProxyObject makeProxyObject(String hostname, int port, String boundName);
}
