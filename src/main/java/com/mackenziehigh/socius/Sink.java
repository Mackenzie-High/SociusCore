package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import java.util.function.Consumer;

/**
 * An actor that consumes incoming messages.
 *
 * <p>
 * This method implements that <code>java.util.function.Consumer</code> interface in
 * order to allow the actor to be used as the output destination <code>Stream</code>s,
 * which may be occasionally convenient when interacting with third-party APIs.
 * </p>
 *
 * @param <I> is the type of the incoming messages.
 */
public interface Sink<I>
        extends Consumer<I>
{
    /**
     * {@inheritDoc}
     */
    @Override
    public default void accept (I message)
    {
        dataIn().send(message);
    }

    /**
     * Input Connection.
     *
     * @return the data-input that provides the messages to the pipeline.
     */
    public Input<I> dataIn ();
}
