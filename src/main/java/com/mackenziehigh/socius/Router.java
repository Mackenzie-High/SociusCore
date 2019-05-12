package com.mackenziehigh.socius;

import com.mackenziehigh.socius.Processor;
import com.google.common.collect.ImmutableList;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Provides a publish/subscribe mechanism.
 *
 * <p>
 * A router contains zero-or-more independent end-points.
 * Each end-point registers to send or receive messages.
 * </p>
 *
 * <p>
 * An output is provided that serves as a default sink,
 * which will receive all messages sent through the router,
 * regardless of where the messages were sent from.
 * </p>
 *
 * <p>
 * Another output is provided that serves as a default sink,
 * which will receive all messages that are sent through the router,
 * but for which no subscriber could be found.
 * </p>
 *
 * @param <T> is the type of the incoming and outgoing messages.
 */
public final class Router<T>
{
    private final Stage stage;

    private final ConcurrentMap<Object, ImmutableList<EndPoint<T>>> routeMap = new ConcurrentHashMap<>();

    /**
     * Provides the sink-all output connector.
     */
    private final Processor<T> sinkAll;

    /**
     * Provides the sink-dead output connector.
     */
    private final Processor<T> sinkDead;

    private Router (final Stage stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.sinkAll = Processor.fromIdentityScript(stage);
        this.sinkDead = Processor.fromIdentityScript(stage);
    }

    public static <T> Router<T> newRouter (final Stage stage)
    {
        return new Router<>(stage);
    }

    public EndPoint<T> newEndPoint ()
    {
        return new EndPoint(this);
    }

    /**
     * Output Connection.
     *
     * @return the output that unconditionally receives all messages.
     */
    public Output<T> sinkAll ()
    {
        return sinkAll.dataOut();
    }

    /**
     * Output Connection.
     *
     * <p>
     * This output is particularly useful during debugging,
     * when messages are being sent to the router,
     * but are <i>seemingly</i> not being forwarded therefrom.
     * </p>
     *
     * @return the output that receives messages that could not be routed.
     */
    public Output<T> sinkDead ()
    {
        return sinkDead.dataOut();
    }

    /**
     * Send a single message to all interested subscribers.
     *
     * <p>
     * Warning: This method may be invoked by multiple actors at once.
     * This method is *not* an actor itself!
     * </p>
     *
     * @param keys
     * @param message
     */
    private void send (final ImmutableList<Object> keys,
                       final T message)
    {
        sinkAll.accept(message);

        boolean none = true;

        for (int i = 0; i < keys.size(); i++)
        {
            final Object key = keys.get(i);

            final ImmutableList<EndPoint<T>> routeList = routeMap.get(key);

            for (int k = 0; k < routeList.size(); k++)
            {
                routeList.get(k).connectorOut.accept(message);
                none = false;
            }
        }

        if (none)
        {
            sinkDead.accept(message);
        }
    }

    private synchronized void subscribe (final Object key,
                                         final EndPoint<T> route)
    {
        routeMap.putIfAbsent(key, ImmutableList.of());
        final Set<EndPoint<T>> copy = new HashSet<>(routeMap.get(key));
        copy.add(route);
        routeMap.put(key, ImmutableList.copyOf(copy));
    }

    private synchronized void unsubscribe (final Object key,
                                           final EndPoint<T> route)
    {
        routeMap.putIfAbsent(key, ImmutableList.of());
        final Set<EndPoint<T>> copy = new HashSet<>(routeMap.get(key));
        copy.add(route);
        routeMap.put(key, ImmutableList.copyOf(copy));
    }

    /**
     * An end-point connected to zero-or-more publishers and subscribers,
     * which are interested in sending and receiving the same messages.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     */
    public static final class EndPoint<T>
            implements Sink<T>,
                       Source<T>
    {
        private final Router<T> router;

        private final Processor<T> connectorIn;

        private final Processor<T> connectorOut;

        private final Set<Object> subscriptionsSet = new HashSet<>();

        private volatile ImmutableList<Object> subscriptionsList = ImmutableList.of();

        private EndPoint (final Router<T> container)
        {
            router = container;
            connectorIn = Processor.fromConsumerScript(router.stage, msg -> router.send(subscriptionsList, msg));
            connectorOut = Processor.fromIdentityScript(router.stage);
        }

        /**
         *
         * <p>
         * This method is a no-op, if the subscription already exists.
         * </p>
         *
         * @param key
         * @return
         */
        public synchronized EndPoint<T> subscribe (final Object key)
        {
            subscriptionsSet.add(key);
            subscriptionsList = ImmutableList.copyOf(subscriptionsSet);
            router.subscribe(key, this);
            return this;
        }

        /**
         *
         * * <p>
         * This method is a no-op, if the subscription does not exist.
         * </p>
         *
         * @param key
         * @return
         */
        public synchronized EndPoint<T> unsubscribe (final Object key)
        {
            subscriptionsSet.remove(key);
            subscriptionsList = ImmutableList.copyOf(subscriptionsSet);
            router.unsubscribe(key, this);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Input<T> dataIn ()
        {
            return connectorIn.dataIn();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Output<T> dataOut ()
        {
            return connectorOut.dataOut();
        }
    }
}
