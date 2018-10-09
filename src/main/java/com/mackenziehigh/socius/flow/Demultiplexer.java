package com.mackenziehigh.socius.flow;

import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.Multiplexer.Message;
import java.util.Map;
import java.util.Objects;

/**
 *
 */
public final class Demultiplexer<K, M>
{
    private final Stage stage;

    private final Processor<Message<K, M>> input;

    private final Map<K, Processor<M>> outputs = Maps.newConcurrentMap();

    private Demultiplexer (final Stage stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.input = Processor.newProcessor(stage, this::onMessage);
    }

    private void onMessage (final Message<K, M> message)
    {
        /**
         * Get the destination directly from the map,
         * instead of calling dataOut(), which is faster
         * due to the avoidance of an allocation.
         * In addition, this ensures that a memory-leak
         * cannot occur due to a sender sending messages
         * with random keys.
         */
        final Processor<M> dest = outputs.get(message.key());

        if (dest != null)
        {
            dest.dataIn().send(message.message());
        }
    }

    public Input<Message<K, M>> dataIn ()
    {
        return input.dataIn();
    }

    public Output<M> dataOut (final K key)
    {
        outputs.putIfAbsent(key, Processor.newProcessor(stage));
        return outputs.get(key).dataOut();
    }

    public static <K, M> Demultiplexer<K, M> newDemultiplexer (final Cascade.Stage stage)
    {
        return new Demultiplexer<>(stage);
    }
}
