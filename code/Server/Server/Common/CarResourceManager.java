package Server.Common;

import Server.Interface.*;

import java.util.*;
import java.rmi.RemoteException;
import java.io.*;

public class CarResourceManager extends AbstractRMHashMapManager implements ICarResourceManager, ICustomerReservationManager
{
  // Create a new car location or add cars to an existing location
  // NOTE: if price <= 0 and the location already exists, it maintains its current price
  private String m_name = "";
  // private carRM = new CarResourceManager();

  public CarResourceManager(String p_name) {
    m_name = p_name;
  };

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

  public boolean reserveItem(int xid, int customerID, String key, String location) {
    Trace.info("RM::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called" );
		// Read customer object if it exists (and read lock it)
		Customer customer = getCustomer(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
			return false;
		}

		// Check if the item is available
		ReservableItem item = (ReservableItem)readData(xid, key);
		if (item == null)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
			return false;
		}
		else if (item.getCount() == 0)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
			return false;
		}
		else
		{
			customer.reserve(key, location, item.getPrice());
			writeData(xid, customer.getKey(), customer);

			// Decrease the number of available items in the storage
			item.setCount(item.getCount() - 1);
			item.setReserved(item.getReserved() + 1);
			writeData(xid, item.getKey(), item);

			Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
			return true;
		}
  }

  /**
   TODO public Customer getCustomer(int xid, int cid) {
   customerRM.getCustomer(xid, cid)
 }
   */
  public String getName() throws RemoteException
	{
		return m_name;
	}
}
