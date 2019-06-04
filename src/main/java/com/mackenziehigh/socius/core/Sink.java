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

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import java.util.function.Consumer;

/**
 * An actor that consumes incoming messages.
 *
 * <p>
 * This method implements that <code>java.util.function.Consumer</code> interface in
 * order to allow the actor to be used as the output destination <code>Stream</code>s,
 * which may be occasionally convenient when interacting with third-party APIs.
 * </p>
 *
 * @param <I> is the type of the incoming messages.
 */
public interface Sink<I>
        extends Consumer<I>
{
    /**
     * {@inheritDoc}
     */
    @Override
    public default void accept (I message)
    {
        dataIn().send(message);
    }

    /**
     * Input Connection.
     *
     * @return the data-input that provides the messages to the pipeline.
     */
    public Input<I> dataIn ();
}
