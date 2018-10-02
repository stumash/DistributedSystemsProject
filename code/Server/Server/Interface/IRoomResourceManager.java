package Server.Interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;

public interface IRoomResourceManager extends Remote, ICustomerReservationManager, IProxiable
{
  /**
   * Add room at a location.
   *
   * This should look a lot like addFlight, only keyed on a string location
   * instead of a flight number.
   *
   * @return Success
   */
  public boolean addRooms(int id, String location, int numRooms, int price)
throws RemoteException;
/**
 * Delete all rooms at a location.
 *
 * It may not succeed if there are reservations for this location.
 *
 * @return Success
 */
public boolean deleteRooms(int id, String location)
throws RemoteException;
/**
 * Query the status of a room location.
 *
 * @return Number of available rooms at this location
 */
public int queryRooms(int id, String location)
throws RemoteException;
/**
 * Query the status of a room location.
 *
 * @return Price of a room
 */
public int queryRoomsPrice(int id, String location)
throws RemoteException;
/**
 * Reserve a room at this location.
 *
 * @return Success
 */
public boolean reserveRoom(int id, int customerID, String location)
throws RemoteException;
/**
 * Convenience for probing the resource manager.
 *
 * @return Name
 */
public String getName()
    throws RemoteException;
}
