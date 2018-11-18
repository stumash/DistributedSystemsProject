// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package group25.Server.RMI;

import group25.Server.Interface.*;
import group25.Server.Common.*;
import group25.Utils.CliParser;

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

public class RMIRoomResourceManager extends RoomResourceManager implements IRemoteResourceManagerGetter {
    private static String s_serverName = "RoomServer";
    private static int s_serverPort = 2002;
    private static String s_rmiPrefix = "group25_";
    private static String s_customerServerHostname = "localhost";
    private static int s_customerServerPort = 2003;

    public static void main(String args[]) {
        CliParser cliParser = new CliParser("RMIRoomResourceManager",args, new String[] {
                CliParser.ROOM_PORT,
                CliParser.CUSTOMER_HOSTNAME,
                CliParser.CUSTOMER_PORT
        });
        if (cliParser.parsedArg(CliParser.ROOM_PORT))
            s_serverPort = cliParser.getParsedPort(CliParser.ROOM_PORT);
        if (cliParser.parsedArg(CliParser.CUSTOMER_HOSTNAME))
            s_customerServerHostname = cliParser.getParsedHostname(CliParser.CUSTOMER_HOSTNAME);
        if (cliParser.parsedArg(CliParser.CUSTOMER_PORT))
            s_customerServerPort = cliParser.getParsedPort(CliParser.CUSTOMER_PORT);

        // Create the RMI server entry
        try {
            // Create a new Server object
            RMIRoomResourceManager server = new RMIRoomResourceManager(s_serverName);

            server.customerRM = (ICustomerResourceManager) server.getRemoteResourceManager(s_customerServerHostname,s_customerServerPort, "CustomerServer");

            // Dynamically generate the stub (client proxy)
            IRoomResourceManager resourceManager = (IRoomResourceManager) UnicastRemoteObject.exportObject(server, 0);

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

    public RMIRoomResourceManager(String name) {
        super(name);
    }
}
