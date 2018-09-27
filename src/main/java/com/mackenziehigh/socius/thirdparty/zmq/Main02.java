package com.mackenziehigh.socius.thirdparty.zmq;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.socius.io.Printer;
import java.io.IOException;

public final class Main02
{
    public static void main (String[] args)
            throws InterruptedException,
                   IOException
    {
        final Cascade.Stage stage = Cascade.newStage();

        final Printer printer = new Printer(stage);

        final EchoBrokerClient client = new EchoBrokerClient(stage);
        client.start();

        client.subscribe(printer.dataIn().actor(), "ticks");

        System.in.read();
    }
}
