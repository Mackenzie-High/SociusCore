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

import com.mackenziehigh.cascade.Cascade.ActorFactory;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Message Bus for (N x N) <i>Intra</i>-Process-Communication.
 *
 * @param <T> is the type of messages that flow through the message-bus.
 */
public final class Bus<T>
        implements DataBus<T, T>
{
    private final ActorFactory stage;

    /**
     * This processor is used to connect the inputs to the outputs.
     */
    private final Processor<T> hub;

    /**
     * Messages are routed from these processors to the hub.
     *
     * <p>
     * In theory, the inputs could have been connected directly to the hub.
     * However, in practice, that would be inefficient,
     * because actors use copy-on-write lists in their connectors.
     * </p>
     */
    private final Map<Object, Processor<T>> inputs = new ConcurrentHashMap<>();

    /**
     * Messages are routed from the hub to these processors.
     *
     * <p>
     * In theory, the outputs could have been connected directly to the hub.
     * However, in practice, that would be inefficient,
     * because actors use copy-on-write lists in their connectors.
     * </p>
     */
    private final Map<Object, Processor<T>> outputs = new ConcurrentHashMap<>();

    /**
     * Cache this, because we will be iterating over it frequently.
     */
    private final Collection<Processor<T>> outputsView = outputs.values();

    private Bus (final ActorFactory stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.hub = Processor.newConsumer(stage, this::forwardFromHub);
    }

    /**
     * Get a named input that supplies messages to this message-bus.
     *
     * <p>
     * This method is synchronized, since this method may need to
     * create the connector that will be associated with the key.
     * If two methods called, without synchronization,
     * then two connectors could potentially be created.
     * </p>
     *
     * @param key identifies the input to retrieve.
     * @return the named input.
     */
    @Override
    public synchronized Input<T> dataIn (final Object key)
    {
        Objects.requireNonNull(key, "key");

        if (inputs.containsKey(key) == false)
        {
            inputs.put(key, Processor.newConsumer(stage, this::forwardToHub));
        }

        return inputs.get(key).dataIn();
    }

    /**
     * Get a named output that transmits messages from this message-bus.
     *
     * <p>
     * This method is synchronized, since this method may need to
     * create the connector that will be associated with the key.
     * If two methods called, without synchronization,
     * then two connectors could potentially be created.
     * </p>
     *
     * @param key identifies the output to retrieve.
     * @return the named output.
     */
    @Override
    public synchronized Output<T> dataOut (final Object key)
    {
        Objects.requireNonNull(key, "key");

        if (outputs.containsKey(key) == false)
        {
            outputs.put(key, Processor.newConnector(stage));
        }

        return outputs.get(key).dataOut();
    }

    /**
     * Factory Method.
     *
     * @param <M> is the type of messages that flow through the message-bus.
     * @param stage will be used to create private actors.
     * @return the new message-bus.
     */
    public static <M> Bus<M> newBus (final ActorFactory stage)
    {
        return new Bus<>(stage);
    }

    private void forwardToHub (final T message)
    {
        hub.dataIn().send(message);
    }

    private void forwardFromHub (final T message)
    {
        for (Processor<T> output : outputsView)
        {
            output.dataIn().send(message);
        }
    }

}
