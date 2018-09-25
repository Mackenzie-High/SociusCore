package com.mackenziehigh.socius.plugins.flow;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Conditionally forwards messages based on a boolean flag (open|closed).
 */
public final class Valve<T>
{
    private final Actor<T, T> pipe;

    private final Actor<Boolean, Boolean> toggle;

    private final AtomicBoolean flag = new AtomicBoolean();

    private Valve (final Stage stage,
                   final boolean open)
    {
        this.pipe = stage.newActor().withScript(this::onPipeMessage).create();
        this.toggle = stage.newActor().withScript(this::onToggleMessage).create();
        this.flag.set(open);
    }

    private T onPipeMessage (final T message)
    {
        return flag.get() ? message : null;
    }

    private void onToggleMessage (final Boolean message)
    {
        flag.set(message);
    }

    public Input<T> dataIn ()
    {
        return pipe.input();
    }

    public Output<T> dataOut ()
    {
        return pipe.output();
    }

    public Input<Boolean> toggleIn ()
    {
        return toggle.input();
    }

    public Input<Boolean> toggleOut ()
    {
        return toggle.input();
    }

    public static <T> Valve<T> newOpenValve (final Stage stage)
    {
        return new Valve<>(stage, true);
    }

    public static <T> Valve<T> newClosedValve (final Stage stage)
    {
        return new Valve<>(stage, false);
    }
}
