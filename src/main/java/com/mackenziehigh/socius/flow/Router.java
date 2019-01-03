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
package com.mackenziehigh.socius.flow;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Collection;
import java.util.Objects;

/**
 * Provides a publish/subscribe mechanism.
 *
 * <p>
 * A router contains zero-or-more independent communication channels.
 * Each communication channel has an associated identifier-key (K).
 * Zero-or-more publishers may register to connect to channel (K).
 * Zero-or-more subscribers may register to connect to channel (K).
 * Whenever a registered publisher sends a messages,
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
 * @param <K> is the type of the keys used to identify the communication channels.
 * @param <M> is the type of the incoming and outgoing messages.
 */
public final class Router<K, M>
{
    private final Stage stage;

    /**
     * Each row is identified by the key corresponding to a communication channel.
     * Each column is identified by an output that provides messages to the channel.
     * The cells contain processors that receive messages from the channel.
     * The output is connected to the data-input stream of the processors.
     */
    private final Table<K, Output<M>, Processor<M>> publishers = HashBasedTable.create();

    /**
     * This map maps a key that identifies a communication channel to
     * the subscribers that want to receive messages from that channel.
     */
    private final Multimap<K, Input<M>> subscriptions = HashMultimap.create();

    /**
     * Provides the sink-all output connector.
     */
    private final Processor<M> sinkAll;

    /**
     * Provides the sink-dead output connector.
     */
    private final Processor<M> sinkDead;

    /**
     * This lock protects the registration and de-registration of publishers and subscribers.
     * In addition, messages cannot be sent, while a change in registration is occurring.
     */
    private final Object lock = new Object();

    private Router (final Stage stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.sinkAll = Processor.newConnector(stage);
        this.sinkDead = Processor.newConnector(stage);
    }

    /**
     * Output Connection.
     *
     * @return the output that unconditionally receives all messages.
     */
    public Output<M> sinkAll ()
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
    public Output<M> sinkDead ()
    {
        return sinkDead.dataOut();
    }

    /**
     * Register a publisher.
     *
     * <p>
     * This method is a no-op, if the publisher is already registered.
     * </p>
     *
     * @param connector will provide the messages to route to subscribers.
     * @param key identifies the communication channel.
     * @return this.
     */
    public Router<K, M> publish (final Output<M> connector,
                                 final K key)
    {
        Objects.requireNonNull(connector, "connector");
        Objects.requireNonNull(key, "key");

        synchronized (lock)
        {
            if (publishers.contains(key, connector) == false)
            {
                final Processor<M> actor = Processor.newConsumer(stage, msg -> send(key, msg));
                publishers.put(key, connector, actor);
                connector.connect(actor.dataIn());
            }
        }

        return this;
    }

    /**
     * Deregister a publisher.
     *
     * <p>
     * This method is a no-op, if the publisher is not currently registered.
     * </p>
     *
     * @param connector will no longer provide the messages to route to subscribers.
     * @param key identifies the communication channel.
     * @return this.
     */
    public Router<K, M> unpublish (final Output<M> connector,
                                   final K key)
    {
        Objects.requireNonNull(connector, "connector");
        Objects.requireNonNull(key, "key");

        synchronized (lock)
        {
            if (publishers.contains(key, connector))
            {
                final Processor<M> actor = publishers.get(key, connector);
                connector.disconnect(actor.dataIn());
                publishers.remove(key, connector);
            }
        }

        return this;
    }

    /**
     * Register a subscriber.
     *
     * <p>
     * This method is a no-op, if the subscriber is already registered.
     * </p>
     *
     * @param connector will receive the the messages routed from the publishers.
     * @param key identifies the communication channel.
     * @return this.
     */
    public Router<K, M> subscribe (final Input<M> connector,
                                   final K key)
    {
        Objects.requireNonNull(connector, "connector");
        Objects.requireNonNull(key, "key");

        synchronized (lock)
        {
            subscriptions.put(key, connector);
        }

        return this;
    }

    /**
     * Deregister a subscriber.
     *
     * @param connector will no longer receive the the messages routed from the publishers.
     * @param key identifies the communication channel.
     * @return this.
     */
    public Router<K, M> unsubscribe (final Input<M> connector,
                                     final K key)
    {
        Objects.requireNonNull(connector, "connector");
        Objects.requireNonNull(key, "key");

        synchronized (lock)
        {
            subscriptions.remove(key, connector);
        }

        return this;
    }

    /**
     * Send a message.
     *
     * <p>
     * This method is a no-op, if the subscriber is not currently registered.
     * </p>
     *
     * @param key identifies the communication channel.
     * @param message will be sent to all subscribers associated with the named channel.
     * @return this.
     */
    public Router<K, M> send (final K key,
                              final M message)
    {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(message, "message");

        synchronized (lock)
        {
            /**
             * Find all of the subscribers for the named channel.
             */
            final Collection<Input<M>> subscribers = subscriptions.get(key);

            /**
             * Send the message to each of the subscribers, if any.
             */
            for (Input<M> sub : subscribers)
            {
                sub.send(message);
            }

            /**
             * If no subscribers are present, then send
             * the message to the sink-dead output.
             */
            if (subscribers.isEmpty())
            {
                sinkDead.accept(message);
            }

            /**
             * Send the message to the sink-all output,
             * which receives all messages.
             */
            sinkAll.accept(message);
        }

        return this;
    }

    /**
     * Factory Method.
     *
     * @param <K> is the type of the keys used to identify the communication channels.
     * @param <M> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @return the new router.
     */
    public static <K, M> Router<K, M> newRouter (final Stage stage)
    {
        return new Router<>(stage);
    }
}
