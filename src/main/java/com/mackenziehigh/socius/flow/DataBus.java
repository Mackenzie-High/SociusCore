package com.mackenziehigh.socius.flow;

/**
 *
 * @author mackenzie
 */
public interface DataBus<I, O>
        extends DataFunnel<I>,
                DataFanout<O>
{
    // Pass.
}
