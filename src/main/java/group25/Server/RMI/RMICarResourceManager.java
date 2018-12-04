// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package group25.Server.RMI;

import group25.Server.Interface.*;
import group25.Server.Common.*;
import group25.Utils.CliParser;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.util.*;

import java.rmi.Remote;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import static group25.Utils.AnsiColors.BLACK;
import static group25.Utils.AnsiColors.BLUE;
import static group25.Utils.AnsiColors.RED;

public class RMICarResourceManager extends CarResourceManager {

    private static final String s_serverName = "CarServer";

    private static String s_serverHostname = "localhost";
    private static int s_serverPort = 2001;
    private static String s_customerServerHostname = "localhost";
    private static int s_customerServerPort = 2003;
    private static String s_middlewareHostname = "localhost";
    private static int s_middlewarePort = 2005;

    private static boolean should_recover = false;

    public RMICarResourceManager(String name, IMiddlewareResourceManager midRM) {
        super(name, "carData1.xml", "carData2.xml", "carMasterRecord.xml", "carLogFile.xml", midRM);
    }

    public static void main(String args[]) {
        CliParser cliParser = new CliParser("RMICarResourceManager", args, new String[]{
                CliParser.SHOULD_RECOVER,
                CliParser.CAR_HOSTNAME,
                CliParser.CAR_PORT,
                CliParser.CUSTOMER_HOSTNAME,
                CliParser.CUSTOMER_PORT,
                CliParser.MIDDLEWARE_HOSTNAME,
                CliParser.MIDDLEWARE_PORT
        });
        if (cliParser.parsedArg(CliParser.CAR_HOSTNAME))
            s_serverHostname = cliParser.getParsedHostname(CliParser.CAR_HOSTNAME);
        if (cliParser.parsedArg(CliParser.CAR_PORT))
            s_serverPort = cliParser.getParsedPort(CliParser.CAR_PORT);
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

        // get middleware for voting
        IMiddlewareResourceManager midRM = null;
        if (s_middlewareHostname == null || s_middlewarePort == -1) {
            System.out.println(RED.colorString("Error: ")+"CarRM failed to parse middleware args");
        } else {
            midRM = RMIUtils.getRMIobject(s_middlewareHostname, s_middlewarePort, "MiddlewareServer");
        }

        // Create a new Server object
        RMICarResourceManager server = new RMICarResourceManager(s_serverName, midRM);
        server.customerRM = RMIUtils.getRMIobject(s_customerServerHostname, s_customerServerPort, "CustomerServer");

        // Dynamically generate the stub (client proxy)
        ICarResourceManager resourceManager = RMIUtils.createRMIproxyObject(server, 0);

        // get local registry
        final Registry registry = RMIUtils.createLocalRMIregistry(s_serverPort);

        // Bind the remote object's stub in the registry
        RMIUtils.bindToRegistry(registry, s_serverName, resourceManager);

        // remove object from registry on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            RMIUtils.unbindFromRegistry(registry, s_serverName);
        }));

        System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_serverName + "'");

        // recover state
        if (should_recover) {
            server.recover();
            System.out.println(BLUE.colorString("recovering state from files"));
        } else {
            // delete files?
        }

        // for recovery, force middleware to reconnect to carRM
        try {
            Thread.sleep(1000);
            midRM.reconnect("car", s_serverHostname, s_serverPort, "CarServer");
        } catch (RemoteException e) {
            System.out.println(RED.colorString("Error: ")+"could not reconnect middleware to car RM");
        } catch (Exception e) {
            System.out.println(RED.colorString("RMICarRM: ")+e.toString());
            e.printStackTrace();
        }
    }
}
