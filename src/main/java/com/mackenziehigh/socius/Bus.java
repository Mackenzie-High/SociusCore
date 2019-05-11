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

import com.mackenziehigh.cascade.Cascade.ActorFactory;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;

/**
 * Message Bus for (N x N) <i>Intra</i>-Process-Communication.
 *
 * @param <T> is the type of messages that flow through the message-bus.
 */
public final class Bus<T>
{
    private final Funnel<T> funnel;

    private final Fanout<T> fanout;

    private Bus (final ActorFactory stage)
    {
        fanout = Fanout.newFanout(stage);
        funnel = Funnel.newFunnel(stage);
        funnel.dataOut().connect(fanout.dataIn());
    }

    /**
     * Get a named input that supplies messages to this message-bus.
     *
     * @param key identifies the input to retrieve.
     * @return the named input.
     */
    public Input<T> dataIn (final Object key)
    {
        return funnel.dataIn(key);
    }

    /**
     * Get a named output that transmits messages from this message-bus.
     *
     * @param key identifies the output to retrieve.
     * @return the named output.
     */
    public Output<T> dataOut (final Object key)
    {
        return fanout.dataOut(key);
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
}
