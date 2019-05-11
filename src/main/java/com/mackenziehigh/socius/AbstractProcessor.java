package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade.Stage;

/**
 * Facilitates easy implementation of a <code>Processor</code> via sub-classing.
 *
 * @param <T> is the type of the incoming and outgoing messages.
 */
public abstract class AbstractProcessor<T>
        extends AbstractPipeline<T, T>
        implements Processor<T>
{
    protected AbstractProcessor (Stage stage)
    {
        super(stage);
    }
}
