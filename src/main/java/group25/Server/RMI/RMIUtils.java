package group25.Server.RMI;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import static group25.Utils.AnsiColors.RED;

/**
 * Holds static methods for dealing with java RMI.
 */
public class RMIUtils {

    private static final String RMI_OBJECT_NAME_PREFIX = "group25_";

    public static <T> T createRMIproxyObject(Remote objToStub, int port) {
        T retval = null;
        try {
            retval = (T) UnicastRemoteObject.exportObject(objToStub, port);
        } catch (RemoteException e) {
            System.out.println(RED.colorString("Server exception: ")+"System.exit(1) called in RMIUtils.createRMIproxyObject(obj,port)");
            e.printStackTrace();
            System.exit(1);
        }
        return retval;
    }

    public static void bindToRegistry(Registry registry, String rmiObjName, Remote rmiObj) {
        try {
            registry.rebind(RMI_OBJECT_NAME_PREFIX+rmiObjName, rmiObj);
        } catch (RemoteException e) {
            System.out.println(RED.colorString("Server exception: ")+"System.exit(1) called in RMIUtils.bindToRegistry(registry,objName,obj)");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void unbindFromRegistry(Registry registry, String rmiObjName) {
        try {
            registry.unbind(RMI_OBJECT_NAME_PREFIX+rmiObjName);
            System.out.println("'"+rmiObjName+"' resource manager unbound");
        } catch (Exception e) {
            System.out.println(RED.colorString("Server exception: ")+"Uncaught exception");
            e.printStackTrace();
        }
    }

    public static <T> T getRMIobject(String hostname, int port, String name) {
        try {
            boolean first = true;
            while (true) {
                try {
                    // get a remote registry
                    Registry registry = LocateRegistry.getRegistry(hostname, port);
                    // get object from remote registry
                    T retval = (T)registry.lookup(RMI_OBJECT_NAME_PREFIX + name);
                    System.out.println("Connected to '"+name+"' server ["+hostname+":"+port+"/"+RMI_OBJECT_NAME_PREFIX+name+"]");
                    return retval;
                } catch (NotBoundException | RemoteException e) {
                    if (first) {
                        System.out.println("Waiting for '"+name+"' server ["+hostname+":"+port+"/"+RMI_OBJECT_NAME_PREFIX+name+"]");
                        first = false;
                    }
                }
                Thread.sleep(500);
            }
        } catch (Exception e) {
            System.out.println(RED.colorString("Server exception: ")+"System.exit(1) called in RMIUtils.getRMIobject(host,port,name)");
            e.printStackTrace();
            System.exit(1);
        }
        return null; // never reached
    }

    public static Registry createLocalRMIregistry(int port) {
        Registry l_registry = null;
        try {
            l_registry = LocateRegistry.createRegistry(port);
        } catch (RemoteException e) {
            try {
                l_registry = LocateRegistry.getRegistry(port);
            } catch (RemoteException e1) {
                System.out.println(RED.colorString("Server exception: ")+"System.exit(1) called RMIUtils.createLocalRMIregistry(port)");
                e1.printStackTrace();
                System.exit(1);
            }
        }
        return l_registry;
    }
}
