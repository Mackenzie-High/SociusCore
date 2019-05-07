package com.mackenziehigh.socius.io.tcp;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.io.ServerSocketFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Objects;

public final class TcpServer<T>
{
    private final Stage stage;

    private final Processor<TcpSocket<T>> socketsOut;

    private final ServerSocketFactory socketFactory;

    private final Thread acceptThread = new Thread(this::acceptLoop);

    private TcpServer (final Stage stage,
                       final ServerSocketFactory socketFactory)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.socketFactory = Objects.requireNonNull(socketFactory, "socketFactory");
        this.socketsOut = Processor.fromIdentityScript(stage);
    }

    private void acceptLoop ()
    {
        try
        {
            final ServerSocket server = socketFactory.newServerSocket();

            while (true)
            {
                final Socket socket = server.accept();
                final TcpSocket<T> wrapper = new TcpSocket(stage, socket);
                socketsOut.accept(wrapper);
            }
        }
        catch (Throwable ex)
        {
            stop();
        }
    }

    public TcpServer<T> start ()
    {
        acceptThread.start();
        return this;
    }

    public TcpServer<T> stop ()
    {
        return this;
    }

    public Output<TcpSocket<T>> socketsOut ()
    {
        return socketsOut.dataOut();
    }

    public static <T> TcpServer<T> newTcpServer (final Stage stage,
                                                 final ServerSocketFactory socketFactory)
    {
        return new TcpServer<>(stage, socketFactory);
    }

    public static <T> TcpServer<T> newTcpServer (final Stage stage,
                                                 final SocketAddress address)
    {
        final ServerSocketFactory factory = () ->
        {
            final ServerSocket socket = new ServerSocket();
            socket.bind(address);
            return socket;
        };

        return new TcpServer<>(stage, factory);
    }

}
