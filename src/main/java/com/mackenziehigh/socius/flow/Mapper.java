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
import com.mackenziehigh.cascade.Cascade.Stage.Actor.FunctionScript;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;

/**
 * Applies a map-function to incoming messages and then forwards the results,
 * such that the output data-type is the same as the input data-type.
 *
 * <p>
 * If the map-function returns null or void, then no message will be forwarded.
 * Thus, in effect, returning null or void causes the <code>Mapper</code> to act as a <code>Filter</code>.
 * </p>
 *
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public final class Mapper<I, O>
        implements DataPipeline<I, O>
{
    private final Actor<I, O> actor;

    private Mapper (final Actor<I, O> actor)
    {
        this.actor = actor;
    }

    /**
     * Input Connection.
     *
     * @return the input that supplies the messages to be processed.
     */
    @Override
    public Input<I> dataIn ()
    {
        return actor.input();
    }

    /**
     * Output Connection.
     *
     * @return the output that receives the results of processing the messages.
     */
    @Override
    public Output<O> dataOut ()
    {
        return actor.output();
    }

    /**
     * Factory Method.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     * @param stage will be used t create private actors.
     * @param script defines the processing to perform.
     * @return the new processor.
     */
    public static <I, O> Mapper<I, O> newFunction (final Stage stage,
                                                   final FunctionScript<I, O> script)
    {
        return new Mapper<>(stage.newActor().withFunctionScript(script).create());
    }
}
