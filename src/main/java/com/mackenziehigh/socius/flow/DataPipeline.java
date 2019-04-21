package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;

/**
 *
 */
public interface DataPipeline<I, O>
        extends DataSink<I>,
                DataSource<O>
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
    @Override
    public Input<I> dataIn ();

    /**
     * Output Connection.
     *
     * @return the data-output that receives messages from the pipeline.
     */
    @Override
    public Output<O> dataOut ();
}
