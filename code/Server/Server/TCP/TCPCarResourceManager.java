package Server.TCP;

import java.net.Socket;
import java.io.*;

import Server.Common.*;
import Server.Interface.*;

public class TCPCarResourceManager extends CarResourceManager implements IProxyResourceManagerGetter {
    private static String s_serverName = "CarServer";
    private static String s_tcpPrefix = "group25_";

    public static void main(String[] args) {
        TCPProxyObjectServer server = new TCPProxyObjectServer("localhost", 2001);
        TCPCarResourceManager carRM = new TCPCarResourceManager(s_serverName);
        carRM.customerRM = (ICustomerResourceManager) carRM.getProxyResourceManager("localhost", 2003, "CustomerServer");

        server.bind(s_serverName + s_tcpPrefix, carRM);
        server.runServer();
        System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_tcpPrefix + s_serverName + "'");
    }

    public TCPCarResourceManager(String name) {
        super(name);
    }

    public AbstractProxyObject getProxyResourceManager(String hostname, int port, String boundName) {
        Message messageToSend = new Message();
        messageToSend.proxyObjectBoundName = s_tcpPrefix + boundName;
        while (true) {
            try {
                Socket socket = new Socket(hostname, port);

                ObjectOutputStream objectOutputStream =
                        new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream objectInputStream =
                        new ObjectInputStream(socket.getInputStream());

                objectOutputStream.writeObject(messageToSend);
                try {
                    Message messageReceived = (Message) objectInputStream.readObject();
                    return (AbstractProxyObject) messageReceived.requestedValue;
                } catch (Exception e) {
                    Trace.info(s_serverName + ": expected customerRM to be AbstractProxyObject. Cast failed.");
                }
            } catch (Exception e) {
                Trace.info(s_serverName + " waiting for customer server");
                try {
                    Thread.sleep(500);
                } catch (Exception err) {
                    Trace.info("TCPCarResourceManager::getProxyResourceManager -> Thread sleep failed");
                }
            }
        }
    }

}
