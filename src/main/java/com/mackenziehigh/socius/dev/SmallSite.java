package com.mackenziehigh.socius.dev;

import com.google.common.collect.Lists;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.ConcurrentOptionList;
import com.mackenziehigh.socius.WebServer;
import java.util.List;
import java.util.Objects;

/**
 *
 */
public final class SmallSite
{
    public interface HttpHandler
            extends ConcurrentOptionList.Option<WebServer.HttpRequest, WebServer.HttpResponse>
    {
        // Pass.
    }

    private final ConcurrentOptionList<WebServer.HttpRequest, WebServer.HttpResponse> httpProcessors;

    private SmallSite (final Builder builder)
    {
        this.httpProcessors = null;
    }

    public Input<WebServer.HttpRequest> httpIn ()
    {
        return httpProcessors.dataIn();
    }

    public Output<WebServer.HttpResponse> httpOut ()
    {
        return httpProcessors.dataOut();
    }

    public Output<WebServer.HttpRequest> httpDropsOut ()
    {
        return httpProcessors.dropsOut();
    }

    public static Builder newBuilder (final Stage stage)
    {
        return new Builder(stage);
    }

    public static final class Builder
    {
        private final Stage stage;

        private final List<HttpHandler> httpHandlers = Lists.newArrayList();

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        public Builder withHandler (final HttpHandler handler)
        {
            Objects.requireNonNull(handler, "handler");
            httpHandlers.add(handler);
            return this;
        }

        public SmallSite build ()
        {
            return new SmallSite(this);
        }
    }
}
