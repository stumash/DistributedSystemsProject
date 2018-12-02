// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package group25.Server.RMI;

import group25.Server.Interface.*;
import group25.Server.Common.*;
import group25.Utils.CliParser;

import java.rmi.registry.Registry;

public class RMIFlightResourceManager extends FlightResourceManager {

    public static final String s_serverName = "FlightServer";

    private static int s_serverPort = 2000;
    private static String s_customerServerHostname = "localhost";
    private static int s_customerServerPort = 2003;

    public RMIFlightResourceManager(String name, IMiddlewareResourceManager midRM) {
        super(name, "flightData1.xml", "flightData2.xml", "flightMasterRecord.xml", "flightLogFile.txt", midRM);
    }

    public static void main(String args[]) {
        CliParser cliParser = new CliParser("RMIFlightResourceManager",args, new String[] {
                CliParser.FLIGHT_PORT,
                CliParser.CUSTOMER_HOSTNAME,
                CliParser.CUSTOMER_PORT,
                CliParser.MIDDLEWARE_HOSTNAME,
                CliParser.MIDDLEWARE_PORT
        });
        if (cliParser.parsedArg(CliParser.FLIGHT_PORT))
            s_serverPort = cliParser.getParsedPort(CliParser.FLIGHT_PORT);
        if (cliParser.parsedArg(CliParser.CUSTOMER_HOSTNAME))
            s_customerServerHostname = cliParser.getParsedHostname(CliParser.CUSTOMER_HOSTNAME);
        if (cliParser.parsedArg(CliParser.CUSTOMER_PORT))
            s_customerServerPort = cliParser.getParsedPort(CliParser.CUSTOMER_PORT);

        String middlewareHostname = null;
        int middlewarePort = -1;
        IMiddlewareResourceManager midRM = null;
        if (cliParser.parsedArg(CliParser.MIDDLEWARE_HOSTNAME))
            middlewareHostname = cliParser.getParsedHostname(CliParser.MIDDLEWARE_HOSTNAME);
        if (cliParser.parsedArg(CliParser.MIDDLEWARE_PORT))
            middlewarePort = cliParser.getParsedPort(CliParser.MIDDLEWARE_PORT);
        if (middlewareHostname == null || middlewarePort == -1) {
            // bad bad not good
        } else {
            midRM = RMIUtils.getRMIobject(middlewareHostname, middlewarePort, "MiddlewareServer");
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
    }
}
