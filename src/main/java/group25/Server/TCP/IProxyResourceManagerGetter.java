package group25.Server.TCP;

public interface IProxyResourceManagerGetter {
    public AbstractProxyObject getProxyResourceManager(String hostname, int port, String boundName);
}
