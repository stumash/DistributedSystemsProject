// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package group25.Server.RMI;

import group25.Server.Interface.*;
import group25.Server.Common.*;
import group25.Utils.CliParser;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import static group25.Utils.AnsiColors.BLUE;
import static group25.Utils.AnsiColors.RED;

public class RMIFlightResourceManager extends FlightResourceManager {

    public static final String s_serverName = "FlightServer";

    private static String s_serverHostname = "localhost";
    private static int s_serverPort = 2000;
    private static String s_customerServerHostname = "localhost";
    private static int s_customerServerPort = 2003;
    private static String s_middlewareHostname = "localhost";
    private static int s_middlewarePort = 2005;

    private static boolean should_recover = false;

    public RMIFlightResourceManager(String name, IMiddlewareResourceManager midRM) {
        super(name, "flightData1.xml", "flightData2.xml", "flightMasterRecord.xml", "flightLogFile.txt", midRM);
    }

    public static void main(String args[]) {
        CliParser cliParser = new CliParser("RMIFlightResourceManager",args, new String[] {
                CliParser.SHOULD_RECOVER,
                CliParser.FLIGHT_HOSTNAME,
                CliParser.FLIGHT_PORT,
                CliParser.CUSTOMER_HOSTNAME,
                CliParser.CUSTOMER_PORT,
                CliParser.MIDDLEWARE_HOSTNAME,
                CliParser.MIDDLEWARE_PORT
        });
        if (cliParser.parsedArg(CliParser.FLIGHT_HOSTNAME))
            s_serverHostname = cliParser.getParsedHostname(CliParser.FLIGHT_HOSTNAME);
        if (cliParser.parsedArg(CliParser.FLIGHT_PORT))
            s_serverPort = cliParser.getParsedPort(CliParser.FLIGHT_PORT);
        if (cliParser.parsedArg(CliParser.CUSTOMER_HOSTNAME))
            s_customerServerHostname = cliParser.getParsedHostname(CliParser.CUSTOMER_HOSTNAME);
        if (cliParser.parsedArg(CliParser.CUSTOMER_PORT))
            s_customerServerPort = cliParser.getParsedPort(CliParser.CUSTOMER_PORT);
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
        RMIFlightResourceManager server = new RMIFlightResourceManager(s_serverName, midRM);
        server.customerRM = RMIUtils.getRMIobject(s_customerServerHostname, s_customerServerPort, "CustomerServer");

        // Dynamically generate the stub (client proxy)
        IFlightResourceManager resourceManager = RMIUtils.createRMIproxyObject(server,0);

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
            System.out.println(BLUE.colorString("recovering global state from files"));
        }

        // for recovery, force middleware to reconnect to carRM
        try {
            Thread.sleep(1000);
            midRM.reconnect("flight", s_serverHostname, s_serverPort, "FlightServer");
        } catch (RemoteException e) {
            System.out.println(RED.colorString("Error: ")+"could not reconnect middleware to car RM");
        } catch (Exception e) {
            System.out.println(RED.colorString("RMIFlightRM: ")+e.toString());
            e.printStackTrace();
        }
    }
}
