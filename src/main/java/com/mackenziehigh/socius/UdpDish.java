package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade.ActorFactory;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.Processor;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import com.mackenziehigh.socius.Source;

/**
 *
 * @author mackenzie
 */
public abstract class UdpDish<T>
        implements Source<T>
{
    private final SocketAddress address;

    private final int length;

    private final Processor<T> procOut;

    private final Thread thread = new Thread(this::run);

    private final AtomicBoolean stop = new AtomicBoolean();

    private volatile DatagramSocket socket;

    public UdpDish (final ActorFactory factory,
                    final SocketAddress address,
                    final int length)
    {
        this.address = Objects.requireNonNull(address, "address");
        this.length = length;
        this.procOut = Processor.fromIdentityScript(factory);
    }

    protected abstract T decode (DatagramPacket packet);

    @Override
    public Output<T> dataOut ()
    {
        return procOut.dataOut();
    }

    public final UdpDish<T> start ()
    {
        thread.start();
        return this;
    }

    public final UdpDish<T> stop ()
    {
        stop.set(true);

        if (socket != null)
        {
            socket.close();
        }

        return this;
    }

    private void run ()
    {
        final byte[] buffer = new byte[length];
        final DatagramPacket packet = new DatagramPacket(buffer, length);

        while (stop.get() == false)
        {
            try (DatagramSocket dsock = new DatagramSocket())
            {
                socket = dsock;

                socket.connect(address);

                while (true)
                {
                    socket.receive(packet);
                    final T message = decode(packet);
                    procOut.accept(message);
                }
            }
            catch (Throwable ex)
            {
                // Pass.
            }
        }
    }
}
