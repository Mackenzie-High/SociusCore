package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;

/**
 * An actor that receives messages from a set of named inputs.
 *
 * @param <I> is the type of the incoming messages.
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
