package com.mackenziehigh.socius.plugins.flow;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;

/**
 *
 */
public final class Caster<I, O>
{
    private final Actor<I, O> actor;

    private Caster (final Actor<I, O> actor)
    {
        this.actor = actor;
    }

    public static <X, Y> Caster<X, Y> newCaster (final Stage stage,
                                                 final Class<Y> type)
    {
        final Actor<X, Y> actor = stage.newActor().withScript((X x) -> type.cast(x)).create();
        return new Caster<>(actor);
    }
}
