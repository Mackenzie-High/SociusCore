package com.mackenziehigh.socius.io.tcp;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.io.SocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

/**
 *
 */
public final class TcpClient
{
    /**
     * Create a new TCP client socket that can connect to a remote server socket.
     *
     * @param <T> is the type of the messages flowing through the socket.
     * @param stage will be used to create private actors.
     * @param socketFactory will be used to create the socket itself.
     * @return the new client.
     * @throws IOException if something goes wrong.
     */
    public static <T> TcpSocket<T> newTcpClient (final Stage stage,
                                                 final SocketFactory socketFactory)
            throws IOException
    {
        return new TcpSocket<>(stage, socketFactory.newSocket());
    }

    /**
     * Create a new TCP client socket that can connect to a remote server socket.
     *
     * @param <T> is the type of the messages flowing through the socket.
     * @param stage will be used to create private actors.
     * @param address identifies where the socket shall connect to.
     * @return the new client.
     * @throws IOException if something goes wrong.
     */
    public static <T> TcpSocket<T> newTcpClient (final Stage stage,
                                                 final SocketAddress address)
            throws IOException
    {
        final Socket socket = new Socket();
        socket.connect(address);
        return new TcpSocket<>(stage, socket);
    }
}
