package Server.Interface;

import Server.Common.ReservedItem;
import Server.Common.RMHashMap;
import Server.TCP.IProxiable;

public interface ICustomer extends IProxiable {
    public boolean setID(int id);

    public int getID();

    public boolean reserve(String key, String location, int price);

    public ReservedItem getReservedItem(String key);

    public String getBill();

    public String getKey();

    public RMHashMap getReservations();

}
