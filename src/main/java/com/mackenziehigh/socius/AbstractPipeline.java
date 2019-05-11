package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Context;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Objects;

/**
 *
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
