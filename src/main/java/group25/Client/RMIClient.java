package group25.Client;

import group25.Server.Interface.*;
import group25.Server.RMI.RMIUtils;
import group25.Utils.CliParser;
import org.apache.commons.cli.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

import static group25.Utils.AnsiColors.RED;

public class RMIClient extends Client {

    private static final String s_serverName = "MiddlewareServer";

    private static String s_middlewareHostname = "localhost";
    private static int s_middlewarePort = 2005;

    public static void main(String args[]) {
        CliParser cliParser = new CliParser(args, new String[]{
                CliParser.MIDDLEWARE_HOSTNAME,
                CliParser.MIDDLEWARE_PORT
        });
        if (cliParser.parsedArg(CliParser.MIDDLEWARE_HOSTNAME))
            s_middlewareHostname = cliParser.getParsedHostname(CliParser.MIDDLEWARE_HOSTNAME);
        if (cliParser.parsedArg(CliParser.MIDDLEWARE_PORT))
            s_middlewarePort = cliParser.getParsedPort(CliParser.MIDDLEWARE_PORT);

        // Set the security policy
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        // Get a reference to the RMIRegister
        try {
            RMIClient client = new RMIClient();
            client.m_resourceManager = RMIUtils.getRMIobject(s_middlewareHostname, s_middlewarePort, s_serverName);
//            client.connectServer();
            client.start();
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public RMIClient() {
        super();
    }

    public void connectServer() {
//        connectServer(s_middlewareHostname, s_middlewarePort, s_serverName);
    }

//    public void connectServer(String server, int port, String name) {
//        try {
//            boolean first = true;
//            while (true) {
//                try {
//                    Registry registry = LocateRegistry.getRegistry(server, port);
//                    m_resourceManager = (IResourceManager) registry.lookup(s_rmiPrefix + name);
//                    System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
//                    break;
//                } catch (NotBoundException | RemoteException e) {
//                    if (first) {
//                        System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
//                        first = false;
//                    }
//                }
//                Thread.sleep(500);
//            }
//        } catch (Exception e) {
//            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
//            e.printStackTrace();
//            System.exit(1);
//        }
//    }
}
