package Client;

import Server.Interface.IResourceManager;
import Server.TCP.IProxyResourceManagerGetter;
import Server.TCP.Message;
import Server.TCP.AbstractProxyObject;
import Server.Common.Trace;

import java.io.*;
import java.net.*;

public class TCPClient extends Client implements IProxyResourceManagerGetter {
    private static String s_serverHost = "localhost";
    private static int s_serverPort = 2005;
    private static String s_serverName = "MiddlewareServer";

    private static String s_tcpPrefix = "group25_";

    public static void main(String args[]) {
        if (args.length > 0) {
            s_serverHost = args[0];
        }
        if (args.length > 1) {
            s_serverName = args[1];
        }
        if (args.length > 2) {
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUsage: java client.TCPClient [server_hostname [server_rmiobject]]");
            System.exit(1);
        }

        // Set the security policy
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        // Get a reference to the RMIRegister
        try {
            TCPClient client = new TCPClient();
            client.m_resourceManager = (IResourceManager) client.getProxyResourceManager(s_serverHost, s_serverPort, s_serverName);
            client.start();
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public TCPClient() {
        super();
    }

    @Override
    public void connectServer() {
        // do nothing, not used
    }

    public AbstractProxyObject getProxyResourceManager(String hostname, int port, String boundName) {
        Message messageToSend = new Message();
        messageToSend.proxyObjectBoundName = s_tcpPrefix + boundName;
        while (true) {
            try {
                Socket socket = new Socket(hostname, port);

                ObjectOutputStream osOut = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream osIn  =  new ObjectInputStream(socket.getInputStream());

                osOut.writeObject(messageToSend);

                try {
                    Message messageReceived = (Message) osIn.readObject();
                    return (AbstractProxyObject) messageReceived.requestedValue;
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
}
