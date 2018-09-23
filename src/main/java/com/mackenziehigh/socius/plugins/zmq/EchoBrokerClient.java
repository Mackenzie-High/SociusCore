package com.mackenziehigh.socius.plugins.zmq;

import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.inception.Kernel;
import com.mackenziehigh.inception.Kernel.KernelApi;
import com.mackenziehigh.inception.Kernel.KernelEvent;
import com.mackenziehigh.inception.Kernel.PublishEvent;
import com.mackenziehigh.inception.Kernel.SubscribeEvent;
import com.mackenziehigh.inception.Kernel.UnpublishEvent;
import com.mackenziehigh.inception.Kernel.UnsubscribeEvent;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

/**
 * TODO: de-duplication.
 * TODO: only subscribe to some channels based on regular-expression/configuration.
 * TODO: shutdown hook.
 * TODO: add stop.
 */
public final class EchoBrokerClient
{
    private static final Logger logger = LogManager.getLogger(EchoBrokerClient.class);

    private final Object lock = new Object();

    /**
     * This is the stage on which the actors herein will reside.
     */
    private final Stage stage;

    /**
     * This actor will listen for subscription changes from the kernel
     * and route those event-messages to type-appropriate handlers.
     */
    private final Actor<KernelEvent, KernelEvent> actorKernelEvt;

    /**
     * This actor will process publisher registrations.
     */
    private final Actor<PublishEvent, PublishEvent> actorPubEvt;

    /**
     * This actor will process publisher deregistrations.
     */
    private final Actor<UnpublishEvent, UnpublishEvent> actorUnpubEvt;

    /**
     * This actor will process subscriber registrations.
     */
    private final Actor<SubscribeEvent, SubscribeEvent> actorSubEvt;

    /**
     * This actor will process subscriber deregistrations.
     */
    private final Actor<UnsubscribeEvent, UnsubscribeEvent> actorUnsubEvt;

    /**
     * This object facilitates the creation of the ZeroMQ sockets.
     */
    private final ZContext context = new ZContext(1);

    /**
     * This socket is used, exclusively, to send messages to the broker.
     */
    private final Socket socketOut = context.createSocket(ZMQ.PUB);

    /**
     * This socket is used, exclusively, to receive messages from the broker.
     */
    private final Socket socketIn = context.createSocket(ZMQ.SUB);

    /**
     * These objects correspond to the named-channels whose
     * messages will be forwarded to the broker.
     *
     * <p>
     * The key is the name of a channel.
     * </p>
     */
    private final Map<String, InputChannel> inputChannels = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * These objects correspond to the named-channels whose
     * messages will be forwarded from the broker.
     *
     * <p>
     * The key is the name of a channel.
     * </p>
     */
    private final Map<String, OutputChannel> outputChannels = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * These objects correspond to the named-channels whose
     * messages will be forwarded from the broker.
     *
     * <p>
     * The key is the non-cryptographic MD5 of the channel-name in upper-case.
     * </p>
     */
    private final Map<byte[], OutputChannel> dispatchTable = new ConcurrentSkipListMap<>(EchoBrokerClient::compareByteArrays);

    private final AtomicBoolean started = new AtomicBoolean();

    private final AtomicBoolean stop = new AtomicBoolean();

    /**
     * Constructor.
     *
     * @param kapi provides access to the inception kernel.
     */
    public EchoBrokerClient (final KernelApi kapi)
    {
        this(kapi.stage());
        kapi.events().connect(actorKernelEvt.input());
    }

    /**
     * Constructor.
     *
     * @param stage will be used to create private actors.
     */
    public EchoBrokerClient (final Stage stage)
    {
        this.stage = stage;
        actorKernelEvt = stage.newActor().withScript(this::onKernelEventMsg).create();
        actorPubEvt = stage.newActor().withScript(this::onPublishEventMsg).create();
        actorUnpubEvt = stage.newActor().withScript(this::onUnpublishEventMsg).create();
        actorSubEvt = stage.newActor().withScript(this::onSubscribeEventMsg).create();
        actorUnsubEvt = stage.newActor().withScript(this::onUnsubscribeEventMsg).create();
    }

    private void onKernelEventMsg (final KernelEvent event)
    {
        if (event instanceof PublishEvent)
        {
            actorPubEvt.accept((PublishEvent) event);
        }
        else if (event instanceof UnpublishEvent)
        {
            actorUnpubEvt.accept((UnpublishEvent) event);
        }
        else if (event instanceof SubscribeEvent)
        {
            actorSubEvt.accept((SubscribeEvent) event);
        }
        else if (event instanceof UnsubscribeEvent)
        {
            actorUnsubEvt.accept((UnsubscribeEvent) event);
        }
    }

