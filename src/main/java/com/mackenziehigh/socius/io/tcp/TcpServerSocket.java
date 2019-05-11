package com.mackenziehigh.socius.io.tcp;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.Processor;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TcpServerSocket
{
    private final Processor<Socket> socketsOut;

    private final Processor<Throwable> rethrower;

    private final ServerSocket serverSocket;

    private final Thread acceptThread = new Thread(this::acceptLoop);

    private final AtomicBoolean stop = new AtomicBoolean();

    private TcpServerSocket (final Stage stage,
                             final ServerSocket serverSocket)
    {
        this.serverSocket = Objects.requireNonNull(serverSocket, "serverSocket");
        this.socketsOut = Processor.fromIdentityScript(stage);
        this.rethrower = Processor.fromConsumerScript(stage, ex -> rethrow(ex));
        this.acceptThread.setName("TcpServerSocket.Accept");
        this.acceptThread.setDaemon(true);
    }

    private void rethrow (Throwable ex)
            throws IOException
    {
        throw new IOException(ex);
    }

    private void acceptLoop ()
    {
        try
        {
            while (true)
            {
                final Socket socket = serverSocket.accept();
                socketsOut.accept(socket);
            }
        }
        catch (Throwable ex)
        {
            rethrower.accept(ex);
        }
        finally
        {
            stop();
        }
    }

    public TcpServerSocket start ()
    {
        acceptThread.start();
        return this;
    }

    public TcpServerSocket stop ()
    {
        if (stop.compareAndSet(false, true))
        {
            try
            {
                serverSocket.close();
            }
            catch (Throwable ex)
            {
                // Pass.
            }
        }

        return this;
    }

    public Output<Socket> socketsOut ()
    {
        return socketsOut.dataOut();
    }

    public static TcpServerSocket newTcpServer (final Stage stage,
                                                final ServerSocket socket)
    {
        return new TcpServerSocket(stage, socket);
    }

}
