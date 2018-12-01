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

public class RMIRoomResourceManager extends RoomResourceManager {

    private static final String s_serverName = "RoomServer";

    private static int s_serverPort = 2002;
    private static String s_customerServerHostname = "localhost";
    private static int s_customerServerPort = 2003;

    public RMIRoomResourceManager(String name) {
        super(name, RMIUtils.DATA_FILE_PATH+"/roomData1.xml", RMIUtils.DATA_FILE_PATH+"/roomData2.xml", RMIUtils.DATA_FILE_PATH+"/roomMasterRecord.xml");
    }

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

        // Create a new Server object
        RMIRoomResourceManager server = new RMIRoomResourceManager(s_serverName);
        server.customerRM = RMIUtils.getRMIobject(s_customerServerHostname, s_customerServerPort, "CustomerServer");

        // Dynamically generate the stub (client proxy)
        IRoomResourceManager resourceManager = RMIUtils.createRMIproxyObject(server,0);

        // get local registry
        final Registry registry = RMIUtils.createLocalRMIregistry(s_serverPort);

        // Bind the remote object's stub in the registry
        RMIUtils.bindToRegistry(registry,s_serverName,resourceManager);

        // remove object from registry on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            RMIUtils.unbindFromRegistry(registry,s_serverName);
        }));

        System.out.println("'"+s_serverName+"' resource manager server ready and bound to '"+s_serverName+"'");
    }
}
