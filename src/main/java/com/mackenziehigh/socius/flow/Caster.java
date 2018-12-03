package com.mackenziehigh.socius.flow;

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
public final class Caster<I, O>
{
    private final Class<O> type;

    private final Mapper<I, O> actorCast;

    private final Processor<I> actorFail;

    private Caster (final Stage stage,
                    final Class<O> type)
    {
        Objects.requireNonNull(stage, "stage");
        this.type = Objects.requireNonNull(type, "type");
        this.actorCast = Mapper.newFunction(stage, this::onMessage);
        this.actorFail = Processor.newConnector(stage);
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
    public Input<I> dataIn ()
    {
        return actorCast.dataIn();
    }

    /**
     * Output Connection.
     *
     * @return the output that forwards the messages after the type-cast.
     */
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
    public static <I, O> Caster<I, O> newCaster (final Stage stage,
                                                 final Class<O> type)
    {
        return new Caster<>(stage, type);
    }
}
