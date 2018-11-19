package group25.Client;

import group25.Server.Interface.IMiddlewareResourceManager;
import group25.Server.LockManager.DeadlockException;
import static group25.Utils.AnsiColors.*;

import java.util.*;
import java.io.*;
import java.rmi.RemoteException;
import java.rmi.ConnectException;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;

public abstract class Client {
    IMiddlewareResourceManager m_resourceManager = null;

    public abstract void connectServer();

    public void start() {
        // Prepare for reading commands
        System.out.println();
        System.out.println("Location \"help\" for list of supported commands");

        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            // Read the next command
            String command = "";
            Vector<String> arguments = new Vector<String>();
            Command cmd = null;
            try {
                System.out.print(GREEN.colorString(">>> "));
                command = stdin.readLine().trim();
            } catch (IOException io) {
                printCommandException(io.getLocalizedMessage());
                io.printStackTrace();
                System.exit(1);
            }

            try {
                arguments = parse(command);
                cmd = Command.fromString((String) arguments.elementAt(0));
                try {
                    execute(cmd, arguments);
                } catch (ConnectException e) {
                    connectServer();
                    execute(cmd, arguments);
                }
            } catch (IllegalArgumentException | ServerException e) {
                printCommandException(e.getLocalizedMessage());
            } catch (ConnectException | UnmarshalException e) {
                if (cmd != Command.Shutdown) {
                    printCommandException("Connection to server lost");
                } else {
                    System.out.println("Shutting down client");
                    System.exit(0);
                }
            } catch (DeadlockException e) {
                printCommandException("Deadlock");
            } catch (Exception e) {
                printCommandException("Uncaught exception");
                e.printStackTrace();
            }
        }
    }

    private void execute(Command cmd, Vector<String> arguments) throws RemoteException, NumberFormatException, DeadlockException {
        switch (cmd) {
            case Help: {
                help(arguments);
                break;
            }
            case Start: {
                start(arguments);
                break;
            }
            case Commit: {
                commit(arguments);
                break;
            }
            case Abort: {
                abort(arguments);
                break;
            }
            case AddFlight: {
                addFlight(arguments);
                break;
            }
            case AddCars: {
                addCars(arguments);
                break;
            }
            case AddRooms: {
                addRooms(arguments);
                break;
            }
            case AddCustomer: {
                addCustomer(arguments);
                break;
            }
            case AddCustomerID: {
                addCustomerid(arguments);
                break;
            }
            case DeleteFlight: {
                deleteFlight(arguments);
                break;
            }
            case DeleteCars: {
                deleteCars(arguments);
                break;
            }
            case DeleteRooms: {
                deleteRooms(arguments);
                break;
            }
            case DeleteCustomer: {
                deleteCustomer(arguments);
                break;
            }
            case QueryFlight: {
                queryFlight(arguments);
                break;
            }
            case QueryCars: {
                queryCars(arguments);
                break;
            }
            case QueryRooms: {
                queryRooms(arguments);
                break;
            }
            case QueryCustomer: {
                queryCustomer(arguments);
                break;
            }
            case QueryFlightPrice: {
                queryFlightPrice(arguments);
                break;
            }
            case QueryCarsPrice: {
                queryCarsPrice(arguments);
                break;
            }
            case QueryRoomsPrice: {
                queryRoomsPrice(arguments);
                break;
            }
            case ReserveFlight: {
                reserveFlight(arguments);
                break;
            }
            case ReserveCar: {
                reserveCar(arguments);
                break;
            }
            case ReserveRoom: {
                reserveRoom(arguments);
                break;
            }
            case Bundle: {
                bundle(arguments);
                break;
            }
            case Shutdown: {
                checkArgumentsCount(1, arguments.size());
                m_resourceManager.shutdown();
                break;
            }
            case Quit: {
                checkArgumentsCount(1, arguments.size());

                System.out.println("Quitting client");
                System.exit(0);
                break;
            }
        }
    }

    private void help(Vector<String> arguments) {
        if (arguments.size() == 1) {
            System.out.println(Command.description());
        } else if (arguments.size() == 2) {
            Command l_cmd = Command.fromString((String) arguments.elementAt(1));
            System.out.println(l_cmd.toString());
        } else {
            printCommandException("Improper use of help command. Location \"help\" or \"help,<CommandName>\"");
        }
    }

    public void start(Vector<String> arguments) throws RemoteException {
        checkArgumentsCount(1, arguments.size());

        System.out.println("Starting a new transaction");

        int xid = m_resourceManager.start();

        System.out.println("New transaction id: " + xid);
    }

    private void commit(Vector<String> arguments) throws RemoteException {
        checkArgumentsCount(2, arguments.size());

        System.out.println("Committing transaction [xid=" + arguments.elementAt(1) + "]");

        int id = toInt(arguments.elementAt(1));

        if (m_resourceManager.commit(id)) {
            System.out.println("Transaction committed");
        } else {
            System.out.println("Transaction could not be committed");
        }
    }

    private void abort(Vector<String> arguments) throws RemoteException {
        checkArgumentsCount(2, arguments.size());

        System.out.println("Aborting transaction [xid=" + arguments.elementAt(1) + "]");

        int id = toInt(arguments.elementAt(1));

        if (m_resourceManager.abort(id)) {
            System.out.println("Transaction aborted");
        } else {
            System.out.println("Transaction could not be aborted");
        }
    }

    private void addFlight(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(5, arguments.size());

        System.out.println("Adding a new flight [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Flight Number: " + arguments.elementAt(2));
        System.out.println("-Flight Seats: " + arguments.elementAt(3));
        System.out.println("-Flight Price: " + arguments.elementAt(4));

        int id = toInt(arguments.elementAt(1));
        int flightNum = toInt(arguments.elementAt(2));
        int flightSeats = toInt(arguments.elementAt(3));
        int flightPrice = toInt(arguments.elementAt(4));

        if (m_resourceManager.addFlight(id, flightNum, flightSeats, flightPrice)) {
            System.out.println("Flight added");
        } else {
            System.out.println("Flight could not be added");
        }
    }

    private void addCars(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(5, arguments.size());

        System.out.println("Adding new cars [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Car Location: " + arguments.elementAt(2));
        System.out.println("-Number of Cars: " + arguments.elementAt(3));
        System.out.println("-Car Price: " + arguments.elementAt(4));

        int id = toInt(arguments.elementAt(1));
        String location = arguments.elementAt(2);
        int numCars = toInt(arguments.elementAt(3));
        int price = toInt(arguments.elementAt(4));

        if (m_resourceManager.addCars(id, location, numCars, price)) {
            System.out.println("Cars added");
        } else {
            System.out.println("Cars could not be added");
        }
    }

    private void addRooms(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(5, arguments.size());

        System.out.println("Adding new rooms [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Room Location: " + arguments.elementAt(2));
        System.out.println("-Number of Rooms: " + arguments.elementAt(3));
        System.out.println("-Room Price: " + arguments.elementAt(4));

        int id = toInt(arguments.elementAt(1));
        String location = arguments.elementAt(2);
        int numRooms = toInt(arguments.elementAt(3));
        int price = toInt(arguments.elementAt(4));

        if (m_resourceManager.addRooms(id, location, numRooms, price)) {
            System.out.println("Rooms added");
        } else {
            System.out.println("Rooms could not be added");
        }
    }

    private void addCustomer(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(2, arguments.size());

        System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");

        int id = toInt(arguments.elementAt(1));
        int customer = m_resourceManager.newCustomer(id);

        System.out.println("Add customer ID: " + customer);
    }

    private void addCustomerid(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(3, arguments.size());

        System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Customer ID: " + arguments.elementAt(2));

        int id = toInt(arguments.elementAt(1));
        int customerID = toInt(arguments.elementAt(2));

        if (m_resourceManager.newCustomer(id, customerID)) {
            System.out.println("Add customer ID: " + customerID);
        } else {
            System.out.println("Customer could not be added");
        }
    }

    private void deleteFlight(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(3, arguments.size());

        System.out.println("Deleting a flight [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Flight Number: " + arguments.elementAt(2));

        int id = toInt(arguments.elementAt(1));
        int flightNum = toInt(arguments.elementAt(2));

        if (m_resourceManager.deleteFlight(id, flightNum)) {
            System.out.println("Flight Deleted");
        } else {
            System.out.println("Flight could not be deleted");
        }
    }

    private void deleteCars(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(3, arguments.size());

        System.out.println("Deleting all cars at a particular location [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Car Location: " + arguments.elementAt(2));

        int id = toInt(arguments.elementAt(1));
        String location = arguments.elementAt(2);

        if (m_resourceManager.deleteCars(id, location)) {
            System.out.println("Cars Deleted");
        } else {
            System.out.println("Cars could not be deleted");
        }
    }

    private void deleteRooms(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(3, arguments.size());

        System.out.println("Deleting all rooms at a particular location [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Car Location: " + arguments.elementAt(2));

        int id = toInt(arguments.elementAt(1));
        String location = arguments.elementAt(2);

        if (m_resourceManager.deleteRooms(id, location)) {
            System.out.println("Rooms Deleted");
        } else {
            System.out.println("Rooms could not be deleted");
        }
    }

    private void deleteCustomer(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(3, arguments.size());

        System.out.println("Deleting a customer from the database [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Customer ID: " + arguments.elementAt(2));

        int id = toInt(arguments.elementAt(1));
        int customerID = toInt(arguments.elementAt(2));

        if (m_resourceManager.deleteCustomer(id, customerID)) {
            System.out.println("Customer Deleted");
        } else {
            System.out.println("Customer could not be deleted");
        }
    }

    private void queryFlight(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(3, arguments.size());

        System.out.println("Querying a flight [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Flight Number: " + arguments.elementAt(2));

        int id = toInt(arguments.elementAt(1));
        int flightNum = toInt(arguments.elementAt(2));

        int seats = m_resourceManager.queryFlight(id, flightNum);
        System.out.println("Number of seats available: " + seats);
    }

    private void queryCars(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(3, arguments.size());

        System.out.println("Querying cars location [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Car Location: " + arguments.elementAt(2));

        int id = toInt(arguments.elementAt(1));
        String location = arguments.elementAt(2);

        int numCars = m_resourceManager.queryCars(id, location);
        System.out.println("Number of cars at this location: " + numCars);
    }

    private void queryRooms(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(3, arguments.size());

        System.out.println("Querying rooms location [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Room Location: " + arguments.elementAt(2));

        int id = toInt(arguments.elementAt(1));
        String location = arguments.elementAt(2);

        int numRoom = m_resourceManager.queryRooms(id, location);
        System.out.println("Number of rooms at this location: " + numRoom);
    }

    private void queryCustomer(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(3, arguments.size());

        System.out.println("Querying customer information [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Customer ID: " + arguments.elementAt(2));

        int id = toInt(arguments.elementAt(1));
        int customerID = toInt(arguments.elementAt(2));

        String bill = m_resourceManager.queryCustomerInfo(id, customerID);
        System.out.print(bill);
    }

    private void queryFlightPrice(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(3, arguments.size());

        System.out.println("Querying a flight price [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Flight Number: " + arguments.elementAt(2));

        int id = toInt(arguments.elementAt(1));
        int flightNum = toInt(arguments.elementAt(2));

        int price = m_resourceManager.queryFlightPrice(id, flightNum);
        System.out.println("Price of a seat: " + price);
    }

    private void queryCarsPrice(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(3, arguments.size());

        System.out.println("Querying cars price [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Car Location: " + arguments.elementAt(2));

        int id = toInt(arguments.elementAt(1));
        String location = arguments.elementAt(2);

        int price = m_resourceManager.queryCarsPrice(id, location);
        System.out.println("Price of cars at this location: " + price);
    }

    private void queryRoomsPrice(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(3, arguments.size());

        System.out.println("Querying rooms price [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Room Location: " + arguments.elementAt(2));

        int id = toInt(arguments.elementAt(1));
        String location = arguments.elementAt(2);

        int price = m_resourceManager.queryRoomsPrice(id, location);
        System.out.println("Price of rooms at this location: " + price);
    }

    private void reserveFlight(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(4, arguments.size());

        System.out.println("Reserving seat in a flight [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Customer ID: " + arguments.elementAt(2));
        System.out.println("-Flight Number: " + arguments.elementAt(3));

        int id = toInt(arguments.elementAt(1));
        int customerID = toInt(arguments.elementAt(2));
        int flightNum = toInt(arguments.elementAt(3));

        if (m_resourceManager.reserveFlight(id, customerID, flightNum)) {
            System.out.println("Flight Reserved");
        } else {
            System.out.println("Flight could not be reserved");
        }
    }

    private void reserveCar(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(4, arguments.size());

        System.out.println("Reserving a car at a location [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Customer ID: " + arguments.elementAt(2));
        System.out.println("-Car Location: " + arguments.elementAt(3));

        int id = toInt(arguments.elementAt(1));
        int customerID = toInt(arguments.elementAt(2));
        String location = arguments.elementAt(3);

        if (m_resourceManager.reserveCar(id, customerID, location)) {
            System.out.println("Car Reserved");
        } else {
            System.out.println("Car could not be reserved");
        }
    }

    private void reserveRoom(Vector<String> arguments) throws RemoteException, DeadlockException {
        checkArgumentsCount(4, arguments.size());

        System.out.println("Reserving a room at a location [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Customer ID: " + arguments.elementAt(2));
        System.out.println("-Room Location: " + arguments.elementAt(3));

        int id = toInt(arguments.elementAt(1));
        int customerID = toInt(arguments.elementAt(2));
        String location = arguments.elementAt(3);

        if (m_resourceManager.reserveRoom(id, customerID, location)) {
            System.out.println("Room Reserved");
        } else {
            System.out.println("Room could not be reserved");
        }
    }

    private void bundle(Vector<String> arguments) throws RemoteException, DeadlockException {
        if (arguments.size() < 7) {
            System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0mBundle command expects at least 7 arguments. Location \"help\" or \"help,<CommandName>\"");
            return;
        }

        System.out.println("Reserving an bundle [xid=" + arguments.elementAt(1) + "]");
        System.out.println("-Customer ID: " + arguments.elementAt(2));
        for (int i = 0; i < arguments.size() - 6; ++i) {
            System.out.println("-Flight Number: " + arguments.elementAt(3 + i));
        }
        System.out.println("-Car Location: " + arguments.elementAt(arguments.size() - 2));
        System.out.println("-Room Location: " + arguments.elementAt(arguments.size() - 1));

        int id = toInt(arguments.elementAt(1));
        int customerID = toInt(arguments.elementAt(2));
        Vector<Integer> flightNumbers = new Vector<Integer>();
        for (int i = 0; i < arguments.size() - 6; ++i) {
            flightNumbers.addElement(toInt(arguments.elementAt(3 + i)));
        }
        String location = arguments.elementAt(arguments.size() - 3);
        boolean car = toBoolean(arguments.elementAt(arguments.size() - 2));
        boolean room = toBoolean(arguments.elementAt(arguments.size() - 1));

        if (m_resourceManager.bundle(id, customerID, flightNumbers, location, car, room)) {
            System.out.println("Bundle Reserved");
        } else {
            System.out.println("Bundle could not be reserved");
        }
    }

    private static Vector<String> parse(String command) {
        Vector<String> arguments = new Vector<String>();
        StringTokenizer tokenizer = new StringTokenizer(command, ",");
        String argument = "";
        while (tokenizer.hasMoreTokens()) {
            argument = tokenizer.nextToken();
            argument = argument.trim();
            arguments.add(argument);
        }
        return arguments;
    }

    private static void checkArgumentsCount(Integer expected, Integer actual) throws IllegalArgumentException {
        if (expected != actual) {
            throw new IllegalArgumentException(
                "Invalid number of arguments. Expected " + (expected - 1) +
                ", received " + (actual - 1) +
                ". Location \"help,<CommandName>\" to check usage of this command"
            );
        }
    }

    private static int toInt(String string) throws NumberFormatException {
        return (new Integer(string)).intValue();
    }

    private static boolean toBoolean(String string) {
        return (new Boolean(string)).booleanValue();
    }

    private static void printCommandException(String s) {
        System.out.println(RED.colorString("Command Exception: ")+s);
    }
}
