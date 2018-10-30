package group25.Server.TCP;

import group25.Server.Common.ReservedItem;
import group25.Server.Common.RMHashMap;
import group25.Server.Common.Trace;
import group25.Server.Interface.ICustomer;

public class ProxyCustomer extends AbstractProxyObject implements ICustomer {

    public ProxyCustomer(String hostname, int port, String boundName) {
        super(hostname, port, boundName);
    }

    public boolean setID(int id) {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "setID";
        message.methodArgs = new Object[]{id};
        message.methodArgTypes = new Class[]{int.class};
        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyCustomer::sendAndReceiveMessage() in setID -> failed");
        }
        return recvMessage.requestSuccessful;
    }

    public int getID() {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "getID";
        // message.methodArgs = new Object[] {id, location};
        // message.methodArgTypes = new Class[] {int.class, String.class};

        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyCustomer::sendAndReceiveMessage() in getID -> failed");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyCustomer::getID() -> requestedValue is null");
        } else {
            return (Integer) recvMessage.requestedValue;
        }
        return -1; // TODO: magic number removal
    }

    public boolean reserve(String key, String location, int price) {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "reserve";
        message.methodArgs = new Object[]{key, location, price};
        message.methodArgTypes = new Class[]{String.class, String.class, int.class};
        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyCustomer::sendAndReceiveMessage() in reserve -> failed");
        }
        return recvMessage.requestSuccessful;
    }

    public ReservedItem getReservedItem(String key) {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "getReservedItem";

        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyCustomer::sendAndReceiveMessage() in getReservedItem -> failed");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyCustomer::getReservedItem(" + key + ") -> requestedValue is null");
        } else {
            return (ReservedItem) recvMessage.requestedValue;
        }
        return null;
    }

    public String getBill() {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "getBill";
        // message.methodArgs = new Object[] {id, location};
        // message.methodArgTypes = new Class[] {int.class, String.class};

        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyCustomer::sendAndReceiveMessage() in getBill -> failed");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyCustomer::getBill() -> requestedValue is null");
        } else {
            return (String) recvMessage.requestedValue;
        }
        return null;
    }

    public String getKey() {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "getBill";
        // message.methodArgs = new Object[] {id, location};
        // message.methodArgTypes = new Class[] {int.class, String.class};

        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyCustomer::sendAndReceiveMessage() in getKey -> failed");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyCustomer::getKey() -> requestedValue is null");
        } else {
            return (String) recvMessage.requestedValue;
        }
        return null;
    }

    public RMHashMap getReservations() {
        ProxyMethodCallMessage message = new ProxyMethodCallMessage();
        message.proxyObjectBoundName = this.boundName;
        message.methodName = "getBill";
        // message.methodArgs = new Object[] {id, location};
        // message.methodArgTypes = new Class[] {int.class, String.class};

        Message recvMessage = null;
        try {
            recvMessage = sendAndReceiveMessage(message);
        } catch (Exception e) {
            Trace.info("ProxyCustomer::sendAndReceiveMessage() in getReservations -> failed");
        }
        if (recvMessage.requestedValue == null) {
            Trace.info("ProxyCustomer::getReservations() -> requestedValue is null");
        } else {
            return (RMHashMap) recvMessage.requestedValue;
        }
        return null;
    }

    public AbstractProxyObject makeProxyObject(String hostname, int port, String boundName) {
        return new ProxyCustomer(hostname, port, boundName);
    }

}
