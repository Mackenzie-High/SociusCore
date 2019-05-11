package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade.Stage;

/**
 *
 * @author mackenzie
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
