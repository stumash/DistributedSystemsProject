package Server.Interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;

import Server.Common.Customer;

public interface ICustomerReservationManager
{
  public boolean reserveItem(int xid, int customerID, String key, String location) throws RemoteException;

  public Customer getCustomer(int xid, int customerID) throws RemoteException;
}
