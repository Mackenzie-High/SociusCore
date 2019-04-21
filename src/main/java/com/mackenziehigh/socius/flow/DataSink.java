package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import java.util.function.Consumer;

/**
 *
 * @author mackenzie
 */
public interface DataSink<I>
        extends Consumer<I>
{
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
