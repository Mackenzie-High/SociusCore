package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.socius.Processor;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import com.mackenziehigh.socius.Sink;

/**
 *
 * @author mackenzie
 */
public abstract class UdpRadio<T>
        implements Sink<T>
{
    private final SocketAddress address;

    private final int length;

    private final Processor<T> procOut;

    private final Thread thread = new Thread(this::run);

    private final AtomicBoolean stop = new AtomicBoolean();

    private volatile DatagramSocket socket;

    private final DatagramPacket packet;

    public UdpRadio (final Cascade.ActorFactory factory,
                     final SocketAddress address,
                     final int length)
    {
        this.address = Objects.requireNonNull(address, "address");
        this.length = length;
        this.procOut = Processor.fromConsumerScript(factory, this::onMessage);

        final byte[] buffer = new byte[length];
        packet = new DatagramPacket(buffer, length);
    }

    protected abstract void encode (DatagramPacket out,
                                    T message);

    @Override
    public Input<T> dataIn ()
    {
        return procOut.dataIn();
    }

    public final UdpRadio<T> start ()
    {
        thread.start();
        return this;
    }

    public final UdpRadio<T> stop ()
    {
        stop.set(true);

        if (socket != null)
        {
            socket.close();
        }

        return this;
    }

    private void onMessage (final T message)
            throws IOException
    {
        if (socket != null)
        {
            encode(packet, message);
            socket.send(packet);
        }
    }

    private void run ()
    {
        try (DatagramSocket dsock = new DatagramSocket(address))
        {
            socket = dsock;
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
            // Pass.
        }

    }
}
