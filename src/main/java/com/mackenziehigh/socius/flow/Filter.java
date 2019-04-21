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

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Filters incoming messages based on a predicate and then
 * forwards only those messages that the predicate accepts.
 *
 * @param <T> is the type of messages flowing through the filter.
 */
public final class Filter<T>
        implements DataPipeline<T, T>
{
    private final Processor<T> actor;

    private final Predicate<T> condition;

    private Filter (final Stage stage,
                    final Predicate<T> condition)
    {
        Objects.requireNonNull(stage, "stage");
        this.condition = Objects.requireNonNull(condition, "condition");
        this.actor = Processor.newFunction(stage, this::onMessage);
    }

    private T onMessage (final T message)
    {
        return condition.test(message) ? message : null;
    }

    /**
     * Input Connection.
     *
     * @return the input the provides the messages to filter.
     */
    @Override
    public Input<T> dataIn ()
    {
        return actor.dataIn();
    }

    /**
     * Output Connection.
     *
     * @return the output that will receive messages that the filter accepted.
     */
    @Override
    public Output<T> dataOut ()
    {
        return actor.dataOut();
    }

    /**
     * Factory Method.
     *
     * @param <T> is type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @param condition determines whether a message should be allowed through the filter.
     * @return this.
     */
    public static <T> Filter<T> newFilter (final Stage stage,
                                           final Predicate<T> condition)
    {
        return new Filter<>(stage, condition);
    }

}
