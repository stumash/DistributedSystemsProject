package Server.TCP;

import java.io.Serializable;

public abstract class AbstractProxyObject implements Serializable
{
  private String hostname;
  private int port;
  private String boundName;

  public AbstractProxyObject(String hostname, int port, String boundName)
  {
    this.hostname = hostname;
    this.port = port;
    this.boundName = boundName;
  }

  public String getBoundName()
  {
    return this.boundName;
  }

  private Message sendAndReceiveMessage(Message messageToSend)
  {
    messageToSend.proxyObjectBoundName = boundName;

    Socket socket = new Socket(hostname, port);

    ObjectOutputStream objectOutput =
      new ObjectInputStream(socket.getOutputStream());
    ObjectInputStream objectInput =
      new ObjectInputStream(socket.getInputStream());

    objectOutput.writeObject(messageToSend);
    return objectInput.readObject();
  }
}
