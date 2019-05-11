package com.mackenziehigh.socius.io.tcp;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.Processor;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import com.mackenziehigh.socius.flow.Pipeline;

/**
 * Bidirectional message-oriented TCP-based socket.
 *
 * @param <T> is the type of the incoming and outgoing messages.
 */
public final class TcpSocket<T>
        implements Pipeline<T, T>
{
    /**
     * Reads a message from a socket.
     *
     * @param <T> is the type of the incoming messages.
     */
    public interface SocketReader<T>
            extends Closeable
    {
        /**
         * This method will be invoked by the <code>TcpSocket</code> object
         * in order to pass-in the socket that shall be read from.
         *
         * @param socket shall be read from.
         * @throws IOException if something goes wrong.
         */
        public default void setSocket (Socket socket)
                throws IOException
        {
            // Pass.
        }

        public default void setInputStream (InputStream stream)
        {
            // Pass.
        }

        /**
         * This method will be repeatedly invoked by the <code>TcpSocket</code> object
         * in order to read-in messages from the socket, blocking as necessary.
         *
         * @return the next message from the socket.
         * @throws IOException if something goes wrong.
         */
        public T onRead ()
                throws IOException;

        public boolean isComplete ();
    }

    /**
     * Writes a message to a socket.
     *
     * @param <T> is the type of the outgoing messages.
     */
    public interface SocketWriter<T>
            extends Closeable
    {
        /**
         * This method will be invoked by the <code>TcpSocket</code> object
         * in order to pass-in the socket that shall be written to.
         *
         * @param socket shall be written to.
         * @throws IOException if something goes wrong.
         */
        public default void setSocket (Socket socket)
                throws IOException
        {
            // Pass.
        }

        public default void setOutputStream (OutputStream stream)
        {
            // Pass.
        }

        /**
         * This method will be repeatedly invoked by the <code>TcpSocket</code> object
         * in order to write messages to the socket, blocking as necessary.
         *
         * @param message is the next message to write to the socket.
         * @throws IOException if something goes wrong.
         */
        public void onWrite (T message)
                throws IOException;

        public boolean isComplete ();
    }

    /**
     * This is the socket that will be read-from and written-to.
     */
    private final Socket socket;

    /**
     * This thread will be used to read messages from the socket.
     */
    private final Thread readerThread = new Thread(this::readLoop);

    /**
     * This thread will be used to write messages to the socket.
     */
    private final Thread writerThread = new Thread(this::writeLoop);

    /**
     * These are the messages that need written to the socket.
     */
    private final BlockingQueue<T> pending;

    /**
     * This actor provides the data-in connector, which supplies
     * the messages to write to the socket.
     */
    private final Processor<T> dataIn;

    /**
     * This actor provides the data-out connector, which receives
     * the messages that are read-in from the socket.
     */
    private final Processor<T> dataOut;

    private final Processor<Throwable> rethrower;

    /**
     * This flag will become true, when the user decides to close the socket.
     */
    private final AtomicBoolean stop = new AtomicBoolean();

    private final CountDownLatch shutdownLatch = new CountDownLatch(2);

    /**
     * This object performs the actual reading from the socket.
     */
    private volatile TcpSocket.SocketReader<T> reader;

    /**
     * This object performs the actual writing to the socket.
     */
    private volatile TcpSocket.SocketWriter<T> writer;

    private volatile boolean readerComplete = false;

    private volatile boolean writerComplete = false;

    private TcpSocket (final Stage stage,
                       final Socket socket)
    {
        this.socket = socket;
        this.pending = new ArrayBlockingQueue<>(256);
        this.dataIn = Processor.fromConsumerScript(stage, msg -> pending.add(msg));
        this.dataOut = Processor.fromIdentityScript(stage);
        this.rethrower = Processor.fromConsumerScript(stage, ex -> rethrow(ex));
        this.readerThread.setName("TcpSocket.Reader");
        this.writerThread.setName("TcpSocket.Writer");
        this.readerThread.setDaemon(true);
        this.writerThread.setDaemon(true);
    }

    /**
     * This input supplies the messages to write to the socket.
     *
     * {@inheritDoc}
     */
    @Override
    public Input<T> dataIn ()
    {
        return dataIn.dataIn();
    }

    /**
     * This output receives the messages that are read from the socket.
     *
     * {@inheritDoc}
     */
    @Override
    public Output<T> dataOut ()
    {
        return dataOut.dataOut();
    }

    /**
     * Cause this object to spawn threads and begin reading and writing
     * messages to the socket, as messages and data become available.
     *
     * @param reader will be used to read from the socket.
     * @param writer will be used to write to the socket.
     * @return this.
     */
    public TcpSocket<T> start (final TcpSocket.SocketReader<T> reader,
                               final TcpSocket.SocketWriter<T> writer)
    {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.writer = Objects.requireNonNull(writer, "writer");
        readerThread.start();
        writerThread.start();
        return this;
    }

    /**
     * Cause this object to shutdown its threads.
     *
     * @return this.
     */
    public TcpSocket<T> stop ()
    {
        if (stop.compareAndSet(false, true))
        {
            try
            {
                socket.close();
                shutdownLatch.await();
            }
            catch (Throwable ex)
            {
                // Pass.
            }
        }

        return this;
    }

    private void rethrow (Throwable ex)
            throws IOException
    {
        throw new IOException(ex);
    }

    /**
     * This method defines the behavior of the reader thread.
     */
    private void readLoop ()
    {
        try (SocketReader<T> sr = reader)
        {
            /**
             * Configure the reader object.
             */
            reader.setSocket(socket);
            reader.setInputStream(socket.getInputStream());

            /**
             * Read messages from the socket and transmit them
             * out of this actor via the data-out connector.
             */
            while (isAlive())
            {
                final T message = reader.onRead();

                if (message == null)
                {
                    throw new NullPointerException("Null was returned by onRead().");
                }
                else
                {
                    dataOut.accept(message);
                }

                readerComplete = reader.isComplete();
            }
        }
        catch (Throwable ex)
        {
            rethrower.accept(ex);
        }
        finally
        {
            shutdownLatch.countDown();
            stop();
        }
    }

    /**
     * This method defines the behavior of the writer socket.
     */
    private void writeLoop ()
    {
        try (SocketWriter<T> sw = writer)
        {
            /**
             * Configure the writer object.
             */
            writer.setSocket(socket);
            writer.setOutputStream(socket.getOutputStream());

            /**
             * Write the messages to the socket, which were sent
             * to us via the data-in connector.
             */
            while (isAlive())
            {
                final T message = pending.take();

                if (message != null)
                {
                    writer.onWrite(message);
                }

                writerComplete = writer.isComplete();
            }
        }
        catch (Throwable ex)
        {
            rethrower.accept(ex);
        }
        finally
        {
            shutdownLatch.countDown();
            stop();
        }
    }

    private boolean isAlive ()
    {
        return !readerComplete && !writerComplete && !stop.get();
    }

    /**
     * Create a new TCP socket.
     *
     * @param <T> is the type of the messages flowing through the socket.
     * @param stage will be used to create private actors.
     * @param socket is the underlying socket.
     * @return the new socket.
     */
    public static <T> TcpSocket<T> newTcpSocket (final Stage stage,
                                                 final Socket socket)
    {
        return new TcpSocket<>(stage, socket);
    }
}
