package Server.TCP;

import java.io.Serializable;

public interface IProxiable
{
  public AbstractProxyObject makeProxyObject(String hostname, int port, String boundName);
}
