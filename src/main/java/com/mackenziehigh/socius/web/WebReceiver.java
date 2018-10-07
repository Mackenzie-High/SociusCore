package com.mackenziehigh.socius.web;

import com.google.protobuf.ByteString;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.gpb.http_m.Request;
import com.mackenziehigh.socius.gpb.http_m.Response;
import java.util.Objects;

/**
 *
 */
public final class WebReceiver
{
    private final Processor<Request> procRequestIn;

    private final Processor<Request> procRequestOut;

    private final Processor<Response> procResponseOut;

    private final Response protoResponse;

    private WebReceiver (final Builder builder)
    {
        this.procRequestIn = Processor.newProcessor(builder.stage, this::onMessage);
        this.procRequestOut = Processor.newProcessor(builder.stage);
        this.procResponseOut = Processor.newProcessor(builder.stage);
        this.protoResponse = builder.protoResponse;
    }

    private void onMessage (final Request message)
    {
        /**
         * Forward the request to any interested parties.
         */
        procRequestOut.dataIn().send(message);

        /**
         * Create the response to send back to the HTTP client.
         */
        final Response response = Response
                .newBuilder(protoResponse)
                .setRequest(message)
                .setTimestamp(System.currentTimeMillis())
                .setCorrelationId(message.getCorrelationId())
                .build();

        /**
         * Send the response back to the client.
         */
        procResponseOut.dataIn().send(response);
    }

    public Input<Request> requestIn ()
    {
        return procRequestIn.dataIn();
    }

    public Output<Request> requestOut ()
    {
        return procRequestOut.dataOut();
    }

    public Output<Response> responseOut ()
    {
        return procResponseOut.dataOut();
    }

    public static Builder newWebReceiver (final Stage stage)
    {
        return new Builder(stage);
    }

    public static final class Builder
    {
        private final Stage stage;

        private Response protoResponse;

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
            this.protoResponse = Response.newBuilder()
                    .setStatus(200)
                    .setContentType("text/plain")
                    .setBody(ByteString.copyFromUtf8("Message Accepted\r\n"))
                    .build();
        }

        public Builder withResponse (final Response response)
        {
            this.protoResponse = Objects.requireNonNull(response, "response");
            return this;
        }

        public WebReceiver build ()
        {
            return new WebReceiver(this);
        }
    }
}
