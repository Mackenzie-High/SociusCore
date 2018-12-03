package com.mackenziehigh.socius.flow;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.List;

/**
 * Unpacks the (N) indexed messages from a batch
 * and then forwards them to (N) indexed outputs.
 *
 * <p>
 * Beware: If the arity is (N) and a batch contains (N + 1)
 * or more element messages, then only the first (N) messages
 * will be unpacked. The other messages will be ignored.
 * </p>
 *
 * @param <T> is the type of the incoming and outgoing messages.
 */
public final class Unbatcher<T>
{
    /**
     * Provides the data-input connector.
     */
    private final Processor<List<T>> input;

    /**
     * Provides the data-output connectors.
     */
    private final ImmutableList<Processor<T>> outputs;

    /**
     * This is the number of outputs.
     */
    private final int arity;

    private Unbatcher (final Stage stage,
                       final int arity)
    {
        Preconditions.checkNotNull(stage, "stage");
        Preconditions.checkArgument(arity >= 0, "arity < 0");
        this.arity = arity;
        this.input = Processor.newConsumer(stage, this::onMessage);
        final ImmutableList.Builder<Processor<T>> builder = ImmutableList.builder();

        for (int i = 0; i < arity; i++)
        {
            builder.add(Processor.newConnector(stage));
        }

        this.outputs = builder.build();
    }

    private void onMessage (final List<T> batch)
    {
        int i = 0;

        for (T item : batch)
        {
            if (i < arity)
            {
                final Processor<T> output = outputs.get(i++);
                output.dataIn().send(item);
            }
        }
    }

    /**
     * Input Connection.
     *
     * @return the input that provides the batches to unpack.
     */
    public Input<List<T>> dataIn ()
    {
        return input.dataIn();
    }

    /**
     * Output Connection.
     *
     * @param index identifies the desired connector.
     * @return the indexed output connector.
     */
    public Output<T> dataOut (final int index)
    {
        return outputs.get(index).dataOut();
    }

    /**
     * Get the number of output connectors.
     *
     * @return the number of outputs.
     */
    public int arity ()
    {
        return arity;
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @param arity will be the number of element messages in each batch.
     * @return the new object.
     */
    public static <T> Unbatcher<T> newUnbatcher (final Stage stage,
                                                 final int arity)
    {
        return new Unbatcher<>(stage, arity);
    }
}
