package com.mackenziehigh.socius.plugins.flow;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 *
 */
public final class Batcher<T>
{
    private final Processor<List<T>> dataOut;

    private final List<Processor<T>> dataIn;

    private final List<Queue<T>> queues;

    private Batcher (final Stage stage,
                     final int count)
    {
        this.dataIn = new ArrayList<>(count);
        this.queues = new ArrayList<>(count);
        this.dataOut = Processor.newProcessor(stage);

        for (int i = 0; i < count; i++)
        {
            final int idx = i;
            dataIn.add(Processor.newProcessor(stage, (T msg) -> onMessage(idx, msg)));
            queues.add(Queues.newArrayDeque());
        }
    }

    private void onMessage (final int index,
                            final T message)
    {
        queues.get(index).add(message);

        if (queues.stream().noneMatch(x -> x.isEmpty()))
        {
            final List<T> batchList = new ArrayList<>(queues.size());

            for (Queue<T> queue : queues)
            {
                batchList.add(queue.poll());
            }

            final List<T> batch = ImmutableList.copyOf(batchList); // TODO: Use list builder.

            dataOut.dataIn().send(batch);
        }
    }

    public Input<T> dataIn (final int index)
    {
        return dataIn.get(index).dataIn();
    }

    public Output<List<T>> dataOut ()
    {
        return dataOut.dataOut();
    }

    public static <T> Batcher<T> newBatcher (final Stage stage,
                                             final int count)
    {
        return new Batcher<>(stage, count);
    }
}
