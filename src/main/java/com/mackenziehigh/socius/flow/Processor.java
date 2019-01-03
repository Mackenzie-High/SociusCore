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
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.ConsumerScript;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.FunctionScript;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.function.Consumer;

/**
 * Applies a map-function to incoming messages and then forwards the results.
 *
 * <p>
 * If the map-function returns null or void, then no message will be forwarded.
 * Thus, in effect, returning null or void causes the <code>Processor</code> to act as a <code>Filter</code>.
 * </p>
 *
 * @param <T> is the type of the incoming and outgoing messages.
 */
public final class Processor<T>
        implements Consumer<T>
{
    private final Actor<T, T> actor;

    private Processor (final Actor<T, T> actor)
    {
        this.actor = actor;
    }

    /**
     * Input Connection.
     *
     * @return the input that supplies the messages to be processed.
     */
    public Input<T> dataIn ()
    {
        return actor.input();
    }

    /**
     * Output Connection.
     *
     * @return the output that receives the results of processing the messages.
     */
    public Output<T> dataOut ()
    {
        return actor.output();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept (final T message)
    {
        dataIn().send(message);
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used t create private actors.
     * @param script defines the processing to perform.
     * @return the new processor.
     */
    public static <T> Processor<T> newFunction (final Stage stage,
                                                final FunctionScript<T, T> script)
    {
        return new Processor<>(stage.newActor().withScript(script).create());
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used t create private actors.
     * @param script defines the processing to perform.
     * @return the new processor.
     */
    public static <T> Processor<T> newConsumer (final Stage stage,
                                                final ConsumerScript<T> script)
    {
        return new Processor<>(stage.newActor().withScript(script).create());
    }

    /**
     * Creates a new identity processor.
     *
     * <p>
     * An identity processor merely forwards incoming messages to
     * the output without performing any actual transformation, etc.
     * </p>
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used t create private actors.
     * @return the new processor.
     */
    public static <T> Processor<T> newConnector (final Stage stage)
    {
        return new Processor<>(stage.newActor().withScript((T x) -> x).create());
    }

}
