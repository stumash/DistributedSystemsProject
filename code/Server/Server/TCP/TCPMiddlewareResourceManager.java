package Server.TCP;

import java.net.Socket;
import java.io.*;

import Server.Common.*;
import Server.Interface.*;

public class TCPMiddlewareResourceManager extends MiddlewareResourceManager implements IProxyResourceManagerGetter {
    private static String s_serverName = "MiddlewareServer";
    private static String s_tcpPrefix = "group25_";

    public static void main(String[] args) {
        TCPProxyObjectServer server = new TCPProxyObjectServer("localhost", 2005);
        TCPMiddlewareResourceManager middlewareRM = new TCPMiddlewareResourceManager(s_serverName);
        middlewareRM.flightRM = (IFlightResourceManager) middlewareRM.getProxyResourceManager("localhost", 2000, "FlightServer");
        middlewareRM.carRM = (ICarResourceManager) middlewareRM.getProxyResourceManager("localhost", 2001, "CarServer");
        middlewareRM.roomRM = (IRoomResourceManager) middlewareRM.getProxyResourceManager("localhost", 2002, "RoomServer");
        middlewareRM.customerRM = (ICustomerResourceManager) middlewareRM.getProxyResourceManager("localhost", 2003, "CustomerServer");

        server.bind(s_tcpPrefix + s_serverName, middlewareRM);
        server.runServer();
        System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_tcpPrefix + s_serverName + "'");
    }

    public AbstractProxyObject getProxyResourceManager(String hostname, int port, String boundName) {
        Message messageToSend = new Message();
        messageToSend.proxyObjectBoundName = s_tcpPrefix + boundName;
        System.out.println("requesting proxy " + messageToSend.proxyObjectBoundName);
        while (true) {
            try {
                Socket socket = new Socket(hostname, port);

                ObjectOutputStream osOut = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream  osIn  = new ObjectInputStream(socket.getInputStream());

                osOut.writeObject(messageToSend);
                try {
                    Message messageReceived = (Message) osIn.readObject();
                    AbstractProxyObject receivedObject = (AbstractProxyObject) messageReceived.requestedValue;
                    if (receivedObject == null) throw new Exception("received proxy object was null");
                    System.out.println("got requested " + messageToSend.proxyObjectBoundName);
                    return receivedObject;
                } catch (Exception e) {
                    Trace.info(s_serverName + ": expected customerRM to be AbstractProxyObject. Cast failed.");
                    e.printStackTrace();
                    System.exit(1);
                }
            } catch (Exception e) {
                Trace.info(s_serverName + " waiting for customer server");
                try {
                    Thread.sleep(500);
                } catch (Exception err) {
                    Trace.info("TCPMiddlewareResourceManager::getProxyResourceManager -> Thread sleep failed");
                }
            }
        }
    }

    public TCPMiddlewareResourceManager(String name) {
        super(name);
    }
}
