package com.mackenziehigh.socius.plugins.flow;

import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Map;

/**
 * Fans out messages from a single input to multiple outputs.
 *
 * @param <T> is the type of the messages passing through the fanout.
 */
public final class Fanout<T>
{
    private final Actor<T, T> recvActor;

    private final Map<Object, Actor<T, T>> outputs = Maps.newConcurrentMap();

    private final Object lock = new Object();

    private Fanout (final Stage stage)
    {
        this.recvActor = stage.newActor().withScript(this::send).create();
    }

    private void send (final T message)
    {
        outputs.values().forEach(x -> x.accept(message));
    }

    /**
     * Input Connection.
     *
     * @return return the input to the fanout.
     */
    public Input<T> dataIn ()
    {
        return recvActor.input();
    }

    /**
     * Output Connection.
     *
     * @param key identifies the data-output to return.
     * @return the identified data-output.
     */
    public Output<T> dataOut (final Object key)
    {
        synchronized (lock)
        {
            if (outputs.containsKey(key) == false)
            {
                final Actor<T, T> identityActor = recvActor.stage().newActor().withScript((T x) -> x).create();
                outputs.put(key, identityActor);
            }
        }

        return outputs.get(key).output();
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of messages passing through the fanout.
     * @param stage will be used to create private actors.
     * @return the new fanout.
     */
    public static <T> Fanout<T> newFanout (final Stage stage)
    {
        return new Fanout<>(stage);
    }
}
