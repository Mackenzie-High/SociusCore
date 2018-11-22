package com.mackenziehigh.socius.dev;

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.WebServer;

/**
 *
 */
public final class ConstantHandler
        implements SmallSite.HttpHandler
{
    @Override
    public boolean isMatch (final WebServer.HttpRequest message)
    {
        return true;
    }

    @Override
    public Input<WebServer.HttpRequest> dataIn ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Output<WebServer.HttpResponse> dataOut ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
