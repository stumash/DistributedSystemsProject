package Server.TCP;

import java.io.Serializable;

public class Message implements Serializable
{
  String proxyObjectBoundName;
  Boolean requestSuccessful;
  Object requestedValue;
}
