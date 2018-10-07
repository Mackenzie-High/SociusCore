package com.mackenziehigh.socius.dev;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.web.WebReceiver;
import com.mackenziehigh.socius.web.WebServer;
import java.io.IOException;

public final class Main01
{
    public static void main (String[] args)
            throws IOException
    {
        final Stage stage = Cascade.newStage();

        final WebServer server = WebServer.newWebServer().withPort(8082).build().start();

        final WebReceiver recv = WebReceiver.newWebReceiver(stage).build();

        server.requestsOut().connect(recv.requestIn());
        server.responsesIn().connect(recv.responseOut());

        System.in.read();
    }
}
