package group25.Server.RMI;

import group25.Server.Interface.*;
import group25.Server.Common.*;
import group25.Utils.CliParser;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;

public class RMIMiddlewareResourceManager extends MiddlewareResourceManager {

    public static final String s_serverName = "MiddlewareServer";

    private static int s_serverPort = 2005;
    private static String s_customerServerHostname = "localhost";
    private static int s_customerServerPort = 2003;
    private static String s_flightServerHostname = "localhost";
    private static int s_flightServerPort = 2000;
    private static String s_roomServerHostname = "localhost";
    private static int s_roomServerPort = 2002;
    private static String s_carServerHostname = "localhost";
    private static int s_carServerPort = 2001;

    public RMIMiddlewareResourceManager(String name) {
        super(name);
    }

    public static void main(String args[]) {
        CliParser cliParser = new CliParser("RMIMiddlewareResourceManager",args, new String[] {
                CliParser.MIDDLEWARE_PORT,
                CliParser.CUSTOMER_HOSTNAME,
                CliParser.CUSTOMER_PORT,
                CliParser.FLIGHT_HOSTNAME,
                CliParser.FLIGHT_PORT,
                CliParser.ROOM_HOSTNAME,
                CliParser.ROOM_PORT,
                CliParser.CAR_HOSTNAME,
                CliParser.CAR_PORT
        });
        if (cliParser.parsedArg(CliParser.MIDDLEWARE_PORT))
            s_serverPort = cliParser.getParsedPort(CliParser.MIDDLEWARE_PORT);
        if (cliParser.parsedArg(CliParser.CUSTOMER_HOSTNAME))
            s_customerServerHostname = cliParser.getParsedHostname(CliParser.CUSTOMER_HOSTNAME);
        if (cliParser.parsedArg(CliParser.CUSTOMER_PORT))
            s_customerServerPort = cliParser.getParsedPort(CliParser.CUSTOMER_PORT);
        if (cliParser.parsedArg(CliParser.FLIGHT_HOSTNAME))
            s_flightServerHostname = cliParser.getParsedHostname(CliParser.FLIGHT_HOSTNAME);
        if (cliParser.parsedArg(CliParser.FLIGHT_PORT))
            s_flightServerPort = cliParser.getParsedPort(CliParser.FLIGHT_PORT);
        if (cliParser.parsedArg(CliParser.ROOM_HOSTNAME))
            s_roomServerHostname = cliParser.getParsedHostname(CliParser.ROOM_HOSTNAME);
        if (cliParser.parsedArg(CliParser.ROOM_PORT))
            s_roomServerPort = cliParser.getParsedPort(CliParser.ROOM_PORT);
        if (cliParser.parsedArg(CliParser.CAR_HOSTNAME))
            s_carServerHostname = cliParser.getParsedHostname(CliParser.CAR_HOSTNAME);
        if (cliParser.parsedArg(CliParser.CAR_PORT))
            s_carServerPort = cliParser.getParsedPort(CliParser.CAR_PORT);

        // Create a new Server object
        RMIMiddlewareResourceManager server = new RMIMiddlewareResourceManager(s_serverName);

        // Dynamically generate the stub (client proxy)
        IMiddlewareResourceManager resourceManager = RMIUtils.createRMIproxyObject(server,0);

        // get local registry
        final Registry registry = RMIUtils.createLocalRMIregistry(s_serverPort);

        // Bind the remote object's stub in the registry
        RMIUtils.bindToRegistry(registry,s_serverName,resourceManager);

        // remove object from registry on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            RMIUtils.unbindFromRegistry(registry,s_serverName);
        }));

        System.out.println("'"+s_serverName+"' resource manager server ready and bound to '"+s_serverName+"'");

        server.flightRM = RMIUtils.getRMIobject(s_flightServerHostname,s_flightServerPort,"FlightServer");
        server.carRM = RMIUtils.getRMIobject(s_carServerHostname,s_carServerPort,"CarServer");
        server.roomRM = RMIUtils.getRMIobject(s_roomServerHostname,s_roomServerPort,"RoomServer");
        server.customerRM = RMIUtils.getRMIobject(s_customerServerHostname,s_customerServerPort,"CustomerServer");

        server.transactionManager = new TransactionManager(server.carRM, server.flightRM, server.roomRM, server.customerRM);

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
    }

}
