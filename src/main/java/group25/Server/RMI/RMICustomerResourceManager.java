// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package group25.Server.RMI;

import group25.Server.Interface.*;
import group25.Server.Common.*;
import group25.Utils.CliParser;

import java.rmi.registry.Registry;
import java.rmi.RemoteException;

import static group25.Utils.AnsiColors.BLUE;
import static group25.Utils.AnsiColors.RED;

// RMIResourceManager is a whole class containing a registry and references to objects
// These objects can be created through "creators", i.e. addFlight, addCars, addRooms
// After creation or updates, through creators/setters, the object is written to RNHashMap
// RNHashMap takes a key and stores the object as the value
// Read data simply pulls object values from this RNHashMap

public class RMICustomerResourceManager extends CustomerResourceManager {

    private static final String s_serverName = "CustomerServer";

    private static String s_serverHostname = "localhost";
    private static int s_serverPort = 2003;
    private static String s_middlewareHostname = "localhost";
    private static int s_middlewarePort = 2005;
    private static boolean should_recover = false;

    public RMICustomerResourceManager(String name, IMiddlewareResourceManager midRM) {
        super(name, "customerData1.xml", "customerData2.xml", "customerMasterRecord.xml", "customerLogFile.txt", midRM);
    }

    public static void main(String args[]) {
        CliParser cliParser = new CliParser("RMICustomerResourceManager",args, new String[] {
                CliParser.SHOULD_RECOVER,
                CliParser.CUSTOMER_HOSTNAME,
                CliParser.CUSTOMER_PORT,
                CliParser.MIDDLEWARE_HOSTNAME,
                CliParser.MIDDLEWARE_PORT
        });
        if (cliParser.parsedArg(CliParser.CUSTOMER_HOSTNAME))
            s_serverHostname = cliParser.getParsedHostname(CliParser.CUSTOMER_HOSTNAME);
        if (cliParser.parsedArg(CliParser.CUSTOMER_PORT))
            s_serverPort = cliParser.getParsedPort(CliParser.CUSTOMER_PORT);
        if (cliParser.parsedArg(CliParser.MIDDLEWARE_HOSTNAME))
            s_middlewareHostname = cliParser.getParsedHostname(CliParser.MIDDLEWARE_HOSTNAME);
        if (cliParser.parsedArg(CliParser.MIDDLEWARE_PORT))
            s_middlewarePort = cliParser.getParsedPort(CliParser.MIDDLEWARE_PORT);
        if (cliParser.parsedArg(CliParser.SHOULD_RECOVER))
            should_recover = true;

        IMiddlewareResourceManager midRM = null;
        if (s_middlewareHostname == null || s_middlewarePort == -1) {
            // bad bad not good
        } else {
            midRM = RMIUtils.getRMIobject(s_middlewareHostname, s_middlewarePort, "MiddlewareServer");
        }
        // Create a new Server object
        RMICustomerResourceManager server = new RMICustomerResourceManager(s_serverName, midRM);

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

        // TODO: recover state
        if (should_recover) {
            System.out.println(BLUE.colorString("recovering state from files"));
        }

        // for recovery, force middleware to reconnect to carRM
        try {
            Thread.sleep(1000);
            midRM.reconnect("customer", s_serverHostname, s_serverPort, "CustomerServer");
        } catch (RemoteException e) {
            System.out.println(RED.colorString("Error: ")+"could not reconnect middleware to car RM");
        } catch (Exception e) {
            System.out.println(RED.colorString("RMICustomerRM: ")+e.toString());
            e.printStackTrace();
        }
    }
}
