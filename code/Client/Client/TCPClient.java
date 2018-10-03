package Client;

import Server.Interface.*;
import Server.TCP.IProxyResourceManagerGetter;
import Server.TCP.Message;
import Server.TCP.AbstractProxyObject;
import Server.Common.Trace;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

import java.util.*;
import java.io.*;
import java.net.*;


public class TCPClient extends Client implements IProxyResourceManagerGetter
{
	private static String s_serverHost = "localhost";
	private static int s_serverPort = 2005;
	private static String s_serverName = "MiddlewareServer";

	//TODO: REPLACE 'ALEX' WITH YOUR GROUP NUMBER TO COMPILE
	private static String s_tcpPrefix = "group25_";

	public static void main(String args[])
	{
		if (args.length > 0)
		{
			s_serverHost = args[0];
		}
		if (args.length > 1)
		{
			s_serverName = args[1];
		}
		if (args.length > 2)
		{
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUsage: java client.TCPClient [server_hostname [server_rmiobject]]");
			System.exit(1);
		}

		// Set the security policy
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}

		// Get a reference to the RMIRegister
		try {
			TCPClient client = new TCPClient();
			client.m_resourceManager = (IResourceManager)client.getProxyResourceManager(s_serverHost, s_serverPort, s_serverName);
			client.start();
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public TCPClient()
	{
		super();
	}

	public AbstractProxyObject getProxyResourceManager(String hostname, int port, String boundName)
	{
    Message messageToSend = new Message();
    messageToSend.proxyObjectBoundName = s_tcpPrefix + boundName;
    while(true) {
      try {
        Socket socket = new Socket(hostname, port);

        ObjectOutputStream objectOutput =
          new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream objectInput =
          new ObjectInputStream(socket.getInputStream());

        objectOutput.writeObject(messageToSend);
        try {
          return (AbstractProxyObject)objectInput.readObject();
        } catch (Exception e) {
          Trace.info(s_serverName + ": expected customerRM to be AbstractProxyObject. Cast failed.");
          e.printStackTrace();
          System.exit(1);
        }
      } catch (Exception e) {
        Trace.info(s_serverName + " waiting for customer server");
        try {
          Thread.sleep(500);
        } catch(Exception err) {
          Trace.info("TCPMiddlewareResourceManager::getProxyResourceManager -> Thread sleep failed");
        }
      }
    }
	}
}
