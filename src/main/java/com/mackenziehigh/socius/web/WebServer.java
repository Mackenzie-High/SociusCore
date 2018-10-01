package com.mackenziehigh.socius.web;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.Mapper;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.web.HTTP.Header;
import com.mackenziehigh.socius.web.HTTP.Protocol;
import com.mackenziehigh.socius.web.HTTP.QueryParameter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A non-blocking HTTP server based on the Netty framework.
 */
public final class WebServer
{
    private static final Logger logger = LogManager.getLogger(WebServer.class);

    /**
     * This counter is used to assign sequence-numbers to requests.
     */
    private static final AtomicLong seqnum = new AtomicLong();

    /**
     * This is the human-readable name of this server to embed in requests.
     */
    private final String serverName;

    /**
     * This is the UUID of this server lifetime to embed in requests.
     */
    private final String serverId = UUID.randomUUID().toString();

    /**
     * This user-defined value will be embedded in requests.
     */
    private final String replyTo;

    /**
     * This is the host that the server will listen on.
     */
    private final String host;

    /**
     * This is the port that the server will listen on.
     */
    private final int port;

    /**
     * Connections will be closed, if a response is not received within this timeout.
     */
    private final Duration responseTimeout;

    /**
     * HTTP Chunk Aggregation Limit.
     */
    private final int aggregationCapacity;

    /**
     * This flag will be set to true, when start() is called.
     */
    private final AtomicBoolean started = new AtomicBoolean();

    /**
     * This flag will be set to true, when stop() is called.
     */
    private final AtomicBoolean stopped = new AtomicBoolean();

    /**
     * A reference to this object is needed in order to be able to stop the server.
     */
    private volatile ChannelFuture shutdownHook;

    /**
     * This stage is used to create private actors herein.
     *
     * <p>
     * Since a server is an I/O end-point, is expected to have high throughput,
     * and needs to have low latency, the server will have its own dedicated stage.
     * </p>
     */
    private final Stage stage = Cascade.newStage();

    /**
     * This map maps correlation UUIDs of requests to consumer functions
     * that will be used to process the corresponding responses.
     *
     * <p>
     * Entries are added to this map whenever an HTTP request is processed.
     * Entries are lazily removed from this map, if they have been there too long.
     * </p>
     */
    private final Map<String, Connection> connections = Maps.newConcurrentMap();

    /**
     * This queue is used to remove connections from the map of connections.
     */
    private final Queue<Connection> responseTimeoutQueue = new PriorityQueue<>();

    /**
     * This processor will be used to send HTTP requests out of the server,
     * so that external handler actors can process the requests.
     */
    private final Processor<HTTP.Request> requestsOut = Processor.newProcessor(stage);

    /**
     * This processor will receive the HTTP responses from the external actors
     * and then will route those responses to the originating connection.
     */
    private final Processor<HTTP.Response> responsesIn = Processor.newProcessor(stage, this::onResponse);

    /**
     * Sole Constructor.
     *
     * @param builder contains the initial server settings.
     */
    private WebServer (final Builder builder)
    {
        this.host = builder.host;
        this.port = builder.port;
        this.aggregationCapacity = builder.aggregationCapacity;
        this.serverName = builder.serverName;
        this.replyTo = builder.replyTo;
        this.responseTimeout = builder.responseTimeout;
    }

    /**
     * Use this connection to receive HTTP Requests from this HTTP server.
     *
     * @return the connection.
     */
    public Output<HTTP.Request> requestsOut ()
    {
        return requestsOut.dataOut();
    }

    /**
     * Use this connection to send HTTP Responses to this HTTP server.
     *
     * <p>
     * If the HTTP Response correlates to an existing live HTTP Request,
     * then the response will be forwarded to the associated client.
     * </p>
     *
     * <p>
     * If the HTTP Response does not correlate to an existing live HTTP Request,
     * then the response will be silently dropped.
     * </p>
     *
     * @return the connection.
     */
    public Input<HTTP.Response> responsesIn ()
    {
        return responsesIn.dataIn();
    }

    /**
     * Use this method to start the server.
     *
     * <p>
     * This method has no effect, if the server was already started.
     * </p>
     *
     * @return this.
     */
    public WebServer start ()
    {
        if (started.compareAndSet(false, true))
        {
            logger.info("Starting Server");
            final Thread thread = new Thread(this::run);
            thread.start();
        }
        return this;
    }

