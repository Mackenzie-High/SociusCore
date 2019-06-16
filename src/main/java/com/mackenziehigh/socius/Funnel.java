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

/**
 * Funnels messages from multiple inputs into a single output.
 *
 * @param <T> is the type of the messages passing through the funnel.
 */
public final class Funnel<T>
        implements Source<T>
{
    private final Stage stage;

    private final Processor<T> output;

    private final ConcurrentMap<Object, Processor<T>> inputs = new ConcurrentHashMap<>();

    private final Object lock = new Object();

    private Funnel (final Stage stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.output = Processor.fromIdentityScript(stage);
    }

    /**
     * Output Connection.
     *
     * @return the output of the funnel.
     */
    @Override
    public Output<T> dataOut ()
    {
        return output.dataOut();
    }

    /**
     * Input Connection.
     *
     * @param key identifies the specific input.
     * @return the identified input to the funnel.
     */
    public Input<T> dataIn (final Object key)
    {
        /**
         * This is synchronized in order to prevent two processors
         * being created for the same key inadvertently.
         */
        synchronized (lock)
        {
            if (inputs.containsKey(key) == false)
            {
                final Input<T> connector = output.dataIn();
                final Processor<T> actor = Processor.fromConsumerScript(stage, connector::send);
                inputs.put(key, actor);
            }
        }

        return inputs.get(key).dataIn();
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of the messages passing through the funnel.
     * @param stage will be used to create private actors.
     * @return the new funnel.
     */
    public static <T> Funnel<T> newFunnel (final Stage stage)
    {
        return new Funnel<>(stage);
    }
}
