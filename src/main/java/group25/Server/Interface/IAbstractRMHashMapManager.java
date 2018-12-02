package group25.Server.Interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

import group25.Server.Common.RMItem;

public interface IAbstractRMHashMapManager extends Remote {
    public RMItem readData(int xid, String key) throws RemoteException;

    public void writeData(int xid, String key, RMItem value) throws RemoteException;
    
    public void removeData(int xid, String key) throws RemoteException;

    public void vote(int xid) throws RemoteException;

    public boolean doCommit(int xid) throws RemoteException;

    public boolean abort(int xid) throws RemoteException;

    public String getName() throws RemoteException;
}