    @SuppressWarnings ("unchecked")
    private void onPublishEventMsg (final Kernel.PublishEvent event)
    {
        final String channelName = event.channel();

        /**
         * Synchronize to protect the maps.
         */
        synchronized (lock)
        {
            /**
             * If this is the first publisher registration for this channel,
             * then we must create an entry in the map.
             */
            if (inputChannels.containsKey(channelName) == false)
            {
                inputChannels.put(channelName, new InputChannel(channelName));
            }

            logger.info("PublishEvent for Channel ({}).", channelName);

            /**
             * Create a connection to the publisher that will route
             * messages from the publisher to the broker up-link.
             */
            final InputChannel channel = inputChannels.get(channelName);
            channel.connections.add(event.publisher().output());
            final Actor actor = event.publisher();
            actor.output().connect(channel.uplink);
        }
    }

    @SuppressWarnings ("unchecked")
    private void onUnpublishEventMsg (final Kernel.UnpublishEvent event)
    {
        final String channelName = event.channel();

        /**
         * Synchronize to protect the maps.
         */
        synchronized (lock)
        {
            /**
             * If the channel is not being monitored,
             * then we do not need to stop monitoring.
             */
            if (inputChannels.containsKey(channelName) == false)
            {
                return;
            }

            logger.info("UnpublishEvent for Channel ({}).", channelName);

            /**
             * Disconnect the connection that routes messages
             * from the publisher(s) to the broker up-link.
             */
            final InputChannel channel = inputChannels.get(channelName);
            channel.connections.remove(event.publisher().output());
            final Actor actor = event.publisher();
            actor.output().disconnect(channel.uplink);

            /**
             * If no more publishers are publishing to the channel,
             * then we do not need to monitor the channel at all.
             */
            if (channel.connections.isEmpty())
            {
                inputChannels.remove(channelName);
            }
        }
    }

    @SuppressWarnings ("unchecked")
    private void onSubscribeEventMsg (final Kernel.SubscribeEvent event)
    {
        final String channelName = event.channel();

        /**
         * Synchronize to protect the map.
         */
        synchronized (lock)
        {
            /**
             * If this is the first subscriber registration for the channel,
             * then we must create an entry in the map.
             */
            if (outputChannels.containsKey(channelName) == false)
            {
                outputChannels.put(channelName, new OutputChannel(channelName));
            }

            logger.info("SubscribeEvent for Channel ({}).", channelName);

            /**
             * Create a connection to the subscriber that will route
             * messages from the broker down-link to subscriber.
             */
            final OutputChannel channel = outputChannels.get(channelName);
            channel.connections.add(event.subscriber().output());
            final Actor actor = event.subscriber();
            actor.input().connect(channel.downlink);

            /**
             * The dispatch-table will be used to efficiently route
             * messages from the down-link to the connection.
             */
            dispatchTable.put(channel.channelHash, channel);

            /**
             * Subscribe to the channel, at the ZMQ level,
             * so that the broker will send us the messages.
             */
            socketIn.subscribe(channel.channelHash);
        }
    }

    @SuppressWarnings ("unchecked")
    private void onUnsubscribeEventMsg (final Kernel.UnsubscribeEvent event)
    {
        final String channelName = event.channel();

        /**
         * Synchronize to protect the maps.
         */
        synchronized (lock)
        {
            /**
             * If the channel is not being monitored,
             * then we do not need to stop monitoring.
             */
            if (outputChannels.containsKey(channelName) == false)
            {
                return;
            }

            logger.info("UnsubscribeEvent for Channel ({}).", channelName);

            /**
             * Disconnect the connection that routes messages
             * from the broker down-link to the subscriber(s).
             */
            final OutputChannel channel = outputChannels.get(channelName);
            channel.connections.remove(event.subscriber().output());
            final Actor actor = event.subscriber();
            actor.input().disconnect(channel.downlink);

            /**
             * If no more subscribers are subscribed to the channel,
             * then we do not need to monitor the channel at all.
             */
            if (channel.connections.isEmpty())
            {
                /**
                 * Cause the broker to stop sending messages for the channel.
                 */
                socketIn.unsubscribe(channel.channelHash);

                /**
                 * Internal clean-up.
                 */
                dispatchTable.remove(channel.channelHash);
                inputChannels.remove(channelName);
            }
        }
    }

    /**
     * Cause messages from the given actor to be forwarded
     * to the broker via the named channel.
     *
     * @param actor will provide messages.
     * @param channel is used to identify the stream of messages.
     * @return this.
     */
    public EchoBrokerClient publish (final Actor<?, ?> actor,
                                     final String channel)
    {
        final PublishEvent event = new PublishEvent()
        {
            @Override
            public String channel ()
            {
                return channel;
            }

            @Override
            public Actor<?, ?> publisher ()
            {
                return actor;
            }
        };

        actorKernelEvt.accept(event);

        return this;
    }

    /**
     * Cause messages from the named channel to be
     * forwarded from the broker to the given actor.
     *
     * @param actor will receive messages.
     * @param channel is used to identify the stream of messages.
     * @return this.
     */
    public EchoBrokerClient subscribe (final Actor<?, ?> actor,
                                       final String channel)
    {
        final SubscribeEvent event = new SubscribeEvent()
        {
            @Override
            public String channel ()
            {
                return channel;
            }

            @Override
            public Actor<?, ?> subscriber ()
            {
                return actor;
            }
        };

        actorKernelEvt.accept(event);

        return this;
    }

