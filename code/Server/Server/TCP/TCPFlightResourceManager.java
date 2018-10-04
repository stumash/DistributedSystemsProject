package Server.TCP;

import java.net.Socket;
import java.io.*;

import Server.Common.*;
import Server.Interface.*;

public class TCPFlightResourceManager extends FlightResourceManager implements IProxyResourceManagerGetter {
    private static String s_serverName = "FlightServer";
    private static int s_serverPort = 2000;
    private static String s_tcpPrefix = "group25_";
    private static String s_customerServerHostname = "localhost";
    private static int s_customerServerPort = 2003;

    public static void main(String[] args) {
        if (args.length > 0) {
            try {
                s_serverPort = Integer.parseInt(args[0]);
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m1st arg must be integer for flightserver port (default 2000)");
                e.printStackTrace();
                System.exit(1);
            }
        }
        if (args.length > 1) {
            s_customerServerHostname = args[1];
        }
        if (args.length > 2) {
            try {
                s_customerServerPort = Integer.parseInt(args[2]);
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m3rd arg must be integer for customer server port (default 2003)");
                e.printStackTrace();
                System.exit(1);
            }
        }

        TCPProxyObjectServer server = new TCPProxyObjectServer("localhost", s_serverPort);
        TCPFlightResourceManager flightRM = new TCPFlightResourceManager(s_serverName);
        flightRM.customerRM = (ICustomerResourceManager) flightRM.getProxyResourceManager(s_customerServerHostname,s_customerServerPort, "CustomerServer");

        server.bind(s_tcpPrefix + s_serverName, flightRM);
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
                    Trace.info("TCPFlightResourceManager::getProxyResourceManager -> Thread sleep failed");
                }
            }
        }
    }

    public TCPFlightResourceManager(String name) {
        super(name);
    }
}
