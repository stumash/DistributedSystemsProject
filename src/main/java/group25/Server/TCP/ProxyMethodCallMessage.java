package group25.Server.TCP;

public class ProxyMethodCallMessage extends Message {
    public String methodName;
    public Object[] methodArgs;
    public Class[] methodArgTypes;
    public Boolean requestedValueIsCustomer;
}
