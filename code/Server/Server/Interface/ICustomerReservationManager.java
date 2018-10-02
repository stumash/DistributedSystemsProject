package Server.Interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;

import Server.Common.Customer;

public interface ICustomerReservationManager
{
  private boolean reserveItem(int xid, int customerID, String key, String location) throws RemoteException;
}
