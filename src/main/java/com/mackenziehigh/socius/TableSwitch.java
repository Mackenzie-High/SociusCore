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

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Conditionally routes messages based on a table lookup.
 *
 * @param <K> is the type of the routing-key that is contained in each message.
 * @param <T> is the type of the incoming and outgoing messages.
 */
public final class TableSwitch<K, T>
        implements Pipeline<T, T>
{
    private final Stage stage;

    /**
     * Provides the data-input connector.
     */
    private final Processor<T> procDataIn;

    /**
     * Provides the data-output connector.
     */
    private final Processor<T> procDataOut;

    /**
     * This map maps routing-keys to the corresponding receivers.
     */
    private final ConcurrentMap<K, Processor<T>> routingTable = new ConcurrentHashMap<>();

    /**
     * This function knows how to extract routing-keys from messages.
     */
    private final Function<T, K> extractor;

    /**
     * This lock synchronizes the creation of output receivers.
     */
    private final Object lock = new Object();

    private TableSwitch (final Stage stage,
                         final Function<T, K> extractor)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.procDataIn = Processor.fromConsumerScript(stage, this::onMessage);
        this.procDataOut = Processor.fromIdentityScript(stage);
        this.extractor = Objects.requireNonNull(extractor, "extractor");
    }

    private void onMessage (final T message)
    {
        final Object key = extractor.apply(message);

        final Processor<T> route = routingTable.get(key);

        if (route == null)
        {
            procDataOut.dataIn().send(message);
        }
        else
        {
            route.dataIn().send(message);
        }
    }

    /**
     * Input Connection.
     *
     * @return the input that provides the messages to select from.
     */
    @Override
    public Input<T> dataIn ()
    {
        return procDataIn.dataIn();
    }

    /**
     * Output Connection.
     *
     * @return the output that receives the messages that were not selected
     */
    @Override
    public Output<T> dataOut ()
    {
        return procDataOut.dataOut();
    }

    /**
     * Output Connection.
     *
     * @param key is the routing-key that identifies messages intended for this output.
     * @return the output that will receive the messages that contain the routing-key.
     */
    public Output<T> selectIf (final K key)
    {
        Objects.requireNonNull(key, "key");

        /**
         * Create the output receiver, if it does not already exist.
         * Do not allow concurrent calls to inadvertently create
         * duplicate output receivers for the same key.
         */
        synchronized (lock)
        {
            if (routingTable.containsKey(key) == false)
            {
                final Processor<T> proc = Processor.fromIdentityScript(stage);
                routingTable.put(key, proc);
            }
        }

        /**
         * The output connector is provided by the output receiver,
         * which was created above, or during a previous invocation.
         */
        return routingTable.get(key).dataOut();
    }

    /**
     * Factory Method.
     *
     * @param <K> is the type of the routing-key that is contained in each message.
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @param extractor knows how to extract routing-keys from messages.
     * @return the new switch.
     */
    public static <K, T> TableSwitch<K, T> newTableSwitch (final Stage stage,
                                                           final Function<T, K> extractor)
    {
        return new TableSwitch(stage, extractor);
    }
}
