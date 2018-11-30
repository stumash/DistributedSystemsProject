package group25.Server.Interface;

import group25.Server.Common.ReservedItem;
import group25.Server.Common.RMHashMap;


public interface ICustomer {
    public boolean setID(int id);

    public int getID();

    public boolean reserve(String key, String location, int price);

    public ReservedItem getReservedItem(String key);

    public String getBill();

    public String getKey();

    public RMHashMap getReservations();

}
