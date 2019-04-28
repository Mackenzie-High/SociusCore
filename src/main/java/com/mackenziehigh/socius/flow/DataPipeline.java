package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;

/**
 * An actor that consumes messages and then drop/transforms/forwards them to other actors.
 *
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public interface DataPipeline<I, O>
        extends DataSink<I>,
                DataSource<O>
{
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
