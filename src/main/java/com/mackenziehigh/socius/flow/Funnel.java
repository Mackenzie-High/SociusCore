package com.mackenziehigh.socius.flow;

import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Map;

/**
 * Funnels messages from multiple inputs into a single output.
 *
 * @param <T> is the type of the messages passing through the funnel.
 */
public final class Funnel<T>
{
    private final Actor<T, T> recvActor;

    private final Map<Object, Actor<T, T>> inputs = Maps.newConcurrentMap();

    private final Object lock = new Object();

    private Funnel (final Stage stage)
    {
        this.recvActor = stage.newActor().withScript((T x) -> x).create();
    }

    private void send (final T message)
    {
        recvActor.accept(message);
    }

    /**
     * Output Connection.
     *
     * @return the output of the funnel.
     */
    public Output<T> dataOut ()
    {
        return recvActor.output();
    }

    /**
     * Input Connection.
     *
     * @param key identifies the specific input.
     * @return the identified input to the funnel.
     */
    public Input<T> dataIn (final Object key)
    {
        synchronized (lock)
        {
            if (inputs.containsKey(key) == false)
            {
                final Cascade.Stage.Actor<T, T> inputActor = recvActor.stage().newActor().withScript(this::send).create();
                inputs.put(key, inputActor);
            }
        }

        return inputs.get(key).input();
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
