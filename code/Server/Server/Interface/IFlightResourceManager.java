package Server.Interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

import Server.TCP.IProxiable;

import java.util.*;

public interface IFlightResourceManager extends Remote, IProxiable
{
  /**
   * Add seats to a flight.
   *
   * In general this will be used to create a new
   * flight, but it should be possible to add seats to an existing flight.
   * Adding to an existing flight should overwrite the current price of the
   * available seats.
   *
   * @return Success
   */
  public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
throws RemoteException;
/**
 * Delete the flight.
 *
 * deleteFlight implies whole deletion of the flight. If there is a
 * reservation on the flight, then the flight cannot be deleted
 *
 * @return Success
 */
public boolean deleteFlight(int id, int flightNum)
throws RemoteException;
/**
 * Query the status of a flight.
 *
 * @return Number of empty seats
 */
public int queryFlight(int id, int flightNumber)
throws RemoteException;

/**
 * Query the status of a flight.
 *
 * @return Price of a seat in this flight
 */
public int queryFlightPrice(int id, int flightNumber)
throws RemoteException;
/**
 * Reserve a seat on this flight.
 *
 * @return Success
 */
public boolean reserveFlight(int id, int customerID, int flightNumber)
throws RemoteException;

/**
 * Convenience for probing the resource manager.
 *
 * @return Name
 */
public String getName()
    throws RemoteException;
}
