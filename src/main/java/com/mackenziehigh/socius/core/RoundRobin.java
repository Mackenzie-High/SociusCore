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
package com.mackenziehigh.socius.core;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;

/**
 * A load balancer that uses a round-robin algorithm.
 *
 * @param <T> is the type of the incoming and outgoing messages.
 */
public final class RoundRobin<T>
        implements Sink<T>
{

    /**
     * Provides the data-input connector.
     */
    private final WeightBalancer<T> delegate;

    private RoundRobin (final Stage stage,
                        final int arity)
    {
        this.delegate = WeightBalancer.newWeightBalancer(stage, arity, x -> 1);
    }

    /**
     * Input Connection.
     *
     * @return the input that provides the messages to balance.
     */
    @Override
    public Input<T> dataIn ()
    {
        return delegate.dataIn();
    }

    /**
     * Output Connection.
     *
     * @param index identifies the output.
     * @return the output.
     */
    public Output<T> dataOut (final int index)
    {
        return delegate.dataOut(index);
    }

    /**
     * Get the number of output connectors.
     *
     * @return the number of outputs.
     */
    public int arity ()
    {
        return delegate.arity();
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
