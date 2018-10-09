package com.mackenziehigh.socius.flow;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 *
 */
public final class Tower<K, I, O>
{
    public interface Floor<K, I, O>
    {
        public K key ();

        public Input<I> requestsIn ();

        public Output<O> responsesOut ();
    }

    private final Stage stage;

    private final Function<I, K> extractor;

    private final Map<K, Floor<K, I, O>> floors = Maps.newConcurrentMap();

    private final Processor<I> procRequestsIn;

    private final Processor<I> procUnroutableOut;

    private final Funnel<O> funnelResponsesOut;

    private Tower (final Builder<K, I, O> builder)
    {
        this.extractor = builder.extractor;
        this.stage = builder.stage;
        this.procRequestsIn = Processor.newProcessor(stage, this::onMessage);
        this.procUnroutableOut = Processor.newProcessor(stage);
        this.funnelResponsesOut = Funnel.newFunnel(stage);
    }

    private void onMessage (final I message)
    {
        final K key = extractor.apply(message);
        final Floor<K, I, O> floor = floors.get(key);

        if (floor == null)
        {
            procUnroutableOut.dataIn().send(message);
        }
        else
        {
            floor.requestsIn().send(message);
        }
    }

    public Input<I> requestsIn ()
    {
        return procRequestsIn.dataIn();
    }

    public Output<O> responsesOut ()
    {
        return funnelResponsesOut.dataOut();
    }

    public Output<I> unroutableOut ()
    {
        return procUnroutableOut.dataOut();
    }

    public Tower<K, I, O> addFloor (final Floor<K, I, O> floor)
    {
        Objects.requireNonNull(floor, "floor");
        Objects.requireNonNull(floor.key(), "floor.key");
        floor.responsesOut().connect(funnelResponsesOut.dataIn(floor.key()));
        floors.put(floor.key(), floor);
        return this;
    }

    public Tower<K, I, O> removeFloor (final K key)
    {
        Objects.requireNonNull(key, "key");
        final Floor<K, I, O> floor = floors.remove(key);
        if (floor != null)
        {
            floor.responsesOut().disconnect(funnelResponsesOut.dataIn(key));
        }
        return this;
    }

    public Set<Floor<K, I, O>> floors ()
    {
        return ImmutableSet.copyOf(floors.values());
    }

    public static <K, I, O> Builder<K, I, O> newTower (final Stage stage)
    {
        return new Builder<>(stage);
    }

    public static final class Builder<K, I, O>
    {
        private final Stage stage;

        private Function<I, K> extractor;

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        public Builder<K, I, O> withKeyExtractor (final Function<I, K> functor)
        {
            this.extractor = Objects.requireNonNull(extractor, "functor");
            return this;
        }

        public Tower<K, I, O> build ()
        {
            return new Tower<>(this);
        }
    }

}
