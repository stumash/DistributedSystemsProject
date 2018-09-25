package Server.RMI;

import Server.Interface.*;
import Server.Common.*;

import java.rmi.NotBoundException;
import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
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

    // get car, flight, room resourceManagers
    // @TODO add parameters
    connectAllServers("localhost", 2000, "FlightServer", "localhost", 2001, "CarServer", "localhost", 2002, "RoomServer");

		// Create the RMI server entry
		try {
			// Create a new Server object
			RMIMiddlewareResourceManager server = new RMIMiddlewareResourceManager(s_serverName);

			// Dynamically generate the stub (client proxy)
			IResourceManager resourceManager = (IResourceManager)UnicastRemoteObject.exportObject(server, 0);

			// Bind the remote object's stub in the registry
			Registry l_registry;
			try {
				l_registry = LocateRegistry.createRegistry(1099);
			} catch (RemoteException e) {
				l_registry = LocateRegistry.getRegistry(1099);
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

	public RMIResourceManager(String name)
	{
		super(name);
	}

	public void connectAllServers(String serverFlight, int portFlight, String nameFlight,
                                String serverCar, int portCar, String nameCar,
                                String serverRoom, int portRoom, String nameRoom)
	{
    // connect to flights server
		try {
			boolean first = true;
			while (true) {
				try {
					Registry registry = LocateRegistry.getRegistry(serverFlight, portFlight);
					flightRM = (IFlightResourceManager)registry.lookup(s_rmiPrefix + nameFlight);
					System.out.println("Connected to '" + nameFlight + "' server [" + serverFlight + ":" + portFlight + "/" + s_rmiPrefix + nameFlight + "]");
					break;
				}
				catch (NotBoundException|RemoteException e) {
					if (first) {
						System.out.println("Waiting for '" + nameFlight + "' server [" + serverFlight + ":" + portFlight + "/" + s_rmiPrefix + nameFlight + "]");
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

    // connect to cars server
    try {
      boolean first = true;
      while (true) {
        try {
          Registry registry = LocateRegistry.getRegistry(serverCar, portCar);
          carRM = (ICarResourceManager)registry.lookup(s_rmiPrefix + nameCar);
          System.out.println("Connected to '" + nameCar + "' server [" + serverCar + ":" + portCar + "/" + s_rmiPrefix + nameCar + "]");
          break;
        }
        catch (NotBoundException|RemoteException e) {
          if (first) {
            System.out.println("Waiting for '" + nameCar + "' server [" + serverCar + ":" + portCar + "/" + s_rmiPrefix + nameCar + "]");
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

    // connect rooms server
    try {
      boolean first = true;
      while (true) {
        try {
          Registry registry = LocateRegistry.getRegistry(serverRoom, portRoom);
          roomRM = (IRoomResourceManager)registry.lookup(s_rmiPrefix + nameRoom);
          System.out.println("Connected to '" + nameRoom + "' server [" + serverRoom + ":" + portRoom + "/" + s_rmiPrefix + nameRoom + "]");
          break;
        }
        catch (NotBoundException|RemoteException e) {
          if (first) {
            System.out.println("Waiting for '" + nameRoom + "' server [" + serverRoom + ":" + portRoom + "/" + s_rmiPrefix + nameRoom + "]");
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
	}
}
