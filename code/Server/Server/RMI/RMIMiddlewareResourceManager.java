package Server.RMI;

import Server.Interface.*;
import Server.Common.*;

import java.rmi.NotBoundException;
import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;

public class RMIMiddlewareResourceManager extends MiddlewareResourceManager
{
  private static String s_serverName = "MiddlewareServer";
	//TODO: REPLACE 'ALEX' WITH YOUR GROUP NUMBER TO COMPILE
	private static String s_rmiPrefix = "group25_";

  public static void main(String args[])
	{
		if (args.length > 0)
		{
			s_serverName = args[0];
		}

		// Create the RMI server entry
		try {
			// Create a new Server object
			RMIMiddlewareResourceManager server = new RMIMiddlewareResourceManager(s_serverName);

			// Dynamically generate the stub (client proxy)
			IResourceManager resourceManager = (IResourceManager)UnicastRemoteObject.exportObject(server, 0);

			// Bind the remote object's stub in the registry
			Registry l_registry;
			try {
				l_registry = LocateRegistry.createRegistry(2005);
			} catch (RemoteException e) {
				l_registry = LocateRegistry.getRegistry(2005);
			}
			final Registry registry = l_registry;
			registry.rebind(s_rmiPrefix + s_serverName, resourceManager);

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						registry.unbind(s_rmiPrefix + s_serverName);
						System.out.println("'" + s_serverName + "' resource manager unbound");
					}
					catch(Exception e) {
						System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
						e.printStackTrace();
					}
				}
			});
			System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");


      // get car, flight,  resourceManagers
      // @TODO add parameters
      // server.connectAllServers("localhost", 2000, "FlightServer", "localhost", 2001, "CarServer", "localhost", 2002, "Server");
      // server.connectCustomerServer("localhost", 2003, "CustomerServer");
      server.flightRM = (IFlightResourceManager)server.getRemoteResourceManager("localhost", 2000, "FlightServer");
      server.carRM = (ICarResourceManager)server.getRemoteResourceManager("localhost", 2001, "CarServer");
      server.roomRM = (IRoomResourceManager)server.getRemoteResourceManager("localhost", 2002, "RoomServer");
      server.customerRM = (ICustomerResourceManager)server.getRemoteResourceManager("localhost", 2003, "CustomerServer");
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

		// Create and install a security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}
	}

	public RMIMiddlewareResourceManager(String name)
	{
		super(name);
	}

  public Remote getRemoteResourceManager(String server, int port, String name) {
    Remote remoteResourceManager = null;
    try {
      boolean first = true;
      while (true) {
        try {
          Registry registry = LocateRegistry.getRegistry(server, port);
          remoteResourceManager = registry.lookup(s_rmiPrefix + name);
          System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
          break;
        }
        catch (NotBoundException|RemoteException e) {
          if (first) {
            System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
            first = false;
          }
        }
        Thread.sleep(500);
      }
    }
    catch (Exception e) {
      System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
      e.printStackTrace();
      System.exit(1);
    }
    return remoteResourceManager; // this line only reached if success
  }
}
