package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade.Stage;
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

    protected abstract void onMessage (I message)
            throws Throwable;

    private final Actor<I, O> actor;

    protected AbstractPipeline (final Stage stage)
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

    public final Context<I, O> context ()
    {
        return actor.context();
    }

    public final void sendTo (final I message)
    {
        context().sendTo(message);
    }

    public final void sendFrom (final O message)
    {
        context().sendFrom(message);
    }

    public final boolean offerTo (final I message)
    {
        return context().offerTo(message);
    }

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
    public Output<O> dataOut ()
    {
        return actor.output();
    }
}
