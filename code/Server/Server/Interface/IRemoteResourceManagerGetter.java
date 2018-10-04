package Server.Interface;

import java.rmi.Remote;

/**
 * RRM = RemoteResourceManager
 */
public interface IRemoteResourceManagerGetter {
    public Remote getRemoteResourceManager(String hostname, int port, String name);
}
