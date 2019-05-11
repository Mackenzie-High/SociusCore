package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * A <code>DataTower</code> that routes incoming messages, in constant-time,
 * to the appropriate floors based on keys obtained from the messages.
 *
 * @param <K> is the type of the keys, which identify the floors.
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public final class TableTower<K, I, O>
        implements DataTower<I, O>
{

    /**
     * This function will extract a key, from an incoming message,
     * which will be used to route the message to the corresponding floor.
     */
    private final Function<I, K> keyFunction;

    /**
     * This actor routes incoming messages to the appropriate floor.
     */
    private final Processor<I> inputConnector;

    /**
     * This actor provides the data-out connector.
     */
    private final Processor<O> outputConnector;

    /**
     * This actor provides the drops-out connector.
     */
    private final Processor<I> dropsConnector;

    /**
     * This map maps user-defined keys to user-defined floors.
     */
    private final ConcurrentMap<K, Pipeline<I, O>> floors = new ConcurrentHashMap<>();

    /**
     * This is merely an unmodifiable version of the floors map,
     * which can be provided to external code.
     */
    private final Map<K, Pipeline<I, O>> floorMap = Collections.unmodifiableMap(floors);

    private TableTower (final Builder<K, I, O> builder)
    {
        this.inputConnector = Processor.fromConsumerScript(builder.stage, this::onInput);
        this.outputConnector = Processor.fromIdentityScript(builder.stage);
        this.dropsConnector = Processor.fromIdentityScript(builder.stage);
        this.keyFunction = builder.keyFunction;

        for (Entry<K, Pipeline<I, O>> floor : builder.floors.entrySet())
        {
            addFloor(floor.getKey(), floor.getValue());
        }
    }

    private void onInput (final I message)
    {
        /**
         * Given the incoming message, obtain the key,
         * which identifies the corresponding floor.
         */
        final K key = keyFunction.apply(message);

        /**
         * Obtain that floor, if any.
         */
        final Pipeline<I, O> floor = floors.get(key);

        /**
         * If the floor does not exist, then drop the message;
         * otherwise, send the message to the floor for processing.
         */
        if (floor == null)
        {
            dropsConnector.accept(message);
        }
        else
        {
            floor.accept(message);
        }
    }

    /**
     * Factory method.
     *
     * @param <K> is the type of the keys, which identify the floors.
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     * @param stage will be used to create private actors.
     * @return a new builder.
     */
    public static <K, I, O> Builder<K, I, O> newTableTower (final Stage stage)
    {
        return new Builder<>(stage);
    }

    /**
     * Add a floor to the tower.
     *
     * @param key identifies the floor.
     * @param floor is the floor itself.
     * @return this.
     */
    public TableTower<K, I, O> addFloor (final K key,
                                         final Pipeline<I, O> floor)
    {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(floor, "floor");
        floor.dataOut().connect(outputConnector.dataIn());
        floors.put(key, floor);
        return this;
    }

    /**
     * Remove a floor from the tower.
     *
     * @param key identifies the floor.
     * @return this.
     */
    public TableTower<K, I, O> removeFloor (final K key)
    {
        Objects.requireNonNull(key, "key");

        final Pipeline<I, O> floor = floors.remove(key);

        if (floor != null)
        {
            floor.dataOut().disconnect(outputConnector.dataIn());
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Pipeline<I, O>> floors ()
    {
        return floorMap().values();
    }

    /**
     * {@inheritDoc}
     */
    public Map<K, Pipeline<I, O>> floorMap ()
    {
        return floorMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input<I> dataIn ()
    {
        return inputConnector.dataIn();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Output<O> dataOut ()
    {
        return outputConnector.dataOut();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Output<I> dropsOut ()
    {
        return dropsConnector.dataOut();
    }

    /**
     * Builder.
     *
     * @param <K> is the type of the keys, which identify the floors.
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     */
    public static final class Builder<K, I, O>
    {
        private final Stage stage;

        private Function<I, K> keyFunction;

        private final Map<K, Pipeline<I, O>> floors = new HashMap<>();

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        /**
         * Specify a function that can obtain keys, from input messages,
         * which can then be used to route the message to a floor.
         *
         * @param functor is the key-function itself.
         * @return this.
         */
        public Builder<K, I, O> withKeyFunction (final Function<I, K> functor)
        {
            this.keyFunction = Objects.requireNonNull(functor, "functor");
            return this;
        }

        /**
         * Add a floor to the tower.
         *
         * @param key identifies the floor.
         * @param floor is the floor itself.
         * @return this.
         */
        public Builder<K, I, O> withFloor (final K key,
                                           final Pipeline<I, O> floor)
        {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(floor, "floor");
            this.floors.put(key, floor);
            return this;
        }

        /**
         * Build the tower.
         *
         * @return the new tower.
         */
        public TableTower<K, I, O> build ()
        {
            Objects.requireNonNull(keyFunction, "keyFunction");
            return new TableTower<>(this);
        }
    }
}
