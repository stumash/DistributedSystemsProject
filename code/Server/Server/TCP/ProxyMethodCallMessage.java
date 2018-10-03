package Server.TCP;

public class ProxyMethodCallMessage extends Message {
  String methodName;
  Object[] methodArgs;
  Class[] methodArgTypes;
  Boolean requestedValueIsCustomer;
}
