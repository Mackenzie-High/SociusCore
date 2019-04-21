package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;

/**
 *
 * @author mackenzie
 */
public interface DataFanout<I>
{
    /**
     * Output Connection.
     *
     * @param key identifies the data-output to return.
     * @return the identified data-output.
     */
    public Output<I> dataOut (Object key);
}
