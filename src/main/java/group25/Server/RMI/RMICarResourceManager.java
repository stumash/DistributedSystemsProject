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

public class RMICarResourceManager extends CarResourceManager {

    private static final String s_serverName = "CarServer";

    private static int s_serverPort = 2001;
    private static String s_customerServerHostname = "localhost";
    private static int s_customerServerPort = 2003;

    public RMICarResourceManager(String name) {
        super(name);
    }

    public static void main(String args[]) {
        CliParser cliParser = new CliParser("RMICarResourceManager", args, new String[]{
                CliParser.CAR_PORT,
                CliParser.CUSTOMER_HOSTNAME,
                CliParser.CUSTOMER_PORT
        });
        if (cliParser.parsedArg(CliParser.CAR_PORT))
            s_serverPort = cliParser.getParsedPort(CliParser.CAR_PORT);
        if (cliParser.parsedArg(CliParser.CUSTOMER_HOSTNAME))
            s_customerServerHostname = cliParser.getParsedHostname(CliParser.CUSTOMER_HOSTNAME);
        if (cliParser.parsedArg(CliParser.CUSTOMER_PORT))
            s_customerServerPort = cliParser.getParsedPort(CliParser.CUSTOMER_PORT);

        // Create a new Server object
        RMICarResourceManager server = new RMICarResourceManager(s_serverName);
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
    }
}
