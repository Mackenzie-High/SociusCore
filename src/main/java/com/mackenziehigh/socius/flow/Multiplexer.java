package com.mackenziehigh.socius.flow;

import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 *
 */
public final class Multiplexer<K, M>
{
    private final Stage stage;

    private final Map<K, Processor<M>> inputs = Maps.newConcurrentMap();

    private final Processor<Entry<K, M>> output;

    private Multiplexer (final Stage stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.output = Processor.newProcessor(stage);
    }

    public Input<M> dataIn (final K key)
    {
        inputs.putIfAbsent(key, Processor.newProcessor(stage, (M msg) -> onMessage(key, msg)));
        return inputs.get(key).dataIn();
    }

    private void onMessage (final K key,
                            final M message)
    {
        final Entry<K, M> entry = new AbstractMap.SimpleImmutableEntry<>(key, message);
        output.dataIn().send(entry);
    }

    public Output<Entry<K, M>> dataOut ()
    {
        return output.dataOut();
    }

    public static <K, M> Multiplexer<K, M> newMultiplexer (final Stage stage)
    {
        return new Multiplexer<>(stage);
    }
}
