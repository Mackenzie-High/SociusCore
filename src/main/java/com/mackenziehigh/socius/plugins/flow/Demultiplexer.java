package com.mackenziehigh.socius.plugins.flow;

import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 *
 */
public final class Demultiplexer<K, M>
{
    private final Cascade.Stage stage;

    private final Processor<Map.Entry<K, M>> input;

    private final Map<K, Processor<M>> outputs = Maps.newConcurrentMap();

    private Demultiplexer (final Cascade.Stage stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.input = Processor.newProcessor(stage, this::onMessage);
    }

    private void onMessage (final Entry<K, M> message)
    {
        /**
         * Get the destination directly from the map,
         * instead of calling dataOut(), which is faster
         * due to the avoidance of an allocation.
         * In addition, this ensures that a memory-leak
         * cannot occur due to a sender sending messages
         * with random keys.
         */
        final Processor<M> dest = outputs.get(message.getKey());

        if (dest != null)
        {
            dest.dataIn().send(message.getValue());
        }
    }

    public Input<Map.Entry<K, M>> dataIn ()
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
