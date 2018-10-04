package Server.TCP;

import java.net.Socket;
import java.net.UnknownHostException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

public abstract class AbstractProxyObject implements Serializable
{
    protected String hostname;
    protected int port;
    protected String boundName;

    public AbstractProxyObject(String hostname, int port, String boundName)
    {
        this.hostname = hostname;
        this.port = port;
        this.boundName = boundName;
    }

    public String getBoundName() {
        return this.boundName;
    }

    protected Message sendAndReceiveMessage(Message messageToSend)
            throws UnknownHostException, IOException, ClassNotFoundException
    {
        messageToSend.proxyObjectBoundName = boundName;
        System.out.println("Socket ateempting to create on hostname and port: " + hostname + ", " + port);
        Socket socket = new Socket(hostname, port);
        ObjectOutputStream objectOutput =
                new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream objectInput =
                new ObjectInputStream(socket.getInputStream());

        objectOutput.writeObject(messageToSend);
        return (Message) objectInput.readObject();
    }
}
