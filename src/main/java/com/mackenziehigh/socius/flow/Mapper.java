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

import com.mackenziehigh.cascade.Cascade.ActorFactory;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.ContextScript;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.FunctionScript;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Map;

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
    public static <I, O> Mapper<I, O> fromContextScript (final ActorFactory stage,
                                                         final ContextScript<I, O> script)
    {
        return new Mapper<>(stage.newActor().withContextScript(script).create());
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
    public static <I, O> Mapper<I, O> fromFunctionScript (final ActorFactory stage,
                                                          final FunctionScript<I, O> script)
    {
        return new Mapper<>(stage.newActor().withFunctionScript(script).create());
    }

    /**
     * Factory Method.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     * @param stage will be used t create private actors.
     * @param mappings defines the the input and output pairings.
     * @param defaultValue is the default output, if the input is not in the mappings map.
     * @return the new processor.
     */
    public static <I, O> Mapper<I, O> fromMap (final ActorFactory stage,
                                               final Map<I, O> mappings,
                                               final O defaultValue)
    {
        /**
         * The map may change during the call.
         * Therefore, get the output and store in a local,
         * rather than calling containsKey() and then get().
         * Moreover, this is slightly faster conceptually.
         */
        final FunctionScript<I, O> script = msg ->
        {
            final O output = mappings.get(msg);
            return output == null ? defaultValue : output;
        };

        return fromFunctionScript(stage, script);
    }
}
