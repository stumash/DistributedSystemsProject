package group25.Server.RMI;

import group25.Server.Interface.*;
import group25.Server.Common.*;

import java.rmi.NotBoundException;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;

public class RMIMiddlewareResourceManager extends MiddlewareResourceManager {
    private static String s_serverName = "MiddlewareServer";
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
        if (args.length > 0) {
            try {
                s_serverPort = Integer.parseInt(args[0]);
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m1st arg must be integer for middlewareserver port (default 2005)");
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
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m3rd arg must be integer for customerserver port (default 2003)");
                e.printStackTrace();
                System.exit(1);
            }
        }
        if (args.length > 3) {
            s_flightServerHostname = args[3];
        }
        if (args.length > 4) {
            try {
                s_flightServerPort = Integer.parseInt(args[4]);
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m5th arg must be integer for flightserver port (default 2000)");
                e.printStackTrace();
                System.exit(1);
            }
        }
        if (args.length > 5) {
            s_roomServerHostname = args[5];
        }
        if (args.length > 6) {
            try {
                s_roomServerPort = Integer.parseInt(args[6]);
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m7th arg must be integer for roomserver port (default 2002)");
                e.printStackTrace();
                System.exit(1);
            }
        }
        if (args.length > 7) {
            s_carServerHostname = args[7];
        }
        if (args.length > 8) {
            try {
                s_carServerPort = Integer.parseInt(args[8]);
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m9th arg must be integer for carserver port (default 2001)");
                e.printStackTrace();
                System.exit(1);
            }
        }

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
