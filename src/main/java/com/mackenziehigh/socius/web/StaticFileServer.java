package com.mackenziehigh.socius.web;

import com.google.common.base.Preconditions;
import com.google.common.net.MediaType;
import com.google.protobuf.ByteString;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.gpb.http_m;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

/**
 * Simple File Server.
 *
 * TODO: Resources too?
 */
public final class StaticFileServer
{
    private final Processor<http_m.Request> procDataIn;

    private final Processor<http_m.Response> procDataOut;

    private final File webroot;

    private StaticFileServer (final Builder builder)
    {
        this.procDataIn = Processor.newProcessor(builder.stage, this::onRequest);
        this.procDataOut = Processor.newProcessor(builder.stage);
        this.webroot = builder.root;
        Preconditions.checkArgument(webroot.isDirectory(), "Web Root must be a directory: %s", webroot);
    }

    public Input<http_m.Request> dataIn ()
    {
        return procDataIn.dataIn();
    }

    public Output<http_m.Response> dataOut ()
    {
        return procDataOut.dataOut();
    }

    private void onRequest (final http_m.Request request)
            throws IOException
    {
        final String path = request.getPath();

        try
        {
            if (path.matches("[A-Za-z0-9\\-_\\/]+([\\.]?[A-Za-z0-9\\-_]+)") == false)
            {
                sendNotFound(request, path);
            }

            final File file = new File(webroot, path);

            if (file.isFile())
            {
                sendFile(request, file);
            }
            else
            {
                sendNotFound(request, path);
            }
        }
        catch (Throwable ex)
        {
            sendNotFound(request, path);
        }
    }

    private void sendFile (final http_m.Request request,
                           final File file)
            throws IOException
    {
        final http_m.Response.Builder builder = http_m.Response.newBuilder();
        builder.setRequest(request);
        builder.setCorrelationId(request.getCorrelationId());
        builder.setTimestamp(System.currentTimeMillis());
        builder.setContentType(mediaType(file));
        builder.setStatus(200);
        builder.setBody(ByteString.copyFrom(Files.readAllBytes(file.toPath())));

        procDataOut.dataIn().send(builder.build());
    }

    private String mediaType (final File file)
            throws IOException
    {
        final String contentType = Files.probeContentType(file.toPath());
        return contentType == null ? MediaType.OCTET_STREAM.toString() : contentType;
    }

    private void sendNotFound (final http_m.Request request,
                               final String path)
    {
        final http_m.Response.Builder builder = http_m.Response.newBuilder();
        builder.setRequest(request);
        builder.setCorrelationId(request.getCorrelationId());
        builder.setTimestamp(System.currentTimeMillis());
        builder.setContentType("text/html");
        builder.setStatus(404);
        builder.setBody(ByteString.copyFrom(String.format("Not Found: %s", path).getBytes()));

        procDataOut.dataIn().send(builder.build());
    }

    public static Builder newStaticFileServer (final Stage stage)
    {
        return new Builder(stage);
    }

    public static final class Builder
    {
        private final Stage stage;

        private File root;

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        public Builder withWebRoot (final File directory)
        {
            this.root = Objects.requireNonNull(directory, "directory");
            return this;
        }

        public StaticFileServer build ()
        {
            return new StaticFileServer(this);
        }
    }
}
