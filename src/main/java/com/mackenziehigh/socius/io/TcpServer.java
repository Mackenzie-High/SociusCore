package com.mackenziehigh.socius.io;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.DataPipeline;
import com.mackenziehigh.socius.flow.Processor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TcpServer<T>
{
    private final Stage serverStage;

    private final Processor<TcpSocket<T>> socketsOut;

    private final ServerSocketFactory socketFactory;

    private final Thread acceptThread = new Thread(this::acceptLoop);

    private final int readBufferSize;

    private final int writeBufferSize;

    private TcpServer (final Builder<T> builder)
    {
        this.serverStage = builder.stage;
        this.socketsOut = Processor.fromIdentityScript(serverStage);
        this.socketFactory = Objects.requireNonNull(builder.socketFactory, "socketFactory");
        this.readBufferSize = builder.readBufferSize;
        this.writeBufferSize = builder.writeBufferSize;
    }

    private void acceptLoop ()
    {
        try
        {
            final ServerSocket server = socketFactory.newServerSocket();

            while (true)
            {
                final Socket socket = server.accept();
                final TcpSocket<T> wrapper = new TcpSocket(this, socket);
                socketsOut.accept(wrapper);
            }
        }
        catch (Throwable ex)
        {

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

    public static <T> Builder<T> newTcpServer (final Stage stage)
    {
        return new Builder<>(stage);
    }

    public static final class SocketInputStream
            extends DataInputStream
    {
        SocketInputStream (final InputStream in)
        {
            super(in);
        }

        @Override
        public void close ()
                throws IOException
        {
            super.close();
        }

    }

    public static final class SocketOutputStream
            extends DataOutputStream
    {
        public SocketOutputStream (final OutputStream out)
        {
            super(out);
        }

        @Override
        public void close ()
                throws IOException
        {
            super.close();
        }
    }

    @FunctionalInterface
    public interface Reader<T>
    {
        public T read (SocketInputStream in)
                throws IOException;
    }

    @FunctionalInterface
    public interface Writer<T>
    {
        public void write (SocketOutputStream out,
                           T message)
                throws IOException;
    }

    public static final class TcpSocket<T>
            implements DataPipeline<T, T>
    {
        private final Thread readerThread = new Thread(this::readLoop);

        private final Thread writerThread = new Thread(this::writeLoop);

        private final BlockingQueue<T> pending;

        private final Processor<T> dataIn;

        private final Processor<T> dataOut;

        private final SocketInputStream socketInputStream;

        private final SocketOutputStream socketOutputStream;

        private volatile Reader<T> reader = null;

        private volatile Writer<T> writer = null;

        private final AtomicBoolean stop = new AtomicBoolean();

        private TcpSocket (final TcpServer<T> server,
                           final Socket socket)
                throws IOException
        {
            final InputStream sin = socket.getInputStream();
            final OutputStream sout = socket.getOutputStream();
            final InputStream bin = server.readBufferSize > 0 ? new BufferedInputStream(sin, server.readBufferSize) : sin;
            final OutputStream bout = server.writeBufferSize > 0 ? new BufferedOutputStream(sout, server.writeBufferSize) : sout;
            this.socketInputStream = new SocketInputStream(bin);
            this.socketOutputStream = new SocketOutputStream(bout);
            this.pending = new ArrayBlockingQueue<>(1024);
            this.dataIn = Processor.fromConsumerScript(server.serverStage, msg -> pending.add(msg));
            this.dataOut = Processor.fromIdentityScript(server.serverStage);
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

        public TcpSocket<T> start (final Reader<T> readerScript,
                                   final Writer<T> writerScript)
        {
            reader = Objects.requireNonNull(readerScript, "readerScript");
            writer = Objects.requireNonNull(writerScript, "writerScript");
            readerThread.start();
            writerThread.start();
            return this;
        }

        private void readLoop ()
        {
            try (SocketInputStream in = socketInputStream)
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
            try (SocketOutputStream out = socketOutputStream)
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
    }

    public static final class Builder<T>
    {
        private final Stage stage;

        private ServerSocketFactory socketFactory;

        private int readBufferSize = 8192;

        private int writeBufferSize = 8192;

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        public Builder<T> withSocketFactory (final ServerSocketFactory factory)
        {
            this.socketFactory = Objects.requireNonNull(factory, "factory");
            return this;
        }

        public Builder<T> withSocket (final SocketAddress address)
        {
            Objects.requireNonNull(address, "address");
            this.socketFactory = () ->
            {
                final ServerSocket server = new ServerSocket();
                server.bind(address);
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

        public TcpServer<T> build ()
        {
            return new TcpServer<>(this);
        }
    }

    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();
        final TcpServer<String> server = TcpServer.<String>newTcpServer(stage)
                .withSocketFactory(TcpServer::newSocket)
                .build();

        final Processor<TcpSocket<String>> procConn = Processor.fromConsumerScript(stage, msg -> onConnection(stage, msg));

        server.socketsOut().connect(procConn.dataIn());

        server.start();

    }

    private static ServerSocket newSocket ()
            throws IOException
    {
        final ServerSocket sock = new ServerSocket();
        sock.bind(new InetSocketAddress("127.0.0.1", 8080));
        return sock;
    }

    private static void onConnection (final Stage stage,
                                      final TcpSocket<String> conn)
    {
        final Processor<String> echo = Processor.fromFunctionScript(stage, msg -> "S = " + msg + "\n");
        final Printer<String> printer = Printer.newPrintln(stage);
        conn.dataOut().connect(printer.dataIn());
        conn.dataOut().connect(echo.dataIn());
        conn.dataIn().connect(echo.dataOut());

        conn.start(TcpServer::onRead, TcpServer::onWrite);
    }

    private static String onRead (final SocketInputStream in)
            throws IOException
    {
        final String str = "X" + in.readByte();
        return str;
    }

    private static void onWrite (final SocketOutputStream out,
                                 final String message)
            throws IOException
    {
        out.writeBytes(message);
        out.flush();
    }
}
