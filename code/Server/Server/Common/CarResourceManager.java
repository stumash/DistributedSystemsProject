package Server.Common;

import Server.Interface.*;

import java.util.*;
import java.rmi.RemoteException;
import java.io.*;

public class CarResourceManager extends AbstractRMHashMapManager implements ICarResourceManager
{
  // Create a new car location or add cars to an existing location
  // NOTE: if price <= 0 and the location already exists, it maintains its current price
  public boolean addCars(int xid, String location, int count, int price) throws RemoteException
  {
    Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
    Car curObj = (Car)readData(xid, Car.getKey(location));
    if (curObj == null)
    {
      // Car location doesn't exist yet, add it
      Car newObj = new Car(location, count, price);
      writeData(xid, newObj.getKey(), newObj);
      Trace.info("RM::addCars(" + xid + ") created new location " + location + ", count=" + count + ", price=$" + price);
    }
    else
    {
      // Add count to existing car location and update price if greater than zero
      curObj.setCount(curObj.getCount() + count);
      if (price > 0)
      {
        curObj.setPrice(price);
      }
      writeData(xid, curObj.getKey(), curObj);
      Trace.info("RM::addCars(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
    }
    return true;
  }

  // Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location) throws RemoteException
	{
		return reserveItem(xid, customerID, Car.getKey(location), location);
	}

  // Delete cars at a location
  public boolean deleteCars(int xid, String location) throws RemoteException
  {
    return deleteItem(xid, Car.getKey(location));
  }
  // Returns the number of cars available at a location
	public int queryCars(int xid, String location) throws RemoteException
	{
		return queryNum(xid, Car.getKey(location));
	}

  // Returns price of cars at this location
  public int queryCarsPrice(int xid, String location) throws RemoteException
  {
    return queryPrice(xid, Car.getKey(location));
  }
}
