package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;

/**
 * An actor that sends messages to a set of named outputs.
 *
 * @param <O> is the type of the outgoing messages.
 */
public interface DataFanout<O>
{
    /**
     * Output Connection.
     *
     * @param key identifies the data-output to return.
     * @return the identified data-output.
     */
    public Output<O> dataOut (Object key);
}
