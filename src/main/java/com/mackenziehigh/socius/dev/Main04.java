package com.mackenziehigh.socius.dev;

import io.nats.client.Connection;
import io.nats.client.Nats;
import java.io.IOException;

/**
 *
 */
public final class Main04
{
    public static void main (String[] args)
            throws IOException,
                   InterruptedException
    {
        final Connection conn = Nats.connect();
        conn.createDispatcher((msg) -> System.out.println("XX = " + new String(msg.getData()))).subscribe("girls");
    }
}
