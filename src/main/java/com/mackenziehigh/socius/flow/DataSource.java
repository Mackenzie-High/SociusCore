package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;

/**
 * An actor that produces messages for other actors to consume.
 *
 * @param <O> is the type of the outgoing messages.
 */
public interface DataSource<O>
{
    /**
     * Output Connection.
     *
     * @return the data-output that provides the outgoing messages.
     */
    public Output<O> dataOut ();
}
