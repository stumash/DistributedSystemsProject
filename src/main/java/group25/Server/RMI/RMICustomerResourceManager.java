// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package group25.Server.RMI;

import group25.Server.Interface.*;
import group25.Server.Common.*;
import group25.Utils.CliParser;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

// RMIResourceManager is a whole class containing a registry and references to objects
// These objects can be created through "creators", i.e. addFlight, addCars, addRooms
// After creation or updates, through creators/setters, the object is written to RNHashMap
// RNHashMap takes a key and stores the object as the value
// Read data simply pulls object values from this RNHashMap

public class RMICustomerResourceManager extends CustomerResourceManager {

    private static final String s_serverName = "CustomerServer";
    private static int s_serverPort = 2003;
    private static final String s_rmiPrefix = "group25_";

    public RMICustomerResourceManager(String name) {
        super(name);
    }

    public static void main(String args[]) {
        CliParser cliParser = new CliParser("RMICustomerResourceManager",args, new String[] {
                CliParser.CUSTOMER_PORT
        });
        if (cliParser.parsedArg(CliParser.CUSTOMER_PORT))
            s_serverPort = cliParser.getParsedPort(CliParser.CUSTOMER_PORT);

        // Create the RMI server entry
        try {
            // Create a new Server object
            RMICustomerResourceManager server = new RMICustomerResourceManager(s_serverName);

            // Dynamically generate the stub (client proxy)
            ICustomerResourceManager resourceManager = (ICustomerResourceManager) UnicastRemoteObject.exportObject(server, 0);

            // Bind the remote object's stub in the registry
            Registry l_registry;
            try {
                l_registry = LocateRegistry.createRegistry(s_serverPort);
            } catch (RemoteException e) {
                l_registry = LocateRegistry.getRegistry(2003);
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
}
