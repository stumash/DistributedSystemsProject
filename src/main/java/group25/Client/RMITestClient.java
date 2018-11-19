package group25.Client;

import group25.Server.Interface.IMiddlewareResourceManager;
import group25.Server.LockManager.DeadlockException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.rmi.RemoteException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.concurrent.CyclicBarrier;

public class RMITestClient {
    private static String s_middlewareHostname = "localhost";
    private static int s_middlewarePort = 2005;
    private static String s_serverName = "MiddlewareServer";
    private static String s_rmiPrefix = "group25_";
    private static boolean s_multiTestClient = false;
    private static long s_minTime = 200; // millis
    private static int s_numClients = 5;

    private static final int NUM_CALLS = 100;
    private static String path = System.getProperty("user.home") + File.separator +
            "group25_data_client_";

    public static void main(String args[]) throws IOException, InterruptedException, DeadlockException {
        // parse all args
        if (args.length > 0) {
            s_middlewareHostname = args[0];
        }
        if (args.length > 1) {
            try {
                s_middlewarePort = Integer.parseInt(args[1]);
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m2nd arg must be integer for middleware port");
                e.printStackTrace();
                System.exit(1);
            }
        }
        if (args.length > 2) {
            try {
                s_multiTestClient = Boolean.parseBoolean(args[2]);
                path += (s_multiTestClient?"multi":"single");
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m3rd arg must be boolean multi test client");
                e.printStackTrace();
                System.exit(1); 
            }
        }
        if (args.length > 3) {
            try {
                s_minTime = Long.parseLong(args[3]);
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m4th arg must be long min method call time");
                e.printStackTrace();
                System.exit(1);           
            }
        }
        if (args.length > 4) {
            try {
                s_numClients = Integer.parseInt(args[4]);
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m5th arg must be int num clients");
                e.printStackTrace();
                System.exit(1);           
            }
        }
        if (args.length > 5) {
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUsage: java client.RMIClient [server_hostname [server_rmiobject [multi_client [min_time [num_cilents]]]]]");
            System.exit(1);
        }

        // set the security policy
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        // set up the csv writer
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                    .withHeader("methodCall", "startTime", "endTime"));

        // get a resource manager (the middleware)
        IMiddlewareResourceManager resourceManager = connectServer(s_middlewareHostname, s_middlewarePort, s_serverName);

        // test the various methods and measure the throughput as a proxy for general system performance

        /**
         * addCustomers, then query Cars+Flights+Rooms = 2 tests
         */

        // test1: addCustomers
        
        if (!s_multiTestClient) {
            int xid = resourceManager.start();
            System.out.print("addCustomers test: ");
            for (int i = 0; i < NUM_CALLS*3; i++) {
                long startTime = System.currentTimeMillis();
                resourceManager.newCustomer(xid);
                long endTime = System.currentTimeMillis();
                System.out.print("-");

                // log one line to csv file
                csvPrinter.printRecord("newCustomer", startTime, endTime);
                csvPrinter.flush();
            }
            System.out.println();

            resourceManager.commit(xid);

            // test2: queryCars, queryFlights, queryRooms
            xid = resourceManager.start();

            System.out.print("addCars/Flights/Rooms test: ");
            for (int i = 0; i < NUM_CALLS; i++) {
                for (int j = 0; j < 3; j++) {
                    String methodCallName = "";
                    long startTime = System.currentTimeMillis();
                    if (j == 0) {
                        methodCallName = "queryCars";
                        resourceManager.queryCars(xid, "loc");
                    } else if (j == 1) {
                        methodCallName = "queryFlights";
                        resourceManager.queryFlight(xid, 789);
                    } else if (j == 2) {
                        methodCallName = "queryRooms";
                        resourceManager.queryRooms(xid, "loc");
                    }
                    long endTime = System.currentTimeMillis();

                    System.out.print("-");

                    // log one line to csv file
                    csvPrinter.printRecord(methodCallName, startTime, endTime);
                    csvPrinter.flush();
                }
            }
            System.out.println();

            resourceManager.commit(xid);
        } else {
            CyclicBarrier barrier = new CyclicBarrier(s_numClients);
            Thread[] threads = new Thread[s_numClients];
            for (int t = 0; t < s_numClients; t++) {
                threads[t] = new Thread(() -> {
                    try {
                        int xid = resourceManager.start();
                        System.out.print("addCustomers test: ");
                        for (int i = 0; i < NUM_CALLS*3/s_numClients; i++) {
                            long startTime = System.currentTimeMillis();
                            resourceManager.newCustomer(xid);
                            long endTime = System.currentTimeMillis();
                            System.out.print("-");
    
                            // log one line to csv file
                            synchronized(csvPrinter) {
                                csvPrinter.printRecord("newCustomer", startTime, endTime);
                                csvPrinter.flush();
                            }
    
                            long timeTaken = System.currentTimeMillis() - startTime;
                            if (timeTaken > 0 && timeTaken < s_minTime) {
                                Thread.sleep(s_minTime - timeTaken);
                            }
                        }
                        System.out.println();
    
                        resourceManager.commit(xid);
    
                        // nobody goes to test2 until all have finished test1
                        barrier.await();

                        // test2: queryCars, queryFlights, queryRooms
                        xid = resourceManager.start();
    
                        System.out.print("addCars/Flights/Rooms test: ");
                        for (int i = 0; i < NUM_CALLS/s_numClients; i++) {
                            for (int j = 0; j < 3; j++) {
                                String methodCallName = "";
                                long startTime = System.currentTimeMillis();
                                if (j == 0) {
                                    methodCallName = "queryCars";
                                    resourceManager.queryCars(xid, "loc");
                                } else if (j == 1) {
                                    methodCallName = "queryFlights";
                                    resourceManager.queryFlight(xid, 789);
                                } else if (j == 2) {
                                    methodCallName = "queryRooms";
                                    resourceManager.queryRooms(xid, "loc");
                                }
                                long endTime = System.currentTimeMillis();
    
                                System.out.print("-");
    
                                // log one line to csv file
                                synchronized(csvPrinter) {
                                    csvPrinter.printRecord(methodCallName, startTime, endTime);
                                    csvPrinter.flush();
                                }
    
                                long timeTaken = System.currentTimeMillis() - startTime;
                                if (timeTaken > 0 && timeTaken < s_minTime) {
                                    Thread.sleep(s_minTime - timeTaken);
                                }
                            }
                        }
                        System.out.println();
    
                        resourceManager.commit(xid);

                    } catch (Exception e) {
                        System.out.println("are u for real?!");
                        e.printStackTrace();
                    }
                });
            }
            for (Thread t : threads) t.start();
            for (Thread t : threads) t.join();
        }
        
    }

    private static IMiddlewareResourceManager connectServer(String server, int port, String name) {
        try {
            boolean first = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(server, port);
                    IMiddlewareResourceManager resourceManager = (IMiddlewareResourceManager) registry.lookup(s_rmiPrefix + name);
                    System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
                    return resourceManager;
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
        return null;
    }
}