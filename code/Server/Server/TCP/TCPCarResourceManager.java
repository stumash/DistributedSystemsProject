package Server.TCP;

import java.net.Socket;
import java.io.*;

import Server.Common.*;
import Server.Interface.*;

public class TCPCarResourceManager extends CarResourceManager implements IProxyResourceManagerGetter
{
    private static String s_serverName = "CarServer";
    private static int s_serverPort = 2001;
    private static String s_serverHost = "localhost";
    private static String s_tcpPrefix = "group25_";
    private static String s_customerServerHostname = "localhost";
    private static int s_customerServerPort = 2003;

    public static void main(String[] args)
    {
        if (args.length > 0) {
          s_serverHost = args[0];

        } if (args.length > 1) {
          try {
              s_serverPort = Integer.parseInt(args[1]);
          } catch (Exception e) {
              System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m1st arg must be integer for carserver port (default 2001)");
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
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m3rd arg must be integer for customer server port (default 2003)");
                e.printStackTrace();
                System.exit(1);
            }
        }

        TCPProxyObjectServer server = new TCPProxyObjectServer(s_serverHost, s_serverPort);
        TCPCarResourceManager carRM = new TCPCarResourceManager(s_serverName);
        carRM.customerRM = (ICustomerResourceManager) carRM.getProxyResourceManager(s_customerServerHostname,s_customerServerPort, "CustomerServer");

        server.bind(s_tcpPrefix + s_serverName, carRM);
        server.runServer();
        System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_tcpPrefix + s_serverName + "'");
    }

    public AbstractProxyObject getProxyResourceManager(String hostname, int port, String boundName)
    {
        Message messageToSend = new Message();
        messageToSend.proxyObjectBoundName = s_tcpPrefix + boundName;
        System.out.println("requesting proxy " + messageToSend.proxyObjectBoundName);
        while (true) {
            try {
                Socket socket = new Socket(hostname, port);

                ObjectOutputStream osOut = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream osIn  =  new ObjectInputStream(socket.getInputStream());

                osOut.writeObject(messageToSend);
                try {
                    Message messageReceived = (Message) osIn.readObject();
                    AbstractProxyObject receivedObject = (AbstractProxyObject) messageReceived.requestedValue;
                    if (receivedObject == null) throw new Exception("received proxy object was null");
                    System.out.println("got requested " + messageToSend.proxyObjectBoundName);
                    return receivedObject;
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

    public TCPCarResourceManager(String name)
    {
        super(name);
    }
}
