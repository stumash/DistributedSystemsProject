public class ProxyCustomerResourceManager extends AbstractProxyObject implements ICustomerResourceManager
{
  public ProxyCustomerResourceManager(String hostname, int port, String boundName)
  {
    super(hostname, port, boundName);
  }

  public AbstractProxyObject makeProxyObject(String p_hostname, int p_port, String p_boundName)
  {
    return new ProxyCustomerResourceManager(p_hostname, p_port, p_boundName);
  }
}
