package com.mackenziehigh.socius.io;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 *
 * @author mackenzie
 */
public class Main
{
    public static void main (String[] args)
            throws IOException,
                   InterruptedException
    {
        final Stage stage = Cascade.newStage(1);
        final Sender sender = new Sender(stage, new InetSocketAddress("127.0.0.1", 8080), 100);
        final Recv receiver = new Recv(stage, new InetSocketAddress("127.0.0.1", 8080), 100);
        final Printer<String> printer = Printer.newPrintln(stage, "X = %s");
        receiver.dataOut().connect(printer.dataIn());

        sender.start();
        receiver.start();
        Thread.sleep(1000);

        sender.accept("Hello");

        System.in.read();
    }

    private static final class Sender
            extends UdpRadio<String>
    {
        public Sender (Cascade.ActorFactory factory,
                       SocketAddress address,
                       int length)
        {
            super(factory, address, length);
        }

        @Override
        protected void encode (DatagramPacket out,
                               String message)
        {
            out.setData(message.getBytes());
        }
    };

    private static final class Recv
            extends UdpDish<String>
    {
        public Recv (Cascade.ActorFactory factory,
                     SocketAddress address,
                     int length)
        {
            super(factory, address, length);
        }

        @Override
        protected String decode (final DatagramPacket packet)
        {
            return new String(packet.getData());
        }

    }
}
