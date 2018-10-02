public class ProxyMethodCallMessage extends Message {
  String methodName;
  String[] methodArgs;
  Class[] methodArgTypes;
  Boolean requestedValueIsCustomer;
}
