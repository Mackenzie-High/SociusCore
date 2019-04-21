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

import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Map;
import java.util.Objects;

/**
 * Fans out messages from a single input to multiple outputs.
 *
 * @param <T> is the type of the messages passing through the fanout.
 */
public final class Fanout<T>
        implements DataSink<T>,
                   DataFanout<T>
{
    private final Stage stage;

    private final Processor<T> input;

    private final Map<Object, Processor<T>> outputs = Maps.newConcurrentMap();

    private final Object lock = new Object();

    private Fanout (final Stage stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.input = Processor.newConsumer(stage, this::send);
    }

    private void send (final T message)
    {
        outputs.values().forEach(x -> x.accept(message));
    }

    /**
     * Input Connection.
     *
     * @return return the input to the fanout.
     */
    @Override
    public Input<T> dataIn ()
    {
        return input.dataIn();
    }

    /**
     * Output Connection.
     *
     * @param key identifies the data-output to return.
     * @return the identified data-output.
     */
    @Override
    public Output<T> dataOut (final Object key)
    {
        Objects.requireNonNull(key, "key");

        /**
         * This is synchronized in order to prevent two processors
         * being created for the same key inadvertently.
         */
        synchronized (lock)
        {
            if (outputs.containsKey(key) == false)
            {
                final Processor output = Processor.newConnector(stage);
                outputs.put(key, output);
            }
        }

        return outputs.get(key).dataOut();
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of messages passing through the fanout.
     * @param stage will be used to create private actors.
     * @return the new fanout.
     */
    public static <T> Fanout<T> newFanout (final Stage stage)
    {
        return new Fanout<>(stage);
    }
}
