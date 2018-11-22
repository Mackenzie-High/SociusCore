package com.mackenziehigh.socius;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
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
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
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
    private final WebServer server = this;

    /**
     * HTTP Request.
     */
    public final class HttpRequest
            implements Serializable
    {

        private final String correlationId;

        private final long sequenceNumber;

        private final long timestamp;

        private final HttpProtocol protocol;

        private final String method;

        private final String uri;

        private final String path;

        private final String rawPath;

        private final String rawQuery;

        private final Map<String, HttpQueryParameter> parameters;

        private final Map<String, HttpHeader> headers;

        private final String host;

        private final List<HttpCookie> cookies;

        private final String contentType;

        private final int contentLength;

        private final ByteSource body;

        private HttpRequest (final FullHttpRequest request)
                throws URISyntaxException
        {
            this.sequenceNumber = server.seqnum.incrementAndGet();
            this.correlationId = UUID.randomUUID().toString();
            this.timestamp = System.currentTimeMillis();
            this.method = request.method().name();

            final String protocolName = request.protocolVersion().protocolName();
            final String protocolText = request.protocolVersion().text();
            final int protocolMajor = request.protocolVersion().majorVersion();
            final int protocolMinor = request.protocolVersion().minorVersion();
            this.protocol = new HttpProtocol(protocolName, protocolText, protocolMajor, protocolMinor);

            /**
             * Encode the URL.
             */
            final QueryStringDecoder qsDecoder = new QueryStringDecoder(request.uri());
            this.uri = qsDecoder.uri();
            this.path = qsDecoder.path();
            this.rawPath = qsDecoder.rawPath();
            this.rawQuery = qsDecoder.rawQuery();

            /**
             * Encode Query Parameters.
             */
            final Map<String, HttpQueryParameter> params = Maps.newTreeMap();
            for (Entry<String, List<String>> entry : qsDecoder.parameters().entrySet())
            {
                final HttpQueryParameter param = new HttpQueryParameter(entry.getKey(), entry.getValue());
                params.put(param.key(), param);
            }
            this.parameters = ImmutableMap.copyOf(params);

            /**
             * Encode Host.
             */
            if (request.headers().contains(Names.HOST))
            {
                host = request.headers().get(Names.HOST);
            }
            else
            {
                // TODO
                host = null;
            }

            /**
             * Encode Content Type.
             */
            if (request.headers().contains(Names.CONTENT_TYPE))
            {
                contentType = request.headers().get(Names.CONTENT_TYPE);
            }
            else
            {
                // TODO
                contentType = null;
            }

            /**
             * Encode the headers.
             */
            final Map<String, HttpHeader> heads = Maps.newTreeMap();
            for (String name : request.headers().names())
            {
                final HttpHeader header = new HttpHeader(name, request.headers().getAll(name));
                heads.put(name, header);
            }
            headers = ImmutableMap.copyOf(heads);

            /**
             * Encode the cookies.
             */
            final List<HttpCookie> httpCookies = Lists.newArrayList();
            for (String header : request.headers().getAll(Names.COOKIE))
            {
                for (Cookie cookie : ServerCookieDecoder.STRICT.decode(header))
                {
                    final HttpCookie.Builder cookieBuilder = HttpCookie.newBuilder();

                    cookieBuilder.withDomain(cookie.domain());
                    cookieBuilder.withHttpOnly(cookie.isHttpOnly());
                    cookieBuilder.withSecure(cookie.isSecure());
                    cookieBuilder.withPath(cookie.path());
                    cookieBuilder.withMaxAge(cookie.maxAge());

                    httpCookies.add(cookieBuilder.build());
                }
            }
            cookies = httpCookies;

            /**
             * Encode the body.
             */
            if (request.content().isReadable())
            {
                // TODO: Optimize? Use shared temp buffer?
                contentLength = request.content().readableBytes();
                final byte[] bytes = new byte[contentLength];
                request.content().readBytes(bytes);
                body = ByteSource.wrap(bytes);
            }
            else
            {
                contentLength = 0;
                body = ByteSource.empty();
            }
        }

        /**
         * Human readable name of the server receiving the request.
         *
         * @return the property.
         */
        public String serverName ()
        {
            return serverName;
        }

        /**
         * Universally-Unique-Identifier of the server receiving the request.
         *
         * <p>
         * This value is unique to a server, per lifetime.
         * If the server restarts, then it will use a different UUID.
         * </p>
         *
         * @return the property.
         */
        public String serverId ()
        {
            return serverId;
        }

        /**
         * Reserved For Application Use.
         *
         * @return the property.
         */
        public String replyTo ()
        {
            return replyTo;
        }

        /**
         * Universally-Unique-Identifier of a single request.
         *
         * @return the property.
         */
        public String correlationId ()
        {
            return correlationId;
        }

        /**
         * Sequence number of requests coming from this server.
         *
         * @return the property.
         */
        public long sequenceNumber ()
        {
            return sequenceNumber;
        }

        /**
         * A Java-compatible timestamp indicating when the request was received.
         *
         * @return the property.
         */
        public long timestamp ()
        {
            return timestamp;
        }

        /**
         * Request Protocol.
         *
         * @return the property.
         */
        public HttpProtocol protocol ()
        {
            return protocol;
        }

        /**
         * HTTP Verb of the request (GET/POST/etc).
         *
         * @return the property.
         */
        public String method ()
        {
            return method;
        }

        /**
         * Requested URI.
         *
         * @return the property.
         */
        public String uri ()
        {
            return uri;
        }

        /**
         * Requested Path of URI.
         *
         * @return the property.
         */
        public String path ()
        {
            return path;
        }

        /**
         * Raw Requested Path of URI.
         *
         * @return the property.
         */
        public String rawPath ()
        {
            return rawPath;
        }

        /**
         * Raw Query String.
         *
         * @return the property.
         */
        public String rawQuery ()
        {
            return rawQuery;
        }

        /**
         * Query Parameters.
         *
         * @return the property.
         */
        public Map<String, HttpQueryParameter> parameters ()
        {
            return parameters;
        }

        /**
         * Raw HTTP Headers.
         *
         * @return the property.
         */
        public Map<String, HttpHeader> headers ()
        {
            return headers;
        }

        /**
         * Client Host.
         *
         * @return the property.
         */
        public String host ()
        {
            return host;
        }

        /**
         * HTTP cookies from the client.
         *
         * @return the property.
         */
        public List<HttpCookie> cookies ()
        {
            return cookies;
        }

        /**
         * MIME Type of the body.
         *
         * @return the property.
         */
        public String contentType ()
        {
            return contentType;
        }

        /**
         * Length of the body.
         *
         * @return the property.
         */
        public int contentLength ()
        {
            return contentLength;
        }

        /**
         * Body of the request.
         *
         * @return the property.
         */
        public ByteSource body ()
        {
            return body;
        }

    }

    /**
     * HTTP Response.
     */
    public static final class HttpResponse
    {

        private final Optional<HttpRequest> request;

        private final String correlationId;

        private final long timestamp;

        private final int status;

        private final Map<String, HttpHeader> headers;

        private final List<HttpCookie> cookies;

        private final String contentType;

        private final ByteSource body;

        private HttpResponse (final Builder builder)
        {
            this.request = Optional.ofNullable(builder.request);
            this.correlationId = builder.correlationId == null && builder.request != null ? builder.request.correlationId : builder.correlationId;
            this.timestamp = builder.timestamp;
            this.status = builder.status;
            this.headers = ImmutableMap.copyOf(builder.headers);
            this.cookies = ImmutableList.copyOf(builder.cookies);
            this.contentType = builder.contentType;
            this.body = builder.body;
        }

        /**
         * The HTTP request that precipitated this response.
         *
         * @return the property.
         */
        public Optional<HttpRequest> request ()
        {
            return request;
        }

        /**
         * Universally-Unique-Identifier both the request and the response.
         *
         * @return the property.
         */
        public String correlationId ()
        {
            return correlationId;
        }

        /**
         * When this response was created.
         *
         * @return the property.
         */
        public long timestamp ()
        {
            return timestamp;
        }

        /**
         * HTTP Response Code.
         *
         * @return the property.
         */
        public int status ()
        {
            return status;
        }

        /**
         * Raw HTTP Headers.
         *
         * @return the property.
         */
        public Map<String, HttpHeader> headers ()
        {
            return headers;
        }

        /**
         * HTTP cookies from the server.
         *
         * @return the property.
         */
        public List<HttpCookie> cookies ()
        {
            return cookies;
        }

        /**
         * MIME type of the body.
         *
         * @return the property.
         */
        public String contentType ()
        {
            return contentType;
        }

        /**
         * Body of the response.
         *
         * @return the property.
         */
        public ByteSource body ()
        {
            return body;
        }

        public static Builder newBuilder ()
        {
            return new Builder();
        }

        public static final class Builder
        {
            private HttpRequest request;

            private String correlationId;

            private long timestamp;

            private int status;

            private final Map<String, HttpHeader> headers = Maps.newTreeMap();

            private List<HttpCookie> cookies = Lists.newArrayList();

            private String contentType;

            private ByteSource body = ByteSource.empty();

            private Builder ()
            {
                // Pass.
            }

            /**
             * Specify the value of the (Request) property.
             *
             * @param value will be assigned to the property.
             * @return this.
             */
            public Builder withRequest (final HttpRequest value)
            {
                this.request = value;
                return this;
            }

            /**
             * Specify the value of the (Correlation-ID) property.
             *
             * @param value will be assigned to the property.
             * @return this.
             */
            public Builder withCorrelationId (final String value)
            {
                this.correlationId = value;
                return this;
            }

            /**
             * Specify the value of the (Timestamp) property.
             *
             * @param value will be assigned to the property.
             * @return this.
             */
            public Builder withTimestamp (final long value)
            {
                this.timestamp = value;
                return this;
            }

            /**
             * Specify the value of the (Status) property.
             *
             * @param value will be assigned to the property.
             * @return this.
             */
            public Builder withStatus (final int value)
            {
                this.status = value;
                return this;
            }

            /**
             * Specify an additional HTTP Header.
             *
             * @param key identifies the header.
             * @param values will be contained within the header.
             * @return this.
             */
            public Builder withHeader (final String key,
                                       final String... values)
            {

                return this;
            }

            /**
             * Specify an additional HTTP Cookie.
             *
             * @param value describes the cookie.
             * @return this.
             */
            public Builder withCookie (final HttpCookie value)
            {
                return this;
            }

            /**
             * Specify the value of the (Content-Type) property.
             *
             * @param value will be assigned to the property.
             * @return this.
             */
            public Builder withContentType (final String value)
            {
                this.contentType = value;
                return this;
            }

            /**
             * Specify the value of the (Body) property.
             *
             * @param value will be assigned to the property.
             * @return this.
             */
            public Builder withBody (final ByteSource value)
            {
                body = Objects.requireNonNull(value, "value");
                return this;
            }

            /**
             * Construct the response.
             *
             * @return the response.
             */
            public HttpResponse build ()
            {
                return new HttpResponse(this);
            }
        }
    }

    /**
     * HTTP Protocol.
     *
     * <p>
     * See Also: https://netty.io/4.0/api/io/netty/handler/codec/http/HttpVersion.html
     * </p>
     */
    public static final class HttpProtocol
    {
        private final String name;

        private final String text;

        private final int major;

        private final int minor;

        private HttpProtocol (final String name,
                              final String text,
                              final int major,
                              final int minor)
        {
            this.name = Objects.requireNonNull(name, "name");
            this.text = Objects.requireNonNull(text, "text");
            this.major = major;
            this.minor = minor;
        }

        public String name ()
        {
            return name;
        }

        public String text ()
        {
            return text;
        }

        public int majorVersion ()
        {
            return major;
        }

        public int minorVersion ()
        {
            return minor;
        }
    }

    /**
     * HTTP Query Parameter.
     */
    public final class HttpQueryParameter
    {
        private final String key;

        private final List<String> values;

        private HttpQueryParameter (final String key,
                                    final List<String> values)
        {
            this.key = Objects.requireNonNull(key, "key");
            this.values = ImmutableList.copyOf(values);
        }

        public String key ()
        {
            return key;
        }

        public List<String> values ()
        {
            return values;
        }
    }

    /**
     * HTTP Header.
     */
    public final class HttpHeader
    {
        private final String key;

        private final List<String> values;

        private HttpHeader (final String key,
                            final List<String> values)
        {
            this.key = Objects.requireNonNull(key, "key");
            this.values = ImmutableList.copyOf(values);
        }

        public String key ()
        {
            return key;
        }

        public List<String> values ()
        {
            return values;
        }
    }

    /**
     * HTTP Cookie.
     *
     * <p>
     * See Also: https://netty.io/4.1/api/io/netty/handler/codec/http/cookie/Cookie.html
     * </p>
     */
    public static final class HttpCookie
    {
        private final String domain;

        private final boolean httpOnly;

        private final long maxAge;

        private final String path;

        private final boolean secure;

        private final String value;

        private final String wrap;

        private HttpCookie (final Builder builder)
        {
            this.domain = builder.domain;
            this.httpOnly = builder.httpOnly;
            this.maxAge = builder.maxAge;
            this.path = builder.path;
            this.secure = builder.secure;
            this.value = builder.value;
            this.wrap = builder.wrap;
        }

        public String domain ()
        {
            return domain;
        }

        public boolean httpOnly ()
        {
            return httpOnly;
        }

        public long maxAge ()
        {
            return maxAge;
        }

        public String path ()
        {
            return path;
        }

        public boolean secure ()
        {
            return secure;
        }

        public String value ()
        {
            return value;
        }

        public String wrap ()
        {
            return wrap;
        }

        public static Builder newBuilder ()
        {
            return new Builder();
        }

        public static final class Builder
        {
            private String domain;

            private boolean httpOnly;

            private long maxAge;

            private String path;

            private boolean secure;

            private String value;

            private String wrap;

            private Builder ()
            {
                // Pass.
            }

            public Builder withDomain (final String value)
            {
                this.domain = value;
                return this;
            }

            public Builder withHttpOnly (final boolean value)
            {
                this.httpOnly = value;
                return this;
            }

            public Builder withMaxAge (final long value)
            {
                this.maxAge = value;
                return this;
            }

            public Builder withPath (final String value)
            {
                this.path = value;
                return this;
            }

            public Builder withSecure (final boolean value)
            {
                this.secure = value;
                return this;
            }

            public Builder value (final String value)
            {
                this.value = value;
                return this;
            }

            public Builder wrap (final String value)
            {
                this.wrap = value;
                return this;
            }

            public HttpCookie build ()
            {
                return new HttpCookie(this);
            }
        }
    }

    private static final Logger logger = LogManager.getLogger(WebServer.class);

    /**
     * This counter is used to assign sequence-numbers to requests.
     */
    private final AtomicLong seqnum = new AtomicLong();

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
    private final Processor<HttpRequest> requestsOut = Processor.newProcessor(stage);

    /**
     * This processor will receive the HTTP responses from the external actors
     * and then will route those responses to the originating connection.
     */
    private final Processor<HttpResponse> responsesIn = Processor.newProcessor(stage, this::onResponse);

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
    public Output<HttpRequest> requestsOut ()
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
    public Input<HttpResponse> responsesIn ()
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

    private void onResponse (final HttpResponse response)
            throws IOException
    {
        routeResponse(response);
    }

    /**
     * This actor receives the HTTP Responses from the external handlers connected to this server,
     * routes them to the appropriate client-server connection, and then transmits them via Netty.
     *
     * @param response needs to be send to a client.
     */
    private void routeResponse (final HttpResponse response)
            throws IOException
    {
        /**
         * Get the Correlation-ID that allows us to map responses to requests.
         * Whoever created the response should have included the Correlation-ID.
         * If they did not, they may have implicitly included it, by including the request itself.
         */
        final String correlationId = response.correlationId();

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
                final HttpRequest encodedRequest = new HttpRequest((FullHttpRequest) msg);
                final String correlationId = encodedRequest.correlationId();

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

        public void send (final HttpResponse encodedResponse)
                throws IOException
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
            final ByteBuf body = Unpooled.copiedBuffer(encodedResponse.body().read());

            /**
             * Decode the HTTP status code.
             */
            final HttpResponseStatus status = HttpResponseStatus.valueOf(encodedResponse.status());

            /**
             * Create the response.
             */
            final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, body);

            /**
             * Decode the headers.
             */
            for (Entry<String, HttpHeader> header : encodedResponse.headers().entrySet())
            {
                response.headers().add(header.getKey(), header.getValue().values());
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

        final Mapper<HttpRequest, HttpResponse> handler = Mapper.newMapper(stage, WebServer::echo);

        server.requestsOut().connect(handler.dataIn());
        server.responsesIn().connect(handler.dataOut());
    }

    private static HttpResponse echo (final HttpRequest req)
    {
        final HttpResponse.Builder rep = HttpResponse.newBuilder();

        rep.withRequest(req);
        rep.withContentType("text/plain");
        rep.withBody(ByteSource.wrap(req.toString().getBytes()));
        rep.withStatus(200);

        return rep.build();
    }
}
