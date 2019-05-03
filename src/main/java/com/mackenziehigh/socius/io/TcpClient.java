package com.mackenziehigh.socius.io;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.DataPipeline;
import com.mackenziehigh.socius.flow.Processor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A minimal peer-to-peer TCP client socket.
 */
public final class TcpClient<T>
        implements DataPipeline<T, T>
{
    private final Thread readerThread = new Thread(this::readLoop);

    private final Thread writerThread = new Thread(this::writeLoop);

    private final BlockingQueue<T> pending;

    private final Processor<T> dataIn;

    private final Processor<T> dataOut;

    private volatile TcpServer.Reader<T> reader = null;

    private volatile TcpServer.Writer<T> writer = null;

    private final AtomicBoolean stop = new AtomicBoolean();

    private final Stage serverStage;

    private final SocketFactory socketFactory;

    private final int readBufferSize;

    private final int writeBufferSize;

    private volatile TcpServer.SocketInputStream socketInputStream;

    private volatile TcpServer.SocketOutputStream socketOutputStream;

    private TcpClient (final Builder<T> builder)
    {
        this.serverStage = builder.stage;
        this.socketFactory = Objects.requireNonNull(builder.socketFactory, "socketFactory");
        this.readBufferSize = builder.readBufferSize;
        this.writeBufferSize = builder.writeBufferSize;
        this.pending = new ArrayBlockingQueue<>(1024);
        this.dataIn = Processor.fromConsumerScript(serverStage, msg -> pending.add(msg));
        this.dataOut = Processor.fromIdentityScript(serverStage);
    }

    @Override
    public Input<T> dataIn ()
    {
        return dataIn.dataIn();
    }

    @Override
    public Output<T> dataOut ()
    {
        return dataOut.dataOut();
    }

    public TcpClient<T> start (final TcpServer.Reader<T> readerScript,
                               final TcpServer.Writer<T> writerScript)
            throws IOException
    {

        final Socket socket = socketFactory.newSocket();
        final InputStream sin = socket.getInputStream();
        final OutputStream sout = socket.getOutputStream();
        final InputStream bin = readBufferSize > 0 ? new BufferedInputStream(sin, readBufferSize) : sin;
        final OutputStream bout = writeBufferSize > 0 ? new BufferedOutputStream(sout, writeBufferSize) : sout;
        this.socketInputStream = new TcpServer.SocketInputStream(bin);
        this.socketOutputStream = new TcpServer.SocketOutputStream(bout);

        reader = Objects.requireNonNull(readerScript, "readerScript");
        writer = Objects.requireNonNull(writerScript, "writerScript");
        readerThread.start();
        writerThread.start();
        return this;
    }

    private void readLoop ()
    {
        try (TcpServer.SocketInputStream in = socketInputStream)
        {
            while (stop.get() == false)
            {
                final T message = reader.read(in);
                if (message != null)
                {
                    dataOut.accept(message);
                }
            }
        }
        catch (Throwable ex)
        {
            killSocket();
        }
    }

    private void writeLoop ()
    {
        try (TcpServer.SocketOutputStream out = socketOutputStream)
        {
            while (stop.get() == false)
            {
                final T message = pending.poll(1, TimeUnit.SECONDS);
                if (message != null)
                {
                    writer.write(out, message);
                }
            }
        }
        catch (Throwable ex)
        {
            killSocket();
        }
    }

    private void killSocket ()
    {

    }

    public static final class Builder<T>
    {
        private final Stage stage;

        private SocketFactory socketFactory;

        private int readBufferSize = 8192;

        private int writeBufferSize = 8192;

        private Builder (final Cascade.Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        public Builder<T> withSocketFactory (final SocketFactory factory)
        {
            this.socketFactory = Objects.requireNonNull(factory, "factory");
            return this;
        }

        public Builder<T> withSocket (final SocketAddress address)
        {
            Objects.requireNonNull(address, "address");
            this.socketFactory = () ->
            {
                final Socket server = new Socket();
                server.connect(address);
                return server;
            };
            return this;
        }

        public Builder<T> withWriteBufferSize (final int capacity)
        {
            this.writeBufferSize = capacity;
            return this;
        }

        public Builder<T> withReadBufferSize (final int capacity)
        {
            this.readBufferSize = capacity;
            return this;
        }

        public TcpClient<T> build ()
        {
            return new TcpClient<>(this);
        }
    }
}
