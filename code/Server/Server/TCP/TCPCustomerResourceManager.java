package Server.TCP;

import java.net.Socket;
import java.io.*;

import Server.Common.*;

public class TCPCustomerResourceManager extends CustomerResourceManager {
    private static String s_serverName = "CustomerServer";
    private static String s_tcpPrefix = "group25_";

    public static void main(String[] args) {
        TCPProxyObjectServer server = new TCPProxyObjectServer("localhost", 2003);
        TCPCustomerResourceManager customerRM = new TCPCustomerResourceManager(s_serverName);

        server.bind(s_tcpPrefix + s_serverName, customerRM);
        server.runServer();
        System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_tcpPrefix + s_serverName + "'");
    }

    public TCPCustomerResourceManager(String name) {
        super(name);
    }
}
