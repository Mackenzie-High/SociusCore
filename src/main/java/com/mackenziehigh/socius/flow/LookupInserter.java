package com.mackenziehigh.socius.flow;

import com.google.common.collect.Lists;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Conditionally routes messages based on an ordered series of option predicates.
 */
public final class LookupInserter<T>
{
    private final Stage stage;

    private final Processor<T> procDataIn;

    private final Processor<T> procDataOut;

    private final List<Entry<Predicate<T>, Input<T>>> routes = Lists.newCopyOnWriteArrayList();

    private LookupInserter (final Stage stage)
    {
        this.stage = Objects.requireNonNull(stage);
        this.procDataIn = Processor.newProcessor(stage, this::onMessage);
        this.procDataOut = Processor.newProcessor(stage);
    }

    private void onMessage (final T message)
    {
        for (Entry<Predicate<T>, Input<T>> route : routes)
        {
            if (route.getKey().test(message))
            {
                route.getValue().send(message);
                return;
            }
        }

        procDataOut.dataIn().send(message);
    }

    public Input<T> dataIn ()
    {
        return procDataIn.dataIn();
    }

    public Output<T> dataOut ()
    {
        return procDataOut.dataOut();
    }

    public synchronized Output<T> selectIf (final Predicate<T> condition)
    {
        Objects.requireNonNull(condition, "condition");
        final Processor<T> proc = Processor.newProcessor(stage);
        // Must be sync!
        routes.add(new AbstractMap.SimpleImmutableEntry<>(condition, proc.dataIn()));
        return proc.dataOut();
    }

    public static <T> LookupInserter<T> newLookupInserter (final Stage stage)
    {
        return new LookupInserter(stage);
    }
}
