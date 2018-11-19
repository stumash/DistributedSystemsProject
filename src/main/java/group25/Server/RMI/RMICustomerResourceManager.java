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

    public RMICustomerResourceManager(String name) {
        super(name);
    }

    public static void main(String args[]) {
        CliParser cliParser = new CliParser("RMICustomerResourceManager",args, new String[] {
                CliParser.CUSTOMER_PORT
        });
        if (cliParser.parsedArg(CliParser.CUSTOMER_PORT))
            s_serverPort = cliParser.getParsedPort(CliParser.CUSTOMER_PORT);

        // Create a new Server object
        RMICustomerResourceManager server = new RMICustomerResourceManager(s_serverName);

        // Dynamically generate the stub (client proxy)
        ICustomerResourceManager resourceManager = RMIUtils.createRMIproxyObject(server,0);

        // get local registry
        final Registry registry = RMIUtils.createLocalRMIregistry(s_serverPort);

        // Bind the remote object's stub in the registry
        RMIUtils.bindToRegistry(registry,s_serverName,resourceManager);

        // remove object from registry on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            RMIUtils.unbindFromRegistry(registry,s_serverName);
        }));

        System.out.println("'"+s_serverName+"' resource manager server ready and bound to '"+s_serverName+"'");

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
    }
}
