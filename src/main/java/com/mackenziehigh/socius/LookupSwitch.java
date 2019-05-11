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

import com.google.common.collect.Lists;
import com.mackenziehigh.cascade.Cascade.ActorFactory;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Conditionally routes messages based on an ordered series of option predicates.
 *
 * @param <T> is the type of the incoming and outgoing messages.
 */
public final class LookupSwitch<T>
        implements Pipeline<T, T>
{
    private final ActorFactory stage;

    /**
     * Provides the data-input connector.
     */
    private final Processor<T> procDataIn;

    /**
     * Provides the data-output connector.
     */
    private final Processor<T> procDataOut;

    /**
     * This is a list of (condition, destination) tuples.
     */
    private final List<Entry<Predicate<T>, Input<T>>> routes = Lists.newCopyOnWriteArrayList();

    private LookupSwitch (final ActorFactory stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.procDataIn = Processor.fromConsumerScript(stage, this::onMessage);
        this.procDataOut = Processor.fromIdentityScript(stage);
    }

    private void onMessage (final T message)
    {
        /**
         * If there is a destination corresponding to the message,
         * then route the message to that destination.
         */
        for (Entry<Predicate<T>, Input<T>> route : routes)
        {
            final Predicate<T> condition = route.getKey();
            final Input<T> destination = route.getValue();

            if (condition.test(message))
            {
                destination.send(message);
                return;
            }
        }

        /**
         * Otherwise, route the message to the default data-output.
         */
        procDataOut.dataIn().send(message);
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
     * <p>
     * If this method is invoked multiple times,
     * then the condition provided by prior calls will
     * have precedence over those in subsequent calls.
     * More specifically, a message that matches multiple conditions
     * will be routed to the destination provided by the first match.
     * </p>
     *
     * @param condition determines which messages will be routed to this output.
     * @return the output that will receive the messages that match the condition.
     */
    public synchronized Output<T> selectIf (final Predicate<T> condition)
    {
        Objects.requireNonNull(condition, "condition");
        final Processor<T> proc = Processor.fromIdentityScript(stage);
        routes.add(new AbstractMap.SimpleImmutableEntry<>(condition, proc.dataIn()));
        return proc.dataOut();
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @return the new inserter.
     */
    public static <T> LookupSwitch<T> newLookupInserter (final ActorFactory stage)
    {
        return new LookupSwitch(stage);
    }
}
