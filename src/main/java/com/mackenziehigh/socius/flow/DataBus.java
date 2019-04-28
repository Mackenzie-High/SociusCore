package com.mackenziehigh.socius.flow;

/**
 * An actor that receives messages from a set of named inputs and
 * then broadcasts those messages to a set of named outputs.
 *
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public interface DataBus<I, O>
        extends DataFunnel<I>,
                DataFanout<O>
{
    // Pass.
}
