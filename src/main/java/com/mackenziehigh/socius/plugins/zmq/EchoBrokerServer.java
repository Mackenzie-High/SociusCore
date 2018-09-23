package com.mackenziehigh.socius.plugins.zmq;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.inception.Kernel.KernelApi;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

/**
 *
 */
public final class EchoBrokerServer
{
    private final ZContext context = new ZContext(1);

    private volatile String subscriberAddress = "tcp://*:2995";

    private volatile String publisherAddress = "tcp://*:2996";

    private final AtomicBoolean stop = new AtomicBoolean(false);

    public EchoBrokerServer (final KernelApi kapi)
    {
        this(kapi.stage());
    }

    public EchoBrokerServer (final Stage stage)
    {

    }

    public EchoBrokerServer start ()
    {
        final Socket socketIn = context.createSocket(ZMQ.SUB);
        final Socket socketOut = context.createSocket(ZMQ.PUB);

        socketIn.bind(subscriberAddress);
        socketOut.bind(publisherAddress);

        socketIn.subscribe("");

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        final Thread thread = new Thread(() -> run(socketIn, socketOut));
        thread.setDaemon(true);
        thread.start();

        return this;
    }

    public EchoBrokerServer stop ()
    {
        if (stop.compareAndSet(false, true))
        {
            context.close();
        }
        return this;
    }

    private void run (final Socket socketIn,
                      final Socket socketOut)
    {
        while (stop.get() == false)
        {
            final byte[] message = socketIn.recv();
            System.out.println("Got One!");
            socketOut.send(message);
        }
    }

    public static void main (String[] args)
            throws IOException
    {
        final EchoBrokerServer server = new EchoBrokerServer(Cascade.newStage());
        server.start();

        System.in.read();
    }

}
