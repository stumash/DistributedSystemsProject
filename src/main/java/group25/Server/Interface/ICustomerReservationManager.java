package group25.Server.Interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;

import group25.Server.Common.Customer;

public interface ICustomerReservationManager {
    public boolean reserveItem(int xid, int customerID, String key, String location) throws RemoteException;
}
