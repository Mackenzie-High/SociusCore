package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Filters incoming messages based on a predicate and then
 * forwards only those messages that the predicate accepts.
 *
 * @param <T> is the type of messages flowing through the filter.
 */
public final class Filter<T>
{
    private final Processor<T> actor;

    private final Predicate<T> condition;

    private Filter (final Stage stage,
                    final Predicate<T> condition)
    {
        Objects.requireNonNull(stage, "stage");
        this.condition = Objects.requireNonNull(condition, "condition");
        this.actor = Processor.newConsumer(stage, this::onMessage);
    }

    private T onMessage (final T message)
    {
        return condition.test(message) ? message : null;
    }

    /**
     * Input Connection.
     *
     * @return the input the provides the messages to filter.
     */
    public Input<T> dataIn ()
    {
        return actor.dataIn();
    }

    /**
     * Output Connection.
     *
     * @return the output that will receive messages that the filter accepted.
     */
    public Output<T> dataOut ()
    {
        return actor.dataOut();
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
        return new Filter<>(stage, condition);
    }

}
