package group25.Server.RMI;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import static group25.Utils.AnsiColors.RED;

public class RMIUtils {

    public static final String RMI_OBJECT_NAME_PREFIX = "group25_";

    public static <T> T getRMIobject(String hostname, int port, String name) {
        try {
            boolean first = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(hostname, port);
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
            System.out.println(RED.colorString("Server exception: ")+"Uncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
        return null; // never reached
    }
}
