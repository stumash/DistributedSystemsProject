package group25.Server.Interface;

import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.transaction.InvalidTransactionException;


import group25.Server.LockManager.DeadlockException;

import java.util.*;

/**
 * Simplified version from CSE 593 Univ. of Washington
 * <p>
 * Distributed  System in Java.
 * <p>
 * failure reporting is done using two pieces, exceptions and boolean
 * return values.  Exceptions are used for systemy things. Return
 * values are used for operations that would affect the consistency
 * <p>
 * If there is a boolean return value and you're not sure how it
 * would be used in your implementation, ignore it.  I used boolean
 * return values in the interface generously to allow flexibility in
 * implementation.  But don't forget to return true when the operation
 * has succeeded.
 */

public interface IMiddlewareResourceManager extends Remote {
    /**
     * Add seats to a flight.
     * <p>
     * In general this will be used to create a new
     * flight, but it should be possible to add seats to an existing flight.
     * Adding to an existing flight should overwrite the current price of the
     * available seats.
     *
     * @return Success
     */
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
            throws RemoteException, DeadlockException;

    /**
     * Add car at a location.
     * <p>
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     *
     * @return Success
     */
    public boolean addCars(int id, String location, int numCars, int price)
            throws RemoteException, DeadlockException;

    /**
     * Add room at a location.
     * <p>
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     *
     * @return Success
     */
    public boolean addRooms(int id, String location, int numRooms, int price)
            throws RemoteException, DeadlockException;

    /**
     * Add customer.
     *
     * @return Unique customer identifier
     */
    public int newCustomer(int id)
            throws RemoteException, DeadlockException;

    /**
     * Add customer with id.
     *
     * @return Success
     */
    public boolean newCustomer(int id, int cid)
            throws RemoteException, DeadlockException;

    /**
     * Delete the flight.
     * <p>
     * deleteFlight implies whole deletion of the flight. If there is a
     * reservation on the flight, then the flight cannot be deleted
     *
     * @return Success
     */
    public boolean deleteFlight(int id, int flightNum)
            throws RemoteException, DeadlockException;

    /**
     * Delete all cars at a location.
     * <p>
     * It may not succeed if there are reservations for this location
     *
     * @return Success
     */
    public boolean deleteCars(int id, String location)
            throws RemoteException, DeadlockException;

    /**
     * Delete all rooms at a location.
     * <p>
     * It may not succeed if there are reservations for this location.
     *
     * @return Success
     */
    public boolean deleteRooms(int id, String location)
            throws RemoteException, DeadlockException;

    /**
     * Delete a customer and associated reservations.
     *
     * @return Success
     */
    public boolean deleteCustomer(int id, int customerID)
            throws RemoteException, DeadlockException;

    /**
     * Query the status of a flight.
     *
     * @return Number of empty seats
     */
    public int queryFlight(int id, int flightNumber)
            throws RemoteException, DeadlockException;

    /**
     * Query the status of a car location.
     *
     * @return Number of available cars at this location
     */
    public int queryCars(int id, String location)
            throws RemoteException, DeadlockException;

    /**
     * Query the status of a room location.
     *
     * @return Number of available rooms at this location
     */
    public int queryRooms(int id, String location)
            throws RemoteException, DeadlockException;

    /**
     * Query the customer reservations.
     *
     * @return A formatted bill for the customer
     */
    public String queryCustomerInfo(int id, int customerID)
            throws RemoteException, DeadlockException;

    /**
     * Query the status of a flight.
     *
     * @return Price of a seat in this flight
     */
    public int queryFlightPrice(int id, int flightNumber)
            throws RemoteException, DeadlockException;

    /**
     * Query the status of a car location.
     *
     * @return Price of car
     */
    public int queryCarsPrice(int id, String location)
            throws RemoteException, DeadlockException;

    /**
     * Query the status of a room location.
     *
     * @return Price of a room
     */
    public int queryRoomsPrice(int id, String location)
            throws RemoteException, DeadlockException;

    /**
     * Reserve a seat on this flight.
     *
     * @return Success
     */
    public boolean reserveFlight(int id, int customerID, int flightNumber)
            throws RemoteException, DeadlockException;

    /**
     * Reserve a car at this location.
     *
     * @return Success
     */
    public boolean reserveCar(int id, int customerID, String location)
            throws RemoteException, DeadlockException;

    /**
     * Reserve a room at this location.
     *
     * @return Success
     */
    public boolean reserveRoom(int id, int customerID, String location)
            throws RemoteException, DeadlockException;

    /**
     * Reserve a bundle for the trip.
     *
     * @return Success
     */
    public boolean bundle(int id, int customerID, Vector<Integer> flightNumbers, String location, boolean car, boolean room)
            throws RemoteException, DeadlockException;

    /**
     * Convenience for probing the resource manager.
     *
     * @return Name
     */
    public String getName()
            throws RemoteException, DeadlockException;

    public int start()
        throws RemoteException;

    public boolean commit(int xid) 
            throws RemoteException, InvalidTransactionException;

    public boolean abort(int xid) 
            throws RemoteException, InvalidTransactionException;

    public void shutdown()
        throws RemoteException;

    public void receiveVote(int xid, boolean voteYes, String rmName)
        throws RemoteException;

    public void crashMiddleware(int mode)
        throws RemoteException;

    public void crashResourceManager(String rmName, int mode)
        throws RemoteException;

    public void reconnect(String rmName, String hostname, int port, String objName)
        throws RemoteException;

    public boolean commited(int xid)
        throws RemoteException;
}
