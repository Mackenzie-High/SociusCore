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
import com.mackenziehigh.cascade.Cascade.ActorFactory;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

/**
 * A load balancer that uses a Markov-Chain like algorithm.
 *
 * @param <T> is the type of the incoming and outgoing messages.
 */
public final class MarkovBalancer<T>
        implements DataSink<T>
{

    /**
     * Provides the data-input connector.
     */
    private final Processor<T> input;

    /**
     * Provides the data-output connectors.
     */
    private final ImmutableList<Selection> outputs;

    private final Random random;

    private MarkovBalancer (final ActorFactory stage,
                            final byte[] seed,
                            final int... weights)
    {
        Preconditions.checkNotNull(stage, "stage");

        this.input = Processor.fromConsumerScript(stage, this::forwardFromHub);
        this.random = new SecureRandom(seed);

        Arrays.sort(weights);

        final ImmutableList.Builder<Selection> builderOutputs = ImmutableList.builder();
        for (int i = 0; i < weights.length; i++)
        {
            final Processor<T> output = Processor.fromIdentityScript(stage);
            builderOutputs.add(new Selection(output, weights[i]));
        }
        this.outputs = builderOutputs.build();
    }

    private void forwardFromHub (final T message)
    {
        final double value = random.nextDouble();

        for (int i = 0; i < outputs.size(); i++)
        {
            outputs.get(i).send(value, message);
        }
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
        return outputs.get(index).output.dataOut();
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
     * @param seed will be used to seed the random number generator.
     * @param weights are the probabilities of a message being sent to the given output.
     * @return the new balancer.
     */
    public static <T> MarkovBalancer<T> newRoundRobin (final ActorFactory stage,
                                                       final byte[] seed,
                                                       final int... weights)
    {
        return new MarkovBalancer<>(stage, seed, weights);
    }

    private final class Selection
    {
        private final Processor<T> output;

        private final double weight;

        public Selection (final Processor<T> output,
                          final double weight)
        {
            Preconditions.checkArgument(weight >= 0.0, "weight >= 0");
            Preconditions.checkArgument(weight <= 1.0, "weight <= 0.0");
            this.output = output;
            this.weight = weight;
        }

        public boolean send (final double value,
                             final T message)
        {
            if (weight <= value)
            {
                output.accept(message);
                return true;
            }
            else
            {
                return false;
            }
        }
    }
}
