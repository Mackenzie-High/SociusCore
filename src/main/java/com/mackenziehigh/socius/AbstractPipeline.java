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
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Context;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Objects;

/**
 * Facilitates easy implementation of a <code>Pipeline</code> via sub-classing.
 *
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public abstract class AbstractPipeline<I, O>
        implements Pipeline<I, O>
{

    /**
     * Implement this method in order to handle incoming messages.
     *
     * @param message was just received.
     * @throws Throwable if something goes unexpectedly wrong.
     */
    protected abstract void onMessage (I message)
            throws Throwable;

    private final Actor<I, O> actor;

    protected AbstractPipeline (final ActorFactory stage)
    {
        Objects.requireNonNull(stage, "stage");
        this.actor = stage.newActor().withContextScript(this::script).create();
    }

    private void script (final Context<I, O> context,
                         final I message)
            throws Throwable
    {
        onMessage(message);
    }

    /**
     * Get the context of the underlying actor.
     *
     * @return the context of the actor.
     */
    public final Context<I, O> context ()
    {
        return actor.context();
    }

    /**
     * Send a message into this pipeline.
     *
     * @param message will be sent.
     */
    public final void sendTo (final I message)
    {
        context().sendTo(message);
    }

    /**
     * Send a message out of this pipeline.
     *
     * @param message will be sent.
     */
    public final void sendFrom (final O message)
    {
        context().sendFrom(message);
    }

    /**
     * Send a message into this pipeline, if possible.
     *
     * @param message will be sent.
     * @return true, if the message was sent successfully.
     */
    public final boolean offerTo (final I message)
    {
        return context().offerTo(message);
    }

    /**
     * Send a message out of this pipeline, if possible.
     *
     * @param message will be sent.
     * @return true, if the message was sent successfully.
     */
    public final boolean offerFrom (final O message)
    {
        return context().offerFrom(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Input<I> dataIn ()
    {
        return actor.input();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Output<O> dataOut ()
    {
        return actor.output();
    }
}
