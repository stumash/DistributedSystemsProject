package Server.Interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;

public interface ICarResourceManager extends Remote
{
  /**
   * Reserve a car at this location.
   *
   * @return Success
   */
  public boolean reserveCar(int id, int customerID, String location)
throws RemoteException;

  /**
   * Add car at a location.
   *
   * This should look a lot like addFlight, only keyed on a string location
   * instead of a flight number.
   *
   * @return Success
   */
  public boolean addCars(int id, String location, int numCars, int price)
throws RemoteException;

/**
 * Delete all cars at a location.
 *
 * It may not succeed if there are reservations for this location
 *
 * @return Success
 */
public boolean deleteCars(int id, String location)
throws RemoteException;

/**
 * Query the status of a car location.
 *
 * @return Number of available cars at this location
 */
public int queryCars(int id, String location)
throws RemoteException;



/**
 * Query the status of a car location.
 *
 * @return Price of car
 */
public int queryCarsPrice(int id, String location)
throws RemoteException;

/**
 * Convenience for probing the resource manager.
 *
 * @return Name
 */
public String getName()
    throws RemoteException;
}
