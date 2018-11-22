package com.mackenziehigh.socius.actors;

import com.mackenziehigh.socius.actors.Processor;
import com.google.common.collect.ImmutableList;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.List;
import java.util.Objects;

/**
 *
 */
public final class Unbatcher<T>
{
    private final Stage stage;

    private final Processor<List<T>> dataIn;

    private final List<Processor<T>> dataOut;

    private Unbatcher (final Stage stage,
                       final int arity)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.dataIn = Processor.newProcessor(stage, this::onMessage);
        final ImmutableList.Builder<Processor<T>> builder = ImmutableList.builder();

        for (int i = 0; i < arity; i++)
        {
            builder.add(Processor.newProcessor(stage));
        }

        this.dataOut = builder.build();
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
        return dataOut.get(index).dataOut();
    }

    public static <T> Unbatcher<T> newUnbatcher (final Stage stage,
                                                 final int arity)
    {
        return new Unbatcher<>(stage, arity);
    }
}
