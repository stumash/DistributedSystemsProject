package Server.TCP;

import java.net.Socket;
import java.io.*;

import Server.Common.*;
import Server.Interface.*;

public class TCPMiddlewareResourceManager extends MiddlewareResourceManager implements IProxyResourceManagerGetter {
    private static String s_serverName = "MiddlewareServer";
    private static int s_serverPort = 2005;
    private static String s_serverHost = "localhost";
    private static String s_tcpPrefix = "group25_";
    private static String s_customerServerHostname = "localhost";
    private static int s_customerServerPort = 2003;
    private static String s_flightServerHostname = "localhost";
    private static int s_flightServerPort = 2000;
    private static String s_roomServerHostname = "localhost";
    private static int s_roomServerPort = 2002;
    private static String s_carServerHostname = "localhost";
    private static int s_carServerPort = 2001;

    public static void main(String[] args) {
        if (args.length > 0) {
          s_serverHost = args[0];
        }
        if (args.length > 1) {
          try {
              s_serverPort = Integer.parseInt(args[1]);
          } catch (Exception e) {
              System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m1st arg must be integer for middlewareserver port (default 2005)");
              e.printStackTrace();
              System.exit(1);
          }
        }
        if (args.length > 2) {
            s_customerServerHostname = args[2];
        }
        if (args.length > 3) {
            try {
                s_customerServerPort = Integer.parseInt(args[3]);
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m3rd arg must be integer for customerserver port (default 2003)");
                e.printStackTrace();
                System.exit(1);
            }
        }
        if (args.length > 4) {
            s_flightServerHostname = args[4];
        }
        if (args.length > 5) {
            try {
                s_flightServerPort = Integer.parseInt(args[5]);
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m5th arg must be integer for flightserver port (default 2000)");
                e.printStackTrace();
                System.exit(1);
            }
        }
        if (args.length > 6) {
            s_roomServerHostname = args[6];
        }
        if (args.length > 7) {
            try {
                s_roomServerPort = Integer.parseInt(args[7]);
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m7th arg must be integer for roomserver port (default 2002)");
                e.printStackTrace();
                System.exit(1);
            }
        }
        if (args.length > 8) {
            s_carServerHostname = args[8];
        }
        if (args.length > 9) {
            try {
                s_carServerPort = Integer.parseInt(args[9]);
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m9th arg must be integer for carserver port (default 2001)");
                e.printStackTrace();
                System.exit(1);
            }
        }

        TCPProxyObjectServer server = new TCPProxyObjectServer(s_serverHost, s_serverPort);
        TCPMiddlewareResourceManager middlewareRM = new TCPMiddlewareResourceManager(s_serverName);
        middlewareRM.flightRM = (IFlightResourceManager) middlewareRM.getProxyResourceManager(s_flightServerHostname,s_flightServerPort, "FlightServer");
        middlewareRM.carRM = (ICarResourceManager) middlewareRM.getProxyResourceManager(s_carServerHostname,s_carServerPort, "CarServer");
        middlewareRM.roomRM = (IRoomResourceManager) middlewareRM.getProxyResourceManager(s_roomServerHostname,s_roomServerPort, "RoomServer");
        middlewareRM.customerRM = (ICustomerResourceManager) middlewareRM.getProxyResourceManager(s_customerServerHostname,s_customerServerPort, "CustomerServer");

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
                Trace.info(s_serverName + " waiting for " + messageToSend.proxyObjectBoundName);
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
