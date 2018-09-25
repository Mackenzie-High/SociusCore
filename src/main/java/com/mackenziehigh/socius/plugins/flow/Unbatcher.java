package com.mackenziehigh.socius.plugins.flow;

import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 */
public final class Unbatcher<T>
{
    private final Stage stage;

    private final Processor<List<T>> dataIn;

    private final Map<Integer, Processor<T>> dataOut = Maps.newConcurrentMap();

    private Unbatcher (final Stage stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.dataIn = Processor.newProcessor(stage, this::onMessage);
    }

    private void onMessage (final List<T> batch)
    {
        int i = 0;

        for (T item : batch)
        {
            final Processor<T> out = dataOut.get(i++);

            if (out != null)
            {
                out.dataIn().send(item);
            }
        }
    }

    public Input<List<T>> dataIn ()
    {
        return dataIn.dataIn();
    }

    public Output<T> dataOut (final int index)
    {
        synchronized (dataOut)
        {
            if (dataOut.containsKey(index) == false)
            {
                dataOut.put(index, Processor.newProcessor(stage));
            }
        }

        return dataOut.get(index).dataOut();
    }

    public static <T> Unbatcher<T> newUnbatcher (final Stage stage)
    {
        return new Unbatcher<>(stage);
    }
}
