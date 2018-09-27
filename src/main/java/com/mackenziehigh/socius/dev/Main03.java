package com.mackenziehigh.socius.dev;

import io.nats.client.Connection;
import io.nats.client.Nats;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 *
 */
public class Main03
{
    public static void main (String[] args)
            throws IOException,
                   InterruptedException,
                   TimeoutException
    {
        final Connection conn = Nats.connect();

        while (true)
        {
            conn.publish("girls", "Erin".getBytes());
            conn.flush(Duration.ZERO);
            System.out.println("S = " + conn.getStatus());
            Thread.sleep(1000);
        }
    }
}
