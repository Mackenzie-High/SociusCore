package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.UUID;

/**
 *
 * @author mackenzie
 */
public final class TcpMessengerSocket<I, O>
        implements Pipeline<I, O>,
                   Closeable
{
    public interface MessageInput
    {
        public UUID connectionId ();

        public boolean isBegin ();

        public boolean isHeartbeat ();

        public boolean isData ();

        public boolean isEnd ();

        public int size ();

        public int read (byte[] buffer);

        public int read (byte[] buffer,
                         int offset,
                         int length);

        public DataInputStream openDataInputStream ();

        public void closeConnection ();
    }

    public interface MessageOutput
    {
        public UUID connectionId ();

        public int capacity ();

        public int write (byte[] buffer);

        public int write (byte[] buffer,
                          int offset,
                          int length);

        public DataOutputStream openDataOutputStream ();

        public void closeConnection ();
    }

    @FunctionalInterface
    public interface MessageReader<O>
    {
        public O onRead (MessageInput input);
    }

    @FunctionalInterface
    public interface MessageWriter<I>
    {
        public void onWrite (MessageOutput output,
                             I message);
    }

    @Override
    public Input<I> dataIn ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Output<O> dataOut ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close ()
            throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public TcpMessengerSocket<I, O> bind (final String host,
                                    final int port)
            throws IOException
    {
        final SocketAddress address = new InetSocketAddress(host, port);
        return bind(address);
    }

    public TcpMessengerSocket<I, O> bind (final SocketAddress address)
            throws IOException
    {
        final ServerSocket socket = new ServerSocket();
        socket.bind(address);
        return bind(socket);
    }

    public TcpMessengerSocket<I, O> bind (final ServerSocket socket)
    {
        return this;
    }

    public TcpMessengerSocket<I, O> connect (final String host,
                                       final int port)
            throws IOException
    {
        final SocketAddress address = new InetSocketAddress(host, port);
        return connect(address);
    }

    public TcpMessengerSocket<I, O> connect (final SocketAddress address)
            throws IOException
    {
        final Socket socket = new Socket();
        socket.connect(address);
        return connect(socket);
    }

    public TcpMessengerSocket<I, O> connect (final Socket socket)
    {
        return this;
    }

    private void acceptLoop (final ServerSocket server)
    {
        try
        {
            final Socket socket = server.accept();
            final Connection conn = new Connection(socket);
            conn.start();
        }
        catch (IOException ex)
        {

        }
    }

    private final class Connection
    {
        private final Socket socket;

        public Connection (final Socket sock)
        {
            this.socket = sock;
        }

        public void start ()
        {

        }

        private void readLoop ()
        {

        }

        private void writeLoop ()
        {

        }
    }

    public static final class Builder<I, O>
    {
        public Builder<I, O> withReader (final MessageReader<O> reader)
        {
            return this;
        }

        public Builder<I, O> withWriter (final MessageWriter<I> writer)
        {
            return this;
        }

        public Builder<I, O> withHeartbeatPeriod (final Duration period)
        {
            return this;
        }

        public Builder<I, O> withHeartbeatTimeout (final Duration timeout)
        {
            return this;
        }

        public Builder<I, O> withMaximumConnections (final int count)
        {
            return this;
        }
    }

}
