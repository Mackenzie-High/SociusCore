package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;

/**
 *
 * @author mackenzie
 */
public interface DataFunnel<I>
{
    /**
     * Input Connection.
     *
     * @param key identifies the specific input.
     * @return the identified input to the funnel.
     */
    public Input<I> dataIn (Object key);
}