    /**
     * Cause messages from the given actor to no longer
     * be forwarded to the broker via the named channel.
     *
     * @param actor will no longer provide messages.
     * @param channel is used to identify the stream of messages.
     * @return this.
     */
    public EchoBrokerClient unpublish (final Actor<?, ?> actor,
                                       final String channel)
    {
        final UnpublishEvent event = new UnpublishEvent()
        {
            @Override
            public String channel ()
            {
                return channel;
            }

            @Override
            public Actor<?, ?> publisher ()
            {
                return actor;
            }
        };

        actorKernelEvt.accept(event);

        return this;
    }

    /**
     * Cause messages from the named channel to no longer
     * be forwarded from the broker to the given actor.
     *
     * @param actor will no longer receive messages.
     * @param channel is used to identify the stream of messages.
     * @return this.
     */
    public EchoBrokerClient unsubscribe (final Actor<?, ?> actor,
                                         final String channel)
    {
        final UnsubscribeEvent event = new UnsubscribeEvent()
        {
            @Override
            public String channel ()
            {
                return channel;
            }

            @Override
            public Actor<?, ?> subscriber ()
            {
                return actor;
            }
        };

        actorKernelEvt.accept(event);

        return this;
    }

    public EchoBrokerClient connectTo (final String host,
                                       final int uplinkPort,
                                       final int downlinkPort)
    {
        socketOut.connect(String.format("tcp://%s:%d", host, uplinkPort));
        socketIn.connect(String.format("tcp://%s:%d", host, downlinkPort));
        return this;
    }

    /**
     * Cause the client to connect to the broker and begin routing messages.
     *
     * @return this.
     */
    public EchoBrokerClient start ()
    {
        if (started.compareAndSet(false, true))
        {
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

            final Thread thread = new Thread(() -> run());
            thread.setDaemon(true);
            thread.start();
        }

        return this;
    }

    public EchoBrokerClient stop ()
    {
        if (stop.compareAndSet(false, true))
        {
            context.close();
        }

        return this;
    }

    /**
     * Receive messages from the broker and then forward
     * the messages to the appropriate subscribers.
     */
    private void run ()
    {
        while (true)
        {
            final byte[] message = socketIn.recv();

            if (message.length >= 13)
            {
                recvDataMsg(message);
            }
        }
    }

    private void recvHeartbeatMsg (final ByteBuffer buffer)
    {

    }

    private void recvDataMsg (final byte[] message)
    {
        final byte[] hash = Arrays.copyOfRange(message, 0, 16);
        final byte[] data = Arrays.copyOfRange(message, 21, message.length);

        // TODO: The lookup is not thread-safe!
        final OutputChannel channel = dispatchTable.get(hash);

        if (channel != null)
        {
            final String messageOut = new String(data);
            channel.input.send(messageOut);
        }
    }

    private static int compareByteArrays (final byte[] left,
                                          final byte[] right)
    {
        if (Integer.compare(left.length, right.length) != 0)
        {
            return Integer.compare(left.length, right.length);
        }

        for (int i = 0; i < left.length; i++)
        {
            if (Integer.compare(left[i], right[i]) != 0)
            {
                return Integer.compare(left[i], right[i]);
            }
        }

        return 0;
    }

    private final class InputChannel
    {
        public final String channel;

        public final byte[] channelHash;

        public final Set<Output<?>> connections = Sets.newConcurrentHashSet();

        public final Input<Object> uplink;

        public InputChannel (final String channel)
        {
            this.channel = channel;
            this.channelHash = Hashing.md5().hashString(channel, StandardCharsets.UTF_8).asBytes();
            this.uplink = stage.newActor().withScript(this::onDataMessage).create().input();
        }

        private Object onDataMessage (final Object message)
        {
            final byte[] data = message.toString().getBytes();
            final ByteBuffer buffer = ByteBuffer.allocate(1000);
            buffer.put(channelHash);
            buffer.putInt(0);
            buffer.put((byte) 0);
            buffer.put(data);

            final byte[] array = new byte[16 + 4 + 1 + data.length];

            buffer.position(0);
            buffer.get(array);

            socketOut.send(array);

            return message;
        }
    }

    private final class OutputChannel
    {
        public final String channel;

        public final byte[] channelHash;

        public final Set<Output<?>> connections = Sets.newConcurrentHashSet();

        public final Input<Object> input;

        public final Output<Object> downlink;

        public OutputChannel (final String channel)
        {
            this.channel = channel;
            this.channelHash = Hashing.md5().hashString(channel, StandardCharsets.UTF_8).asBytes();
            final Actor<Object, Object> actor = stage.newActor().withScript(this::onDataMessage).create();
            this.input = actor.input();
            this.downlink = actor.output();
        }

        private Object onDataMessage (final Object message)
        {

            return message;
        }
    }
}
