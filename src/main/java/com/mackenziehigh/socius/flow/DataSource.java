package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;

/**
 *
 * @author mackenzie
 */
public interface DataSource<O>
{
    /**
     * Output Connection.
     *
     * @return the data-output that receives messages from the pipeline.
     */
    public Output<O> dataOut ();
}
