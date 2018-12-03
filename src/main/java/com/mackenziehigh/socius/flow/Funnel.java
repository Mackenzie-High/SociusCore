package com.mackenziehigh.socius.flow;

import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Map;
import java.util.Objects;

/**
 * Funnels messages from multiple inputs into a single output.
 *
 * @param <T> is the type of the messages passing through the funnel.
 */
public final class Funnel<T>
{
    private final Stage stage;

    private final Processor<T> output;

    private final Map<Object, Processor<T>> inputs = Maps.newConcurrentMap();

    private final Object lock = new Object();

    private Funnel (final Stage stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.output = Processor.newConnector(stage);
    }

    /**
     * Output Connection.
     *
     * @return the output of the funnel.
     */
    public Output<T> dataOut ()
    {
        return output.dataOut();
    }

    /**
     * Input Connection.
     *
     * @param key identifies the specific input.
     * @return the identified input to the funnel.
     */
    public Input<T> dataIn (final Object key)
    {
        /**
         * This is synchronized in order to prevent two processors
         * being created for the same key inadvertently.
         */
        synchronized (lock)
        {
            if (inputs.containsKey(key) == false)
            {
                final Input<T> connector = output.dataIn();
                final Processor<T> actor = Processor.newConsumer(stage, connector::send);
                inputs.put(key, actor);
            }
        }

        return inputs.get(key).dataIn();
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of the messages passing through the funnel.
     * @param stage will be used to create private actors.
     * @return the new funnel.
     */
    public static <T> Funnel<T> newFunnel (final Stage stage)
    {
        return new Funnel<>(stage);
    }
}
