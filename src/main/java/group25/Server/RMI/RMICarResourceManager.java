// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package group25.Server.RMI;

import group25.Server.Interface.*;
import group25.Server.Common.*;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.util.*;

import java.rmi.Remote;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMICarResourceManager extends CarResourceManager implements IRemoteResourceManagerGetter {
    private static String s_serverName = "CarServer";
    private static int s_serverPort = 2001;
    private static String s_rmiPrefix = "group25_";
    private static String s_customerServerHostname = "localhost";
    private static int s_customerServerPort = 2003;


    public static void main(String args[]) {
        if (args.length > 0) {
            try {
                s_serverPort = Integer.parseInt(args[0]);
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m1st arg must be integer for carserver port (default 2001)");
                e.printStackTrace();
                System.exit(1);
            }
        }
        if (args.length > 1) {
            s_customerServerHostname = args[1];
        }
        if (args.length > 2) {
            try {
                s_customerServerPort = Integer.parseInt(args[2]);
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m3rd arg must be integer for customer server port (default 2003)");
                e.printStackTrace();
                System.exit(1);
            }
        }

        // Create the RMI server entry
        try {
            // Create a new resource manager object
            RMICarResourceManager server = new RMICarResourceManager(s_serverName);

            // get a reference to a customerRM
            server.customerRM = (ICustomerResourceManager) server.getRemoteResourceManager(s_customerServerHostname, s_customerServerPort, "CustomerServer");

            // Dynamically generate the stub (client proxy)
            ICarResourceManager resourceManager = (ICarResourceManager) UnicastRemoteObject.exportObject(server, 0);

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

    public RMICarResourceManager(String name) {
        super(name);
    }
}
