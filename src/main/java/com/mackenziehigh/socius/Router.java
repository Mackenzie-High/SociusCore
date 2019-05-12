package com.mackenziehigh.socius;

import com.google.common.collect.ImmutableList;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class Router<T>
{
    private final Stage stage;

    private final ConcurrentMap<Object, ImmutableList<Subscriber<T>>> routeMap = new ConcurrentHashMap<>();

    /**
     * Provides the sink-all output connector.
     */
    private final Processor<T> sinkAll;

    /**
     * Provides the sink-dead output connector.
     */
    private final Processor<T> sinkDead;

    private volatile boolean sync = false;

    private Router (final Cascade.Stage stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.sinkAll = Processor.fromIdentityScript(stage);
        this.sinkDead = Processor.fromIdentityScript(stage);
    }

    public static <T> Router<T> newRouter (final Cascade.Stage stage)
    {
        return new Router<>(stage);
    }

    public Publisher<T> newPublisher (final String key)
    {
        return new Publisher(this, key);
    }

    public Subscriber<T> newSubscriber (final String key)
    {
        return new Subscriber(this, key);
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
    private void send (final Object key,
                       final T message)
    {
        if (sync)
        {
            synchronized (this)
            {
                sendImp(key, message);
            }
        }
        else
        {
            sendImp(key, message);
        }
    }

    private void sendImp (final Object key,
                          final T message)
    {
        sinkAll.accept(message);

        boolean none = true;

        final ImmutableList<Subscriber<T>> routeList = routeMap.get(key);

        for (int k = 0; k < routeList.size(); k++)
        {
            routeList.get(k).connector.accept(message);
            none = false;
        }

        if (none)
        {
            sinkDead.accept(message);
        }
    }

    private synchronized void subscribe (final Object key,
                                         final Subscriber<T> route)
    {
        routeMap.putIfAbsent(key, ImmutableList.of());
        final Set<Subscriber<T>> copy = new HashSet<>(routeMap.get(key));
        copy.add(route);
        routeMap.put(key, ImmutableList.copyOf(copy));
    }

    private synchronized void unsubscribe (final Object key,
                                           final Subscriber<T> route)
    {
        routeMap.putIfAbsent(key, ImmutableList.of());
        final Set<Subscriber<T>> copy = new HashSet<>(routeMap.get(key));
        copy.add(route);
        routeMap.put(key, ImmutableList.copyOf(copy));
    }

    /**
     * An end-point connected to zero-or-more publishers and subscribers,
     * which are interested in sending and receiving the same messages.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     */
    public static final class Publisher<T>
            implements Sink<T>
    {
        private final Router<T> router;

        private final String key;

        private final Processor<T> connector;

        private final AtomicBoolean active = new AtomicBoolean();

        private Publisher (final Router<T> router,
                           final String key)
        {
            this.router = router;
            this.key = Objects.requireNonNull(key, "key");
            this.connector = Processor.fromConsumerScript(router.stage, this::onMessage);
        }

        private void onMessage (final T message)
        {
            if (active.get())
            {
                router.send(key, message);
            }
        }

        public Publisher<T> activate ()
        {
            active.set(true);
            return this;
        }

        public Publisher<T> deactivate ()
        {
            active.set(false);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Input<T> dataIn ()
        {
            return connector.dataIn();
        }
    }

    /**
     * An end-point connected to zero-or-more publishers and subscribers,
     * which are interested in sending and receiving the same messages.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     */
    public static final class Subscriber<T>
            implements Source<T>
    {
        private final Router<T> router;

        private final String key;

        private final Processor<T> connector;

        private final AtomicBoolean active = new AtomicBoolean();

        private Subscriber (final Router<T> router,
                            final String key)
        {
            this.router = router;
            this.key = Objects.requireNonNull(key, "key");
            this.connector = Processor.fromFunctionScript(router.stage, this::onMessage);
        }

        private T onMessage (final T message)
        {
            return active.get() ? message : null;
        }

        public Subscriber<T> activate ()
        {
            if (active.compareAndSet(false, true))
            {
                router.subscribe(key, this);
            }

            return this;
        }

        public Subscriber<T> deactivate ()
        {
            if (active.compareAndSet(true, false))
            {
                router.unsubscribe(key, this);
            }

            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Output<T> dataOut ()
        {
            return connector.dataOut();
        }
    }
}
