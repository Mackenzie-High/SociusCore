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

/**
 * Casts messages from one type to another and then forwards them.
 *
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public final class TypeCaster<I, O>
        implements Pipeline<I, O>
{
    private final Class<O> type;

    private final Pipeline<I, O> actorCast;

    private final Processor<I> actorFail;

    private TypeCaster (final Stage stage,
                        final Class<O> type)
    {
        Objects.requireNonNull(stage, "stage");
        this.type = Objects.requireNonNull(type, "type");
        this.actorCast = Pipeline.fromFunctionScript(stage, this::onMessage);
        this.actorFail = Processor.fromIdentityScript(stage);
    }

    private O onMessage (final I message)
    {
        if (type.isInstance(message))
        {
            final O converted = type.cast(message);
            return converted;
        }
        else
        {
            actorFail.accept(message);
            return null;
        }
    }

    /**
     * Input Connection.
     *
     * @return the input that receives the messages to type-cast.
     */
    @Override
    public Input<I> dataIn ()
    {
        return actorCast.dataIn();
    }

    /**
     * Output Connection.
     *
     * @return the output that forwards the messages after the type-cast.
     */
    @Override
    public Output<O> dataOut ()
    {
        return actorCast.dataOut();
    }

    /**
     * Output Connection.
     *
     * @return the output that forwards the messages that failed the type-cast.
     */
    public Output<I> errorOut ()
    {
        return actorFail.dataOut();
    }

    /**
     * Factory Method.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     * @param stage will be used to create private actors.
     * @param type is the type of the outgoing messages.
     * @return the new converter.
     */
    public static <I, O> TypeCaster<I, O> newTypeCaster (final Stage stage,
                                                         final Class<O> type)
    {
        return new TypeCaster<>(stage, type);
    }
}
