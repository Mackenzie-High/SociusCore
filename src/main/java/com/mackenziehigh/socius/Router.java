/*
 * Copyright 2019 Michael Mackenzie High
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mackenziehigh.socius;

import com.google.common.collect.ImmutableList;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Provides an in-process publish/subscribe mechanism.
 *
 * <p>
 * A router contains zero-or-more independent communication channels.
 * Each communication channel has an associated route-key (K).
 * Zero-or-more publishers may register to connect to channel (K).
 * Zero-or-more subscribers may register to connect to channel (K).
 * Whenever a registered publisher sends a message,
 * the message will be forwarded to every subscriber that is
 * registered to receive messages from channel (K).
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
 * <p>
 * Assume that two publishers (P1 and P2) are connected to channel (K).
 * Assume that two subscribers (S1 and S2) are connected to channel (K).
 * Assume that (P1) and (P2), independently, send message (X and Y) to channel (K) simultaneously.
 * Specifically, (P1) sends message (X) and (P2) sends message (Y).
 * By default, there is no guarantee that the subscribers will receive the messages in the same order.
 * For example, (S1) may receive (X) and then (Y), whereas (S2) received (Y) and then (X).
 * This provides for maximum performance, since the publishers do not synchronize sends.
 * However, an option is provided to auto-synchronize the sends, by invoking <code>synchronize()</code>.
 * In that case, the subscribers will receive the messages in the same order.
 * For example, either (S1 and S2) will receive (X) and then (Y), or (S1 and S2) will receive (Y) and then (X).
 * </p>
 *
 * <p>
 * <b>Performance Note:</b> Internally, this class maintains a map of that maps a routing-key
 * to an immutable list of the subscribers interested in receiving messages with that key.
 * The use of the immutable list promotes locality, avoids the need for locks, and avoids
 * unnecessary object creation (iterators). However, there is a potential down-side.
 * Specifically, the list must be copied each time a subscriber subscribes to the <b>same</b> key.
 * Thus, subscribing large numbers (1000s) of subscribers to a single key can become inefficient.
 * </p>
 *
 * @param <K> is the type of the routing-keys.
 * @param <T> is the type of the incoming and outgoing messages.
 */
public final class Router<K, T>
{
    /**
     * This stage will be used to create private actors.
     */
    private final Stage stage;

    /**
     * This map maps a routing-key to a list of subscribers that
     * are interested in messages with that routing-key.
     */
    private final ConcurrentMap<K, ImmutableList<Subscriber<K, T>>> routeMap = new ConcurrentHashMap<>();

    /**
     * Provides the sink-all output connector.
     */
    private final Processor<T> sinkAll;

    /**
     * Provides the sink-dead output connector.
     */
    private final Processor<T> sinkDead;

    /**
     * This flag is true, if all subscribers shall receive messages in the same order,
     * if they are subscribed to the same routing-key.
     */
    private volatile boolean sync = false;

    private Router (final Stage stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.sinkAll = Processor.fromIdentityScript(stage);
        this.sinkDead = Processor.fromIdentityScript(stage);
    }

    /**
     * Factory Method.
     *
     * @param <K> is the type of the routing-keys.
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @return the new router.
     */
    public static <K, T> Router<K, T> newRouter (final Stage stage)
    {
        return new Router<>(stage);
    }

    /**
     * Henceforth, all subscribers shall receive messages in the same order,
     * if they are subscribed to the same routing-key.
     *
     * @return this.
     */
    public Router<K, T> synchronize ()
    {
        sync = true;
        return this;
    }

    /**
     * Determine whether all subscribers shall receive messages in the same order,
     * if they are subscribed to the same routing-key.
     *
     * @return true, if so.
     */
    public boolean isSynchronous ()
    {
        return sync;
    }

    /**
     * Create a new activated publisher that can send messages
     * to communication channels identified on-demand by
     * extracting the routing-key from the messages themselves
     * using the given function.
     *
     * @param keyFunction will be used to obtain routing-keys from messages.
     * @return the newly created publisher.
     */
    public Publisher<K, T> newPublisher (final Function<T, K> keyFunction)
    {
        Objects.requireNonNull(keyFunction, "keyFunction");
        return new Publisher(this, keyFunction).activate();
    }

