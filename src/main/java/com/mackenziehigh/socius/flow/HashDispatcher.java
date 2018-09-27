package com.mackenziehigh.socius.flow;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Collection;
import java.util.Objects;

/**
 *
 * @param <K>
 * @param <M>
 */
public final class HashDispatcher<K, M>
        implements ChannelBilink<K, M>
{
    private final Stage stage;

    private final Table<K, Output<M>, Actor<M, M>> publishers = HashBasedTable.create();

    private final Multimap<K, Input<M>> subscriptions = HashMultimap.create();

    private final Actor<M, M> sinkAll;

    private final Actor<M, M> sinkDead;

    private HashDispatcher (final Stage stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.sinkAll = stage.newActor().withScript((M x) -> x).create();
        this.sinkDead = stage.newActor().withScript((M x) -> x).create();
    }

    public Output<M> sinkAll ()
    {
        return sinkAll.output();
    }

    public Output<M> sinkDead ()
    {
        return sinkDead.output();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized HashDispatcher<K, M> publish (final Actor<?, M> connector,
                                                      final K key)
    {
        return publish(connector.output(), key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized HashDispatcher<K, M> publish (final Actor.Output<M> connector,
                                                      final K key)
    {
        if (publishers.contains(key, connector) == false)
        {
            final Actor<M, M> mediator = stage
                    .newActor()
                    .withScript((M msg) -> mediateSend(key, msg))
                    .create();

            publishers.put(key, connector, mediator);
        }

        final Actor<M, M> mediator = publishers.get(key, connector);

        connector.connect(mediator.input());

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized HashDispatcher<K, M> unpublish (final Actor<?, M> connector,
                                                        final K key)
    {
        return unpublish(connector.output(), key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized HashDispatcher<K, M> unpublish (final Actor.Output<M> connector,
                                                        final K key)
    {
        subscriptions.remove(key, connector);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized HashDispatcher<K, M> subscribe (final Actor<M, ?> connector,
                                                        final K key)
    {
        return subscribe(connector.input(), key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized HashDispatcher<K, M> subscribe (final Actor.Input<M> connector,
                                                        final K key)
    {
        subscriptions.put(key, connector);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized HashDispatcher<K, M> unsubscribe (final Actor<M, ?> connector,
                                                          final K key)
    {
        return unsubscribe(connector.input(), key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized HashDispatcher<K, M> unsubscribe (final Actor.Input<M> connector,
                                                          final K key)
    {
        subscriptions.remove(key, connector);
        return this;
    }

    private void mediateSend (final K key,
                              final M message)
    {
        send(key, message);
    }

    public synchronized HashDispatcher<K, M> send (final K key,
                                                   final M message)
    {
        final Collection<Input<M>> subscribers = subscriptions.get(key);

        for (Input<M> sub : subscribers)
        {
            sub.send(message);
        }

        sinkAll.accept(message);

        if (subscribers.isEmpty())
        {
            sinkDead.accept(message);
        }

        return this;
    }

    public static <K, M> HashDispatcher<K, M> newDispatcher (final Stage stage)
    {
        return new HashDispatcher<>(stage);
    }
}
