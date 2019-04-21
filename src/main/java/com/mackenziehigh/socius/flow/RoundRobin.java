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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Iterator;

/**
 * A load balancer that uses a round-robin algorithm.
 *
 * @param <T> is the type of the incoming and outgoing messages.
 */
public final class RoundRobin<T>
        implements DataSink<T>
{

    /**
     * Provides the data-input connector.
     */
    private final Processor<T> input;

    /**
     * Provides the data-output connectors.
     */
    private final ImmutableList<Processor<T>> outputs;

    /**
     * This is a circular iterator over the outputs.
     */
    private final Iterator<Processor<T>> iter;

    private RoundRobin (final Stage stage,
                        final int arity)
    {
        Preconditions.checkNotNull(stage, "stage");
        Preconditions.checkArgument(arity > 0, "arity <= 0");
        this.input = Processor.newConsumer(stage, this::forwardFromHub);

        final ImmutableList.Builder<Processor<T>> builderOutputs = ImmutableList.builder();
        for (int i = 0; i < arity; i++)
        {
            builderOutputs.add(Processor.newConnector(stage));
        }
        this.outputs = builderOutputs.build();

        this.iter = Iterables.cycle(outputs).iterator();
    }

    private void forwardFromHub (final T message)
    {
        iter.next().accept(message);
    }

    /**
     * Input Connection.
     *
     * @return the input that provides the messages to balance.
     */
    @Override
    public Input<T> dataIn ()
    {
        return input.dataIn();
    }

    /**
     * Output Connection.
     *
     * @param index identifies the output.
     * @return the output.
     */
    public Output<T> dataOut (final int index)
    {
        return outputs.get(index).dataOut();
    }

    /**
     * Get the number of output connectors.
     *
     * @return the number of outputs.
     */
    public int arity ()
    {
        return outputs.size();
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @param arity will be the number of output connections.
     * @return the new balancer.
     */
    public static <T> RoundRobin<T> newRoundRobin (final Stage stage,
                                                   final int arity)
    {
        return new RoundRobin<>(stage, arity);
    }
}
