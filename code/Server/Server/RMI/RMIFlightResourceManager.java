// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.RMI;

import Server.Interface.*;
import Server.Common.*;

import java.rmi.NotBoundException;
import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

// RMIResourceManager is a whole class containing a registry and references to objects
// These objects can be created through "creators", i.e. addFlight, addCars, addRooms
// After creation or updates, through creators/setters, the object is written to RNHashMap
// RNHashMap takes a key and stores the object as the value
// Read data simply pulls object values from this RNHashMap

public class RMIFlightResourceManager extends FlightResourceManager
{
	private static String s_serverName = "FlightServer";
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
			RMIFlightResourceManager server = new RMIFliResourceManager(s_serverName);

			// Dynamically generate the stub (client proxy)
			IFlightResourceManager resourceManager = (IFlightResourceManager)UnicastRemoteObject.exportObject(server, 0);

			// Bind the remote object's stub in the registry
			Registry l_registry;
			try {
				l_registry = LocateRegistry.createRegistry(2000);
			} catch (RemoteException e) {
				l_registry = LocateRegistry.getRegistry(2000);
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

	public RMIFlightResourceManager(String name)
	{
		super(name);
	}
}