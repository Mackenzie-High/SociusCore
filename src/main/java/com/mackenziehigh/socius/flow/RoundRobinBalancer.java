package com.mackenziehigh.socius.flow;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A Load Balancer that uses a Round Robin Algorithm.
 */
public final class RoundRobinBalancer<T>
{
    private final Actor<T, T> input;

    private final Actor<T, T> hub;

    private final ArrayList<Actor<T, T>> outputs;

    private final Iterator<Actor<T, T>> iter;

    private RoundRobinBalancer (final Stage stage,
                                final int arity)
    {
        this.input = stage.newActor().withScript(this::forwardToHub).create();
        this.hub = stage.newActor().withScript(this::forwardFromHub).create();
        this.outputs = new ArrayList<>(arity);
        this.iter = Iterables.cycle(outputs).iterator();

        for (int i = 0; i < arity; i++)
        {
            outputs.add(stage.newActor().withScript((T x) -> x).create());
        }
    }

    public Input<T> dataIn ()
    {
        return input.input();
    }

    public Output<T> dataOut (final int index)
    {
        return outputs.get(index).output();
    }

    public int arity ()
    {
        return outputs.size();
    }

    private void forwardToHub (final T message)
    {
        hub.accept(message);
    }

    private void forwardFromHub (final T message)
    {
        iter.next().accept(message);
    }

    public static <T> RoundRobinBalancer<T> newBalancer (final Stage stage,
                                                         final int arity)
    {
        Preconditions.checkArgument(arity > 0, "arity <= 0");
        return new RoundRobinBalancer<>(stage, arity);
    }
}
