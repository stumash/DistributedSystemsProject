package Server.Common;

import Server.Interface.*;

import java.util.*;
import java.rmi.RemoteException;
import java.io.*;

public abstract class MiddlewareResourceManager implements IResourceManager
{
  private CarResourceManager carRM;
  private FlightResourceManager flightRM;
  private RoomResourceManager roomRM;

  // Create a new flight, or add seats to existing flight
  // NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
  public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException
  {
    flightRM.addFlight(xid, flightNum, flightSeats, flightPrice);
  };

  // Deletes flight
  public boolean deleteFlight(int xid, int flightNum) throws RemoteException
  {
    flightRM.deleteFlight(xid, flightNum);
  }

  // Returns the number of empty seats in this flight
	public int queryFlight(int xid, int flightNum) throws RemoteException
	{
		flightRM.queryFlight(xid, flightNum);
	};

  // Returns price of a seat in this flight
	public int queryFlightPrice(int xid, int flightNum) throws RemoteException
	{
		return flightRM.queryFlightPrice(xid, flightNum);
	}
  // Adds flight reservation to this customer
  public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException
  {
    return flightRM.reserveFlight(xid, customerID, flightNum);
  };

  // Adds car reservation to this customer
  public boolean reserveCar(int xid, int customerID, String location) throws RemoteException
  {
    return carRM.reserveCar(xid, customerID, location);
  };

  // Create a new car location or add cars to an existing location
  // NOTE: if price <= 0 and the location already exists, it maintains its current price
  public boolean addCars(int xid, String location, int count, int price) throws RemoteException
  {
    return carRM.addCars(xid, location, count, price);
  }

  // Delete cars at a location
  public boolean deleteCars(int xid, String location) throws RemoteException
  {
    return carRM.deleteItem(xid, location);
  }

  // Returns the number of cars available at a location
  public int queryCars(int xid, String location) throws RemoteException
  {
    return carRM.queryCars(xid, location);
  }

  // Returns price of cars at this location
  public int queryCarsPrice(int xid, String location) throws RemoteException
  {
    return carRM.queryPrice(xid, location);
  }

  // Adds room reservation to this customer
  public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException
  {
    return roomRM.reserveRoom(xid, customerID, location);
  }

  // Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int xid, String location, int count, int price) throws RemoteException
	{
		return roomRM.addRooms(xid, location, count, price);
	}

  // Returns the amount of rooms available at a location
  public int queryRooms(int xid, String location) throws RemoteException
  {
    return roomRM.queryRooms(xid, location);
  }

  // Returns room price at this location
  public int queryRoomsPrice(int xid, String location) throws RemoteException
  {
    return roomRM.queryRoomsPrice(xid, location);
  }

  // Delete rooms at a location
  public boolean deleteRooms(int xid, String location) throws RemoteException
  {
    return roomRM.deleteRooms(xid, location);
  }

}
