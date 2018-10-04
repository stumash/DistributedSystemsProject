package Server.TCP;

import java.net.Socket;
import java.io.*;

import Server.Common.*;

public class TCPCustomerResourceManager extends CustomerResourceManager {
    private static String s_serverName = "CustomerServer";
    private static String s_serverHost = "localhost";
    private static int s_serverPort = 2003;
    private static String s_tcpPrefix = "group25_";

    public static void main(String[] args) {
        if (args.length > 0) {
          s_serverHost = args[0];
        }
        if (args.length > 1) {
          try {
              s_serverPort = Integer.parseInt(args[1]);
          } catch (Exception e) {
              System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m1st arg must be integer for customerserver port (default 2003)");
              e.printStackTrace();
              System.exit(1);
          }
        }

        TCPProxyObjectServer server = new TCPProxyObjectServer(s_serverHost, s_serverPort);
        TCPCustomerResourceManager customerRM = new TCPCustomerResourceManager(s_serverName);

        server.bind(s_tcpPrefix + s_serverName, customerRM);
        server.runServer();
        System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_tcpPrefix + s_serverName + "'");
    }

    public TCPCustomerResourceManager(String name) {
        super(name);
    }
}
