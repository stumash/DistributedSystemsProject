package Server.TCP;

import java.io.Serializable;

public class Message implements Serializable {
    public String proxyObjectBoundName;
    public Boolean requestSuccessful;
    public Object requestedValue;
}
