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
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.ConsumerScript;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.ContextScript;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.FunctionScript;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Applies a function to incoming messages and then forwards the results.
 *
 * <p>
 * If the function returns null or void, then no message will be forwarded.
 * Thus, in effect, returning null or void causes the <code>Processor</code> to act as a filter.
 * </p>
 *
 * @param <T> is the type of the incoming and outgoing messages.
 */
public interface Processor<T>
        extends Pipeline<T, T>
{
    /**
     * Input Connection.
     *
     * @return the input that supplies the messages to be processed.
     */
    @Override
    public Input<T> dataIn ();

    /**
     * Output Connection.
     *
     * @return the output that receives the results of processing the messages.
     */
    @Override
    public Output<T> dataOut ();

    /**
     * Factory Method.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param actor will be wrapped in a pipeline facade.
     * @return the new processor.
     */
    public static <T> Processor<T> fromActor (final Actor<T, T> actor)
    {
        Objects.requireNonNull(actor, "actor");
        return fromIO(actor.input(), actor.output());
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @param script defines the processing to perform.
     * @return the new processor.
     */
    public static <T> Processor<T> fromContextScript (final ActorFactory stage,
                                                      final ContextScript<T, T> script)
    {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(script, "script");
        return fromActor(stage.newActor().withContextScript(script).create());
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @param script defines the processing to perform.
     * @return the new processor.
     */
    public static <T> Processor<T> fromFunctionScript (final ActorFactory stage,
                                                       final FunctionScript<T, T> script)
    {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(script, "script");
        return fromActor(stage.newActor().withFunctionScript(script).create());
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @param script defines the processing to perform.
     * @return the new processor.
     */
    public static <T> Processor<T> fromConsumerScript (final ActorFactory stage,
                                                       final ConsumerScript<T> script)
    {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(script, "script");
        return fromActor(stage.newActor().withConsumerScript(script).create());
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
     * @param stage will be used to create private actors.
     * @return the new processor.
     */
    public static <T> Processor<T> fromIdentityScript (final ActorFactory stage)
    {
        Objects.requireNonNull(stage, "stage");
        return fromFunctionScript(stage, (T x) -> x);
    }

    /**
     * Factory Method.
     *
     * @param <T> is type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @param condition determines whether a message should be allowed through the filter.
     * @return this.
     */
    public static <T> Processor<T> fromFilter (final ActorFactory stage,
                                               final Predicate<T> condition)
    {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(condition, "condition");
        return fromFunctionScript(stage, x -> condition.test(x) ? x : null);
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param input provides the incoming messages.
     * @param output receives the outgoing messages.
     * @return the new processor.
     */
    public static <T> Processor<T> fromIO (final Input<T> input,
                                           final Output<T> output)
    {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");

        return new Processor<T>()
        {
            @Override
            public Input<T> dataIn ()
            {
                return input;
            }

            @Override
            public Output<T> dataOut ()
            {
                return output;
            }
        };
    }
}
