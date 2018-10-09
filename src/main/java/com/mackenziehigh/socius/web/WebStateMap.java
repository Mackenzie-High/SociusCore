package com.mackenziehigh.socius.web;

import com.google.common.collect.Maps;
import com.google.common.net.MediaType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.protobuf.ByteString;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.FunctionScript;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.LookupInserter;
import com.mackenziehigh.socius.flow.Mapper;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.gpb.http_m.Request;
import com.mackenziehigh.socius.gpb.http_m.Response;
import com.mackenziehigh.socius.io.Printer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Lazily builds a JSON Map representing the current state of a web-application.
 * The state is retrievable via HTTP requests.
 */
public final class WebStateMap
{
    private final String instanceUUID = UUID.randomUUID().toString();

    private final Stage stage;

    private final Map<String, String> map = Maps.newConcurrentMap();

    private final Mapper<Request, Response> server;

    private final Gson gson = new GsonBuilder().create();

    private WebStateMap (final Stage stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.server = Mapper.newMapper(stage, this::onRequest);
    }

    private Response onRequest (final Request request)
    {
        final String json = gson.toJson(map);

        final Response.Builder response = Response.newBuilder();
        response.setRequest(request);
        response.setStatus(200);
        response.setCorrelationId(request.getCorrelationId());
        response.setTimestamp(System.currentTimeMillis());
        response.setResponderName(WebStateMap.class.getName());
        response.setResponderId(instanceUUID);
        response.setContentType(MediaType.JSON_UTF_8.toString());
        response.setBody(ByteString.copyFrom(json.getBytes(StandardCharsets.UTF_8)));

        return response.build();
    }

    public WebStateMap clear ()
    {
        map.clear();
        return this;
    }

    public Input<Request> requestIn ()
    {
        return server.dataIn();
    }

    public Output<Response> responseOut ()
    {
        return server.dataOut();
    }

    public <T> Input<T> dataIn (final String key,
                                final FunctionScript<T, String> extractor)
    {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(extractor, "extractor");
        final Mapper<T, String> transform = Mapper.newMapper(stage, extractor);
        final Processor<String> inserter = Processor.newProcessor(stage, (String x) -> map.put(key, x));
        transform.dataOut().connect(inserter.dataIn());
        return transform.dataIn();
    }

    public static WebStateMap newWebStateMap (final Stage stage)
    {
        return new WebStateMap(stage);
    }

    public static void main (String[] args)
            throws IOException
    {
        final Stage stage = Cascade.newStage();
        final WebServer server = WebServer.newWebServer().withPort(8083).build();
        final Printer printer = Printer.newPrintln(stage);
        final WebReceiver recv1 = WebReceiver.newWebReceiver(stage).build();
        final WebReceiver recv2 = WebReceiver.newWebReceiver(stage).build();
        final WebStateMap smap = WebStateMap.newWebStateMap(stage);
        final LookupInserter<Request> router = LookupInserter.newLookupInserter(stage);
        printer.dataOut().connect(router.dataIn());
        router.selectIf(x -> x.getUri().contains("set-state")).connect(recv1.requestIn());
        router.selectIf(x -> x.getUri().contains("set-city")).connect(recv2.requestIn());
        router.selectIf(x -> true).connect(smap.requestIn());
        final Mapper<Request, String> action1 = Mapper.newMapper(stage, (Request r) -> r.getBody().toStringUtf8());
        final Mapper<Request, String> action2 = Mapper.newMapper(stage, (Request r) -> r.getBody().toStringUtf8());
        recv1.requestOut().connect(action1.dataIn());
        recv2.requestOut().connect(action2.dataIn());
        action1.dataOut().connect(smap.dataIn("state", x -> x.toLowerCase()));
        action2.dataOut().connect(smap.dataIn("city", x -> x.toLowerCase()));
        server.requestsOut().connect(printer.dataIn());
        server.responsesIn().connect(recv1.responseOut());
        server.responsesIn().connect(recv2.responseOut());
        server.responsesIn().connect(smap.responseOut());
        server.start();

        System.in.read();
    }
}
