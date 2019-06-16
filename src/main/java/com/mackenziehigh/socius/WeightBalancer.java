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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToIntFunction;

/**
 * A load balancer that uses a weight minimizing algorithm.
 *
 * @param <T> is the type of the incoming and outgoing messages.
 */
public final class WeightBalancer<T>
        implements Sink<T>
{

    /**
     * Provides the data-input connector.
     */
    private final Processor<T> input;

    /**
     * Provides the data-output connectors.
     */
    private final List<Processor<T>> outputs;

    /**
     * Each element in this list corresponds to an output connector.
     * Each element stores how much weight has passed through that connector.
     */
    private final List<AtomicLong> weights;

    /**
     * This function determines the weight of each incoming message.
     */
    private final ToIntFunction<T> scale;

    private WeightBalancer (final Stage stage,
                            final int arity,
                            final ToIntFunction<T> scale)
    {
        Objects.requireNonNull(stage, "stage");

        if (arity <= 0)
        {
            throw new IllegalArgumentException("arity <= 0");
        }

        this.input = Processor.fromConsumerScript(stage, this::forwardFromHub);

        final List<Processor<T>> builderOutputs = new LinkedList<>();
        final List<AtomicLong> builderWeights = new LinkedList<>();
        for (int i = 0; i < arity; i++)
        {
            builderOutputs.add(Processor.fromIdentityScript(stage));
            builderWeights.add(new AtomicLong());
        }
        this.outputs = List.copyOf(builderOutputs);
        this.weights = List.copyOf(builderWeights);

        this.scale = Objects.requireNonNull(scale, "scale");
    }

    private void forwardFromHub (final T message)
    {
        final int weight = scale.applyAsInt(message);

        int index = 0;

        long best = weight + weights.get(0).get();

        for (int i = 0; i < outputs.size(); i++)
        {
            final long current = weights.get(i).get();
            final long proposed = current + weight;
            index = (proposed < best) ? i : index;
            best = (proposed < best) ? proposed : best;
        }

        weights.get(index).addAndGet(weight);
        outputs.get(index).dataIn().send(message);
    }

    /**
     * Get how much weight has been sent to the indexed output.
     *
     * @param index identifies an output connection.
     * @return the sum of the weights sent to the output.
     */
    public long sumOf (final int index)
    {
        return weights.get(index).get();
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
     * @param scale assigns weights to each of the incoming messages.
     * @return the new balancer.
     */
    public static <T> WeightBalancer<T> newWeightBalancer (final Stage stage,
                                                           final int arity,
                                                           final ToIntFunction<T> scale)
    {
        return new WeightBalancer<>(stage, arity, scale);
    }
}