    /**
     * Create a new activated publisher that can send messages
     * to the communication channel identified by the given key.
     *
     * @param key is the routing-key that identifies the channel.
     * @return the newly created publisher.
     */
    public Publisher<K, T> newPublisher (final K key)
    {
        Objects.requireNonNull(key, "key");
        return new Publisher(this, msg -> key).activate();
    }

    /**
     * Create a new activated subscriber that can receive messages
     * from the communication channels identified by the given keys.
     *
     * @param keys are the routing-keys that identify the channels.
     * @return the newly created subscriber.
     */
    public Subscriber<K, T> newSubscriber (final K... keys)
    {
        Objects.requireNonNull(keys, "keys");
        final var keyList = Arrays.asList(keys);
        return newSubscriber(keyList);
    }

    /**
     * Create a new activated subscriber that can receive messages
     * from the communication channels identified by the given keys.
     *
     * @param keys are the routing-keys that identify the channels.
     * @return the newly created subscriber.
     */
    public Subscriber<K, T> newSubscriber (final Collection<K> keys)
    {
        Objects.requireNonNull(keys, "keys");
        final var keySet = new HashSet<>(keys);
        return new Subscriber(this, keySet).activate();
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
     * <p>
     * This output only receives messages from activated publishers.
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
     * Warning: This method may be invoked by multiple threads at once.
     * This method is *not* an actor itself!
     * </p>
     *
     * @param key is a routing-key that identifies the channel to send-to.
     * @param message will be sent to the identified channel.
     * @return this.
     */
    public Router<K, T> send (final K key,
                              final T message)
    {
        if (sync)
        {
            /**
             * Ensure that all of the subscribers receive messages in the same order,
             * if the messages originated from different publishers. Of course,
             * messages from the same publisher are always in-order anyway.
             */
            synchronized (this)
            {
                sendImp(key, message);
            }
        }
        else
        {
            /**
             * Send the message to all interested subscribers as fast as possible,
             * even if that means subscribers receive messages from different
             * publishers in different orders. That said, messages from the same
             * publisher are always sent in-order.
             */
            sendImp(key, message);
        }

        return this;
    }

    private void sendImp (final K key,
                          final T message)
    {
        boolean none = true;

        /**
         * Anyone listening to the all-sink gets all of the messages, regardless of key.
         */
        sinkAll.accept(message);

        /**
         * Get all of the subscribers for the given routing-key, if any.
         *
         * Notice that the use of an immutable-list here avoids the need for locks,
         * since we can rest assured that no subscribers will be added/removed
         * while we are iterating over the list. Further, notice that the iteration
         * uses a for-loop, instead of a for-each-loop, which can avoid an unnecessary
         * object allocation (Iterator).
         */
        final ImmutableList<Subscriber<K, T>> routeList = routeMap.getOrDefault(key, ImmutableList.of());

        /**
         * Send the message to all of the subscribers, if any.
         */
        for (int k = 0; k < routeList.size(); k++)
        {
            routeList.get(k).connector.accept(message);
            none = false;
        }

        /**
         * If no subscribers were interested in receiving the message,
         * then send the message to the dead-sink to aid debugging.
         */
        if (none)
        {
            sinkDead.accept(message);
        }
    }

    private void subscribe (final K key,
                            final Subscriber<K, T> subscriber)
    {
        /**
         * The copy is inefficient here, but makes the message-sending more efficient.
         * The synchronization prevents duplicate (concurrent) subscriptions.
         */
        synchronized (this)
        {
            routeMap.putIfAbsent(key, ImmutableList.of());
            final Set<Subscriber<K, T>> copy = new HashSet<>(routeMap.get(key));
            copy.add(subscriber);
            routeMap.put(key, ImmutableList.copyOf(copy));
        }
    }

    private void unsubscribe (final K key,
                              final Subscriber<K, T> subscriber)
    {
        /**
         * The copy is inefficient here, but makes the message-sending more efficient.
         * The synchronization prevents duplicate (concurrent) unsubscriptions.
         */
        synchronized (this)
        {
            routeMap.putIfAbsent(key, ImmutableList.of());
            final Set<Subscriber<K, T>> copy = new HashSet<>(routeMap.get(key));
            copy.add(subscriber);
            routeMap.put(key, ImmutableList.copyOf(copy));
        }
    }

    /**
     * Forwards incoming messages to any activated subscribers
     * that have subscribed to the same communication channel.
     *
     * <p>
     * A publisher is only eligible for garbage-collection,
     * if it is disconnected from all strongly-referenced external actors.
     * </p>
     *
     * @param <K> is the type of the routing-keys.
     * @param <T> is the type of the incoming messages.
     */
    public static final class Publisher<K, T>
            implements Sink<T>
    {
        /**
         * This is the router that maintains the list of subscribers.
         */
        private final Router<K, T> router;

        /**
         * This function extracts the routing-key tat identifies
         * the communication channel to use for a given message.
         */
        private final Function<T, K> keyFunction;

        /**
         * This processor will receive the incoming messages
         * and then perform the actual forwarding operation.
         */
        private final Processor<T> connector;

        /**
         * This flag is used to turn this publisher on and off.
         */
        private final AtomicBoolean active = new AtomicBoolean();

        private Publisher (final Router<K, T> router,
                           final Function<T, K> keyFunction)
        {
            this.router = router;
            this.keyFunction = Objects.requireNonNull(keyFunction, "keyFunction");
            this.connector = Processor.fromConsumerScript(router.stage, this::onMessage);
        }

        /**
         * This actor script will send the given message to the subscribers,
         * if this publisher is currently turned on (activated).
         *
         * @param message will be sent to the subscribers, if allowed.
         */
        private void onMessage (final T message)
        {
            if (active.get())
            {
                final K routingKey = keyFunction.apply(message);
                router.send(routingKey, message);
            }
        }

        /**
         * Activate this publisher, so that messages will be sent to subscribers.
         *
         * @return this.
         */
        public Publisher<K, T> activate ()
        {
            active.set(true);
            return this;
        }

        /**
         * Deactivate this publisher, so that messages will not longer be sent to subscribers.
         *
         * @return this.
         */
        public Publisher<K, T> deactivate ()
        {
            active.set(false);
            return this;
        }

        /**
         * Determine whether this publisher is activated.
         *
         * @return true, if so.
         */
        public boolean isActive ()
        {
            return active.get();
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
     * Receives messages from any activated publishers that
     * are connected to the same communication channel.
     *
     * <p>
     * A subscriber is only eligible for garbage-collection,
     * if it is disconnected from all strongly-referenced
     * external actors <b>and</b> deactivated.
     * </p>
     *
     * @param <K> is the type of the routing-keys.
     * @param <T> is the type of the incoming messages.
     */
    public static final class Subscriber<K, T>
            implements Source<T>
    {
        /**
         * This is the router that maintains the list of subscribers.
         */
        private final Router<K, T> router;

        /**
         * This routing-key identifies the communication channel.
         */
        private final List<K> routingKeys;

        /**
         * This processor will receive the incoming messages from
         * the publishers and then forward them out of the router.
         */
        private final Processor<T> connector;

        /**
         * This flag is used to turn this subscriber on and off.
         */
        private final AtomicBoolean active = new AtomicBoolean();

        private Subscriber (final Router<K, T> router,
                            final Set<K> keys)
        {
            this.router = router;
            this.routingKeys = List.copyOf(Objects.requireNonNull(keys, "key"));
            this.connector = Processor.fromFunctionScript(router.stage, this::onMessage);
        }

        /**
         * This actor script receives messages from the publishers,
         * and forwards them out of the router, if activated.
         *
         * @param message will be sent out of the router, if allowed.
         */
        private T onMessage (final T message)
        {
            return active.get() ? message : null;
        }

        /**
         * Get the routing-keys that identify the communication channels
         * that this subscriber receives messages from.
         *
         * @return the associated routing-keys.
         */
        public Collection<K> routingKeys ()
        {
            return routingKeys;
        }

        /**
         * Activate this subscriber, so that messages will be received from publishers.
         *
         * @return this.
         */
        public Subscriber<K, T> activate ()
        {
            if (active.compareAndSet(false, true))
            {
                for (K key : routingKeys)
                {
                    router.subscribe(key, this);
                }
            }

            return this;
        }

        /**
         * Activate this subscriber, so that messages will be received from publishers.
         *
         * @return this.
         */
        public Subscriber<K, T> deactivate ()
        {
            if (active.compareAndSet(true, false))
            {
                for (K key : routingKeys)
                {
                    router.unsubscribe(key, this);
                }
            }

            return this;
        }

        /**
         * Determine whether this subscriber is activated.
         *
         * @return true, if so.
         */
        public boolean isActive ()
        {
            return active.get();
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
