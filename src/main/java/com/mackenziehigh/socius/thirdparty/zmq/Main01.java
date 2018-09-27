package com.mackenziehigh.socius.thirdparty.zmq;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.socius.time.Clock;
import java.io.IOException;

public final class Main01
{
    public static void main (String[] args)
            throws InterruptedException,
                   IOException
    {
        final Cascade.Stage stage = Cascade.newStage();

        final Clock clock = new Clock(stage);
        clock.start();

        final EchoBrokerClient client = new EchoBrokerClient(stage);
        client.start();

        client.publish(clock.clockOut().actor(), "ticks");

        System.in.read();
    }
}
