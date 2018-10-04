// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.RMI;

import Server.Interface.*;
import Server.Common.*;

import java.rmi.NotBoundException;
import java.util.*;

import java.rmi.Remote;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

// RMIResourceManager is a whole class containing a registry and references to objects
// These objects can be created through "creators", i.e. addFlight, addCars, addRooms
// After creation or updates, through creators/setters, the object is written to RNHashMap
// RNHashMap takes a key and stores the object as the value
// Read data simply pulls object values from this RNHashMap

public class RMIFlightResourceManager extends FlightResourceManager implements IRemoteResourceManagerGetter {
    private static String s_serverName = "FlightServer";
    private static int s_serverPort = 2000;
    private static String s_rmiPrefix = "group25_";
    private static String s_customerServerHostname = "localhost";
    private static int s_customerServerPort = 2003;

    public static void main(String args[]) {
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

        // Create the RMI server entry
        try {
            // Create a new Server object
            RMIFlightResourceManager server = new RMIFlightResourceManager(s_serverName);

            server.customerRM = (ICustomerResourceManager) server.getRemoteResourceManager(s_customerServerHostname,s_customerServerPort, "CustomerServer");

            // Dynamically generate the stub (client proxy)
            IFlightResourceManager resourceManager = (IFlightResourceManager) UnicastRemoteObject.exportObject(server, 0);

            // Bind the remote object's stub in the registry
            Registry l_registry;
            try {
                l_registry = LocateRegistry.createRegistry(s_serverPort);
            } catch (RemoteException e) {
                l_registry = LocateRegistry.getRegistry(s_serverPort);
            }
            final Registry registry = l_registry;
            registry.rebind(s_rmiPrefix + s_serverName, resourceManager);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        registry.unbind(s_rmiPrefix + s_serverName);
                        System.out.println("'" + s_serverName + "' resource manager unbound");
                    } catch (Exception e) {
                        System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
                        e.printStackTrace();
                    }
                }
            });
            System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
    }

    public Remote getRemoteResourceManager(String hostname, int port, String name) {
        Remote remoteResourceManager = null;
        try {
            boolean first = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(hostname, port);
                    remoteResourceManager = registry.lookup(s_rmiPrefix + name);
                    System.out.println("Connected to '" + name + "' server [" + hostname + ":" + port + "/" + s_rmiPrefix + name + "]");
                    break;
                } catch (NotBoundException | RemoteException e) {
                    if (first) {
                        System.out.println("Waiting for '" + name + "' server [" + hostname + ":" + port + "/" + s_rmiPrefix + name + "]");
                        first = false;
                    }
                }
                Thread.sleep(500);
            }
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
        return remoteResourceManager; // this line only reached if success
    }

    public RMIFlightResourceManager(String name) {
        super(name);
    }
}
