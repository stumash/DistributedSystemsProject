package group25.Server.Common;

import group25.Server.Interface.*;


import java.util.*;

import javax.transaction.InvalidTransactionException;

import java.rmi.RemoteException;
import group25.Server.LockManager.*;
import group25.Utils.CrashMode;

public abstract class MiddlewareResourceManager implements IMiddlewareResourceManager {
    protected ICarResourceManager carRM;
    protected IFlightResourceManager flightRM;
    protected IRoomResourceManager roomRM;
    protected ICustomerResourceManager customerRM;
    protected TransactionManager transactionManager;
    private String m_name = "";


    public MiddlewareResourceManager(String p_name) {
        m_name = p_name;
    }

    public int start() throws RemoteException {
        return transactionManager.start();
    }

    public boolean commit(int xid) throws RemoteException, InvalidTransactionException {
        return transactionManager.commit(xid);
    }

    public boolean abort(int xid) throws RemoteException, InvalidTransactionException {
        return transactionManager.abort(xid, false);
    }

    // Create a new flight, or add seats to existing flight
    // NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
    public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException, DeadlockException {
        return transactionManager.addFlight(xid, flightNum, flightSeats, flightPrice);
    }

    // Deletes flight
    public boolean deleteFlight(int xid, int flightNum) throws RemoteException, DeadlockException {
        return transactionManager.deleteFlight(xid, flightNum);
    }

    // Returns the number of empty seats in this flight
    public int queryFlight(int xid, int flightNum) throws RemoteException, DeadlockException {
        return transactionManager.queryFlight(xid, flightNum);
    }

    // Returns price of a seat in this flight
    public int queryFlightPrice(int xid, int flightNum) throws RemoteException, DeadlockException {
        return transactionManager.queryFlightPrice(xid, flightNum);
    }

    // Adds flight reservation to this customer
    public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException, DeadlockException {
        return transactionManager.reserveFlight(xid, customerID, flightNum);
    }

    // Adds car reservation to this customer
    public boolean reserveCar(int xid, int customerID, String location) throws RemoteException, DeadlockException {
        return transactionManager.reserveCar(xid, customerID, location);
    }

    // Create a new car location or add cars to an existing location
    // NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int xid, String location, int count, int price) throws RemoteException, DeadlockException {
        return transactionManager.addCars(xid, location, count, price);
    }

    // Delete cars at a location
    public boolean deleteCars(int xid, String location) throws RemoteException, DeadlockException {
        return transactionManager.deleteCars(xid, location);
    }

    // Returns the number of cars available at a location
    public int queryCars(int xid, String location) throws RemoteException, DeadlockException {
        return transactionManager.queryCars(xid, location);
    }

    // Returns price of cars at this location
    public int queryCarsPrice(int xid, String location) throws RemoteException, DeadlockException {
        return transactionManager.queryCarsPrice(xid, location);
    }

    // Adds room reservation to this customer
    public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException, DeadlockException {
        return transactionManager.reserveRoom(xid, customerID, location);
    }

    // Create a new room location or add rooms to an existing location
    // NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int xid, String location, int count, int price) throws RemoteException, DeadlockException {
        return transactionManager.addRooms(xid, location, count, price);
    }

    // Returns the amount of rooms available at a location
    public int queryRooms(int xid, String location) throws RemoteException, DeadlockException {
        return transactionManager.queryRooms(xid, location);
    }

    // Returns room price at this location
    public int queryRoomsPrice(int xid, String location) throws RemoteException, DeadlockException {
        return transactionManager.queryRoomsPrice(xid, location);
    }

    // Delete rooms at a location
    public boolean deleteRooms(int xid, String location) throws RemoteException, DeadlockException {
        return transactionManager.deleteRooms(xid, location);
    }

    public int newCustomer(int id) throws RemoteException, DeadlockException {
        return transactionManager.newCustomer(id);
    }

    public boolean newCustomer(int id, int cid) throws RemoteException, DeadlockException {
        return transactionManager.newCustomer(id, cid);
    }

    public boolean deleteCustomer(int id, int customerID) throws RemoteException, DeadlockException {
        return transactionManager.deleteCustomer(id, customerID);
    }

    public String queryCustomerInfo(int id, int customerID) throws RemoteException, DeadlockException {
        return transactionManager.queryCustomerInfo(id, customerID);
    }

    /**
     * Reserve a bundle for the trip.
     *
     * @return Success
     */
    public boolean bundle(int xid, int customerID, Vector<Integer> flightNumbers, String location, boolean car, boolean room)
            throws RemoteException, DeadlockException {

        // TODO: reserveXXX() methods SHOULD ABORT ON FAILURE!!!

        // first check if all the data exists
        for (int flightNumber: flightNumbers) {
            int numFlights = transactionManager.queryFlight(xid, flightNumber);
            if (!(numFlights > 0)) {
                return false;
            }
        }

        if (car) {
            int numCars = transactionManager.queryCars(xid, location);
            if (!(numCars > 0)) {
                return false;
            }
        }
        if (room) {
            int numRooms = transactionManager.queryRooms(xid, location);
            if (!(numRooms > 0)) {
                return false;
            }
        }
        // if all data exists, try to reserve
        for (int flightNumber : flightNumbers) {
            boolean flightReserved = transactionManager.reserveFlight(xid, customerID, flightNumber);
            if (!flightReserved) {
                transactionManager.abort(xid, false);
                return false;
            }
        }

        if (car) {
            boolean carReserved = transactionManager.reserveCar(xid, customerID, location);
            if (!carReserved) {
                transactionManager.abort(xid, false);
                return false;
            }
        }

        if (room) {
            boolean roomReserved = transactionManager.reserveRoom(xid, customerID, location);
            if (!roomReserved) {
                transactionManager.abort(xid, false);
                return false;
            }
        }

        return true;
    }

    public void receiveVote(int xid, boolean voteYes, String rmName) throws InvalidTransactionException, RemoteException {
        transactionManager.receiveVote(xid, voteYes, rmName);
    }

    public String getName() throws RemoteException, DeadlockException {
        return m_name;
    }

    public void crashMiddleware(int mode) throws RemoteException {
        transactionManager.crashMiddleware(mode);
    }

    public void crashResourceManager(String rmName, int mode) throws RemoteException {
        transactionManager.crashResourceManager(rmName, mode);
    }

    @Override
    public void shutdown() {
        transactionManager.shutdownAllResourceManagers();
        System.out.println("Shutting down middleware server");
        System.exit(0);
    }
}
