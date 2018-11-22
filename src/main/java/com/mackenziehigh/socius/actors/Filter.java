package com.mackenziehigh.socius.actors;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.function.Predicate;

/**
 * Filters incoming messages based on a predicate and
 * forwards only those messages that the predicate accepts.
 *
 * @param <T> is the type of messages flowing through the filter.
 */
public final class Filter<T>
{
    private final Actor<T, T> actor;

    private Filter (final Actor<T, T> actor)
    {
        this.actor = actor;
    }

    /**
     * Input Connection.
     *
     * @return the input the provides the messages to filter.
     */
    public Input<T> dataIn ()
    {
        return actor.input();
    }

    /**
     * Output Connection.
     *
     * @return the output that will receive messages that the filter accepted.
     */
    public Output<T> dataOut ()
    {
        return actor.output();
    }

    /**
     * Factory Method.
     *
     * @param <T> is type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @param condition determines whether a message should be allowed through the filter.
     * @return this.
     */
    public static <T> Filter<T> newFilter (final Stage stage,
                                           final Predicate<T> condition)
    {
        return new Filter<>(stage.newActor().withScript((T x) -> condition.test(x) ? x : null).create());
    }

}
