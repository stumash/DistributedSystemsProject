package group25.Server.RMI;

import group25.Server.Interface.*;
import group25.Server.Common.*;
import group25.Utils.CliParser;

import java.rmi.NotBoundException;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;

import static group25.Utils.AnsiColors.BLUE;

public class RMIMiddlewareResourceManager extends MiddlewareResourceManager {
    private static final String s_serverName = "MiddlewareServer";
    private static int s_serverPort = 2005;
    private static String s_rmiPrefix = "group25_";
    private static String s_customerServerHostname = "localhost";
    private static int s_customerServerPort = 2003;
    private static String s_flightServerHostname = "localhost";
    private static int s_flightServerPort = 2000;
    private static String s_roomServerHostname = "localhost";
    private static int s_roomServerPort = 2002;
    private static String s_carServerHostname = "localhost";
    private static int s_carServerPort = 2001;

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

        // Create the RMI server entry
        try {
            // Create a new Server object
            RMIMiddlewareResourceManager server = new RMIMiddlewareResourceManager(s_serverName);

            // Dynamically generate the stub (client proxy)
            IResourceManager resourceManager = (IResourceManager) UnicastRemoteObject.exportObject(server, 0);

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

            server.flightRM = (IFlightResourceManager) server.getRemoteResourceManager(s_flightServerHostname,s_flightServerPort, "FlightServer");
            server.carRM = (ICarResourceManager) server.getRemoteResourceManager(s_carServerHostname,s_carServerPort, "CarServer");
            server.roomRM = (IRoomResourceManager) server.getRemoteResourceManager(s_roomServerHostname,s_roomServerPort, "RoomServer");
            server.customerRM = (ICustomerResourceManager) server.getRemoteResourceManager(s_customerServerHostname,s_customerServerPort, "CustomerServer");

            server.transactionManager = new TransactionManager(server.carRM, server.flightRM, server.roomRM, server.customerRM);
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

    public RMIMiddlewareResourceManager(String name) {
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
                } catch (NotBoundException | RemoteException e) {
                    if (first) {
                        System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
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
}
