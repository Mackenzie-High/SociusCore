package com.mackenziehigh.socius.flow;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Select one message from each of (N) data-inputs,
 * combines the selections into a single messages,
 * and then forwards the combined message.
 *
 * @param <T> is the type of the messages flowing through.
 */
public final class Batcher<T>
{
    private final Processor<List<T>> dataOut;

    private final List<Processor<T>> dataIn;

    private final List<Queue<T>> queues;

    private Batcher (final Stage stage,
                     final int arity)
    {
        this.dataIn = new ArrayList<>(arity);
        this.queues = new ArrayList<>(arity);
        this.dataOut = Processor.newProcessor(stage);

        for (int i = 0; i < arity; i++)
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

        final boolean readyToSend = queues.stream().noneMatch(x -> x.isEmpty());

        if (readyToSend)
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

    /**
     * The batch will be created from these data-inputs.
     *
     * @param index identifies the data-input.
     * @return the desired data-input.
     */
    public Input<T> dataIn (final int index)
    {
        return dataIn.get(index).dataIn();
    }

    /**
     * The combined messages will be forwarded to this output.
     *
     * <p>
     * The combined messages are immutable lists containing
     * the component messages, such that the <code>Kth</code>
     * message was obtained from the <code>Kth</code> data-input.
     * </p>
     *
     * @return the data-output.
     */
    public Output<List<T>> dataOut ()
    {
        return dataOut.dataOut();
    }

    /**
     * Create a new <code>Batcher</code>.
     *
     * @param <T> is the type of the messages flowing through.
     * @param stage will be used to create private actors.
     * @param arity will be the number of data-inputs.
     * @return the newly constructed object.
     */
    public static <T> Batcher<T> newBatcher (final Stage stage,
                                             final int arity)
    {
        return new Batcher<>(stage, arity);
    }
}
