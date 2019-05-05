package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Collection;

/**
 * A set of <code>DataPipeline</code>s arranged into a tower-like structure.
 *
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public interface DataTower<I, O>
        extends DataPipeline<I, O>
{
    /**
     * Get the floors that make up this tower.
     *
     * @return the floors herein.
     */
    public Collection<DataPipeline<I, O>> floors ();

    /**
     * Output Connection.
     *
     * @return the data-output that receives dropped input messages.
     */
    public Output<I> dropsOut ();
}
