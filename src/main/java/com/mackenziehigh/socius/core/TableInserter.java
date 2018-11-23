package com.mackenziehigh.socius.core;

import com.mackenziehigh.socius.core.Processor;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Conditionally routes messages based on a table lookup.
 *
 * @param <T>
 */
public final class TableInserter<K, T>
{
    private final Stage stage;

    private final Processor<T> procDataIn;

    private final Processor<T> procDataOut;

    private final Map<K, Input<T>> routes = Maps.newConcurrentMap();

    private final Function<T, K> extractor;

    private TableInserter (final Stage stage,
                           final Function<T, K> extractor)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.procDataIn = Processor.newProcessor(stage, this::onMessage);
        this.procDataOut = Processor.newProcessor(stage);
        this.extractor = Objects.requireNonNull(extractor, "extractor");
    }

    private void onMessage (final T message)
    {
        final Object key = extractor.apply(message);

        final Input<T> route = routes.get(key);

        if (route == null)
        {
            procDataOut.dataIn().send(message);
        }
        else
        {
            route.send(message);
        }
    }

    public Input<T> dataIn ()
    {
        return procDataIn.dataIn();
    }

    public Output<T> dataOut ()
    {
        return procDataOut.dataOut();
    }

    public synchronized Output<T> selectIf (final K key)
    {
        Preconditions.checkState(routes.containsKey(key) == false, "Duplicate: selectIf(%s)", key);
        final Processor<T> proc = Processor.newProcessor(stage);
        routes.put(key, proc.dataIn());
        return proc.dataOut();
    }

    public static <K, T> TableInserter<K, T> newTableInserter (final Stage stage,
                                                               final Function<T, K> extractor)
    {
        return new TableInserter(stage, extractor);
    }
}
