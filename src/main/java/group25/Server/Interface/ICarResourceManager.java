package group25.Server.Interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

import group25.Server.TCP.IProxiable;

import java.util.*;

public interface ICarResourceManager extends Remote, IProxiable, IAbstractRMHashMapManager {
    /**
     * Reserve a car at this location.
     *
     * @return Success
     */
    public boolean reserveCar(int xid, int customerID, String location)
            throws RemoteException;

    /**
     * Add car at a location.
     * <p>
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     *
     * @return Success
     */
    public boolean addCars(int xid, String location, int numCars, int price)
            throws RemoteException;

    /**
     * Delete all cars at a location.
     * <p>
     * It may not succeed if there are reservations for this location
     *
     * @return Success
     */
    public boolean deleteCars(int xid, String location)
            throws RemoteException;

    /**
     * Query the status of a car location.
     *
     * @return Number of available cars at this location
     */
    public int queryCars(int xid, String location)
            throws RemoteException;


    /**
     * Query the status of a car location.
     *
     * @return Price of car
     */
    public int queryCarsPrice(int xid, String location)
            throws RemoteException;

    /**
     * Convenience for probing the resource manager.
     *
     * @return Name
     */
    public String getName()
            throws RemoteException;
            
    public void shutdown()
            throws RemoteException;        
}
