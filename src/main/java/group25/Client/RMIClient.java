package group25.Client;

import group25.Server.RMI.RMIUtils;
import group25.Utils.CliParser;

import static group25.Utils.AnsiColors.BLUE;
import static group25.Utils.AnsiColors.RED;

public class RMIClient extends Client {

    private static final String s_serverName = "MiddlewareServer";

    private static String s_middlewareHostname = "localhost";
    private static int s_middlewarePort = 2005;

    public static void main(String args[]) {
        CliParser cliParser = new CliParser("RMIClient",args, new String[]{
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

        // Connect to middleware and start running client
        try {
            RMIClient client = new RMIClient();
            client.connectServer();
            client.start();
        } catch (Exception e) {
            System.err.println(RED.colorString("Client exception: ")+"Uncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void connectServer() {
        m_resourceManager = RMIUtils.getRMIobject(s_middlewareHostname, s_middlewarePort, s_serverName);
    }
}