    /**
     * Use this method to shutdown the server and release its threads.
     *
     * <p>
     * This method has no effect, if the server already begun to stop.
     * </p>
     *
     * @return this.
     * @throws java.lang.InterruptedException
     */
    public WebServer stop ()
            throws InterruptedException
    {
        if (stopped.compareAndSet(false, true))
        {
            logger.info("Stopping Server");

            if (shutdownHook != null)
            {
                shutdownHook.channel().close().sync();
            }
        }
        return this;
    }

    /**
     * Server Main.
     */
    private void run ()
    {
        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workerGroup = new NioEventLoopGroup();
        try
        {
            Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));

            final Thread thread = new Thread(this::prunePendingResponses);
            thread.setDaemon(true);
            thread.start();

            final ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new Initializer());

            shutdownHook = b.bind(host, port).sync();
            shutdownHook.channel().closeFuture().sync();
        }
        catch (InterruptedException ex)
        {
            logger.catching(ex);
        }
        finally
        {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private void onShutdown ()
    {
        try
        {
            stop();
        }
        catch (InterruptedException ex)
        {
            logger.error(ex);
        }
    }

    private void onResponse (final HTTP.Response response)
    {
        routeResponse(response);
    }

    /**
     * This actor receives the HTTP Responses from the external handlers connected to this server,
     * routes them to the appropriate client-server connection, and then transmits them via Netty.
     *
     * @param response needs to be send to a client.
     */
    private void routeResponse (final HTTP.Response response)
    {
        /**
         * Get the Correlation-ID that allows us to map responses to requests.
         * Whoever created the response should have included the Correlation-ID.
         * If they did not, they may have implicitly included it, by including the request itself.
         */
        final String correlationId;

        if (response.hasCorrelationId())
        {
            correlationId = response.getCorrelationId();
        }
        else if (response.hasRequest() && response.getRequest().hasCorrelationId())
        {
            correlationId = response.getRequest().getCorrelationId();
        }
        else
        {
            /**
             * No Correlation-ID is present.
             * Therefore, we will not be able to find the relevant client-server connection.
             * Drop the response silently.
             */
            return;
        }

        /**
         * This consumer will take the response and send it to the client.
         * This consumer is a one-shot operation.
         */
        final Connection connection = connections.get(correlationId);

        if (connection == null)
        {
            return;
        }

        /**
         * Send the HTTP Response to the client, if they are still connected.
         */
        connection.send(response);
    }

    private void prunePendingResponses ()
    {
        while (responseTimeoutQueue.isEmpty() == false)
        {
            final Connection conn = responseTimeoutQueue.peek();

            if (conn.timeout.isBefore(Instant.now().minus(responseTimeout)))
            {
                conn.close();
            }
            else
            {
                break;
            }
        }
    }

    /**
     * Logic to setup the Netty pipeline to handle a <b>single</b> connection.
     *
     * <p>
     * The logic herein is executed whenever a new connection is established!
     * </p>
     */
    private final class Initializer
            extends ChannelInitializer<SocketChannel>
    {
        @Override
        protected void initChannel (final SocketChannel channel)
                throws Exception
        {
            channel.pipeline().addLast(new HttpResponseEncoder());
            channel.pipeline().addLast(new HttpRequestDecoder()); // TODO: Args?
            channel.pipeline().addLast(new HttpObjectAggregator(aggregationCapacity, true));
            channel.pipeline().addLast(new HttpHandler());
        }

    }

    /**
     * An instance of this class will be used to translate
     * an incoming HTTP Request into a Protocol Buffer
     * and then send the Protocol Buffer to external handlers.
     *
     * <p>
     * A new instance of this class will be created per connection!
     * </p>
     */
    private final class HttpHandler
            extends SimpleChannelInboundHandler<Object>
    {
        @Override
        public void channelReadComplete (final ChannelHandlerContext ctx)
                throws Exception
        {
            ctx.flush();
        }

        @Override
        protected void channelRead0 (final ChannelHandlerContext ctx,
                                     final Object msg)
                throws Exception
        {
            if (msg instanceof FullHttpRequest)
            {
                final HTTP.Request encodedRequest = encode((FullHttpRequest) msg);
                final String correlationId = encodedRequest.getCorrelationId();

                /**
                 * Create the response handler that will be used to route
                 * the corresponding HTTP Response, if and when it occurs.
                 */
                Verify.verify(connections.containsKey(correlationId) == false);
                final Connection connection = new Connection(correlationId, Instant.now(), ctx);
                responseTimeoutQueue.add(connection);
                connections.put(correlationId, connection);

                /**
                 * Send the HTTP Request to the external actors,
                 * so they can form an HTTP Response.
                 */
                requestsOut.dataIn().send(encodedRequest);
            }
        }

        @Override
        public void exceptionCaught (final ChannelHandlerContext ctx,
                                     final Throwable cause)
                throws Exception
        {
            /**
             * Log the exception.
             */
            logger.warn(cause);

            /**
             * Notify the client of the error, but do not tell them why (for security).
             */
            final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);

            /**
             * Send the response to the client.
             */
            ctx.writeAndFlush(response);

            /**
             * Close the connection.
             */
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }

        private HTTP.Request encode (final FullHttpRequest request)
                throws URISyntaxException
        {
            final String correlationId = UUID.randomUUID().toString();

            final HTTP.Request.Builder builder = HTTP.Request.newBuilder();

            builder.setServerName(serverName);
            builder.setServerId(serverId);
            builder.setSequenceNumber(seqnum.getAndIncrement());
            builder.setTimestamp(System.currentTimeMillis());
            builder.setCorrelationId(correlationId);
            builder.setReplyTo(replyTo);
            builder.setProtocol(Protocol.newBuilder()
                    .setText(request.protocolVersion().text())
                    .setName(request.protocolVersion().protocolName())
                    .setMajorVersion(request.protocolVersion().majorVersion())
                    .setMinorVersion(request.protocolVersion().minorVersion()));
            builder.setMethod(request.method().name());

            /**
             * Encode the URL.
             */
            final QueryStringDecoder qsDecoder = new QueryStringDecoder(request.uri());
            builder.setUri(qsDecoder.uri());
            builder.setPath(qsDecoder.path());
            builder.setRawPath(qsDecoder.rawPath());
            builder.setRawQuery(qsDecoder.rawQuery());

            /**
             * Encode Query Parameters.
             */
            for (Entry<String, List<String>> params : qsDecoder.parameters().entrySet())
            {
                final QueryParameter.Builder param = QueryParameter.newBuilder();
                param.setKey(params.getKey());
                param.addAllValues(params.getValue());

                builder.putParameters(param.getKey(), param.build());
            }

            /**
             * Encode Host.
             */
            if (request.headers().contains(Names.HOST))
            {
                builder.setHost(request.headers().get(Names.HOST));
            }

            /**
             * Encode Content Type.
             */
            if (request.headers().contains(Names.CONTENT_TYPE))
            {
                builder.setContentType(request.headers().get(Names.CONTENT_TYPE));
            }

            /**
             * Encode the headers.
             */
            for (String name : request.headers().names())
            {
                builder.putHeaders(name,
                                   Header.newBuilder()
                                           .setKey(name)
                                           .addAllValues(request.headers().getAll(name))
                                           .build());
            }

            /**
             * Encode the cookies.
             */
            for (String header : request.headers().getAll(Names.COOKIE))
            {
                for (Cookie cookie : ServerCookieDecoder.STRICT.decode(header))
                {
                    final HTTP.Cookie.Builder cookieBuilder = HTTP.Cookie.newBuilder();

                    cookieBuilder.setDomain(cookie.domain());
                    cookieBuilder.setHttpOnly(cookie.isHttpOnly());
                    cookieBuilder.setSecure(cookie.isSecure());
                    cookieBuilder.setPath(cookie.path());
                    cookieBuilder.setMaxAge(cookie.maxAge());

                    builder.addCookies(cookieBuilder);
                }
            }

            /**
             * Encode the body.
             */
            if (request.content().isReadable())
            {
                // TODO: Optimize? Use shared temp buffer?
                final byte[] bytes = new byte[request.content().readableBytes()];
                request.content().readBytes(bytes);
                final ByteString byteString = ByteString.copyFrom(bytes);
                builder.setContentLength(byteString.size());
                builder.setBody(byteString);
            }
            else
            {
                builder.setContentLength(0);
                builder.setBody(ByteString.EMPTY);
            }

            return builder.build();
        }

    }

    /**
     * Representation of client-server connection.
     */
    private final class Connection
            implements Comparable<Connection>
    {
        public final Instant timeout;

        public final ChannelHandlerContext ctx;

        public final String correlationId;

        private final AtomicBoolean sent = new AtomicBoolean();

        private final AtomicBoolean closed = new AtomicBoolean();

        public Connection (final String correlationId,
                           final Instant timeout,
                           final ChannelHandlerContext context)
        {

            this.correlationId = correlationId;
            this.timeout = timeout;
            this.ctx = context;
        }

        @Override
        public int compareTo (final Connection other)
        {
            return timeout.compareTo(other.timeout);
        }

        public void send (final HTTP.Response encodedResponse)
        {
            /**
             * Sending a response is a one-shot operation.
             * Do not allow duplicate responses.
             */
            if (sent.compareAndSet(false, true) == false)
            {
                return;
            }

            /**
             * Decode the body.
             */
            final ByteBuf body = Unpooled.copiedBuffer(encodedResponse.getBody().asReadOnlyByteBuffer());

            /**
             * Decode the HTTP status code.
             */
            final HttpResponseStatus status = encodedResponse.hasStatus()
                    ? HttpResponseStatus.valueOf(encodedResponse.getStatus())
                    : HttpResponseStatus.OK;

            /**
             * Create the response.
             */
            final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, body);

            /**
             * Decode the headers.
             */
            for (Entry<String, Header> header : encodedResponse.getHeadersMap().entrySet())
            {
                response.headers().add(header.getKey(), header.getValue().getValuesList());
            }

            /**
             * Send the response to the client.
             */
            ctx.writeAndFlush(response);

            /**
             * Close the connection.
             */
            close();
        }

        public void close ()
        {
            /**
             * Release this connection.
             */
            connections.remove(correlationId);

            /**
             * Formally close the connection.
             */
            if (closed.compareAndSet(false, true))
            {
                ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    /**
     * Use this method to begin creating a new web-server instance.
     *
     * @return a builder that can build a web-server.
     */
    public static Builder newWebServer ()
    {
        return new Builder();
    }

    /**
     * Builder of Web Servers.
     */
    public static final class Builder
    {
        private String serverName = "";

        private String replyTo = "";

        private String host = "localhost";

        private int port = 8080;

        private int aggregationCapacity = 65536;

        private Duration responseTimeout = Duration.ofSeconds(60);

        private Builder ()
        {
            // Pass.
        }

        /**
         * Set the reply-to field to embed inside of all requests.
         *
         * @param value will be embedded in all requests.
         * @return this.
         */
        public Builder withReplyTo (final String value)
        {
            this.replyTo = value;
            return this;
        }

        /**
         * Set the human-readable name of the new web-server.
         *
         * @param value will be embedded in all requests.
         * @return this.
         */
        public Builder withServerName (final String value)
        {
            this.serverName = value;
            return this;
        }

        /**
         * Set the maximum capacity of HTTP Chunk Aggregation.
         *
         * @param capacity will limit the size of incoming messages.
         * @return this.
         */
        public Builder withAggregationCapacity (final int capacity)
        {
            Preconditions.checkArgument(capacity >= 0, "capacity < 0");
            this.aggregationCapacity = capacity;
            return this;
        }

        /**
         * Connections will be closed automatically, if a response exceeds this timeout.
         *
         * @param timeout is the maximum amount of time allowed for a response.
         * @return this.
         */
        public Builder setResponseTimeout (final Duration timeout)
        {
            responseTimeout = Objects.requireNonNull(timeout, "timeout");
            return this;
        }

        /**
         * Specify the host that the server will listen on.
         *
         * @param host is a host-name or IP address.
         * @return this.
         */
        public Builder withHost (final String host)
        {
            this.host = Objects.requireNonNull(host, "host");
            return this;
        }

        /**
         * Set the port that the server will listen on.
         *
         * @param value is the port to use.
         * @return this.
         */
        public Builder withPort (final int value)
        {
            this.port = value;
            return this;
        }

        /**
         * Construct the web-server and start it up.
         *
         * @return the new web-server.
         */
        public WebServer build ()
        {
            final WebServer server = new WebServer(this);
            return server;
        }
    }

    /**
     * Start an echo server.
     *
     * @param args
     */
    public static void main (String[] args)
    {
        final int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        final Stage stage = Cascade.newStage();

        final WebServer server = WebServer.newWebServer()
                .withServerName("EchoServer")
                .withReplyTo("N/A")
                .withPort(port)
                .build()
                .start();

        final Mapper<HTTP.Request, HTTP.Response> handler = Mapper.newMapper(stage, WebServer::echo);

        server.requestsOut().connect(handler.dataIn());
        server.responsesIn().connect(handler.dataOut());
    }

    private static HTTP.Response echo (final HTTP.Request req)
    {
        final HTTP.Response.Builder rep = HTTP.Response.newBuilder();

        rep.setRequest(req);
        rep.setContentType("text/plain");
        rep.setBody(ByteString.copyFrom(req.toString().getBytes()));
        rep.setStatus(200);

        return rep.build();
    }
}
