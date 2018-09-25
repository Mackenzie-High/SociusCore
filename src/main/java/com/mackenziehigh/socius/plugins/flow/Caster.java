package com.mackenziehigh.socius.plugins.flow;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;

/**
 * Casts messages from one type to another and then forwards them.
 *
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public final class Caster<I, O>
{
    private final Actor<I, O> actor;

    private Caster (final Actor<I, O> actor)
    {
        this.actor = actor;
    }

    /**
     * Input Connection.
     *
     * @return the input that receives the messages to cast.
     */
    public Input<I> dataIn ()
    {
        return actor.input();
    }

    /**
     * Output Connection.
     *
     * @return the output that merely forwards the messages from data-in.
     */
    public Output<O> dataOut ()
    {
        return actor.output();
    }

    /**
     * Factory Method.
     *
     * @param <X> is the type of the incoming messages.
     * @param <Y> is the type of the outgoing messages.
     * @param stage will be used to create private actors.
     * @param type is the type of the outgoing messages.
     * @return the new converter.
     */
    public static <X, Y> Caster<X, Y> newCaster (final Stage stage,
                                                 final Class<Y> type)
    {
        final Actor<X, Y> actor = stage.newActor().withScript((X x) -> type.cast(x)).create();
        return new Caster<>(actor);
    }
}
