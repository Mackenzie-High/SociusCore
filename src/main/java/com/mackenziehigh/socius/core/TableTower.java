/*
 * Copyright 2019 Michael Mackenzie High
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mackenziehigh.socius.core;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A stack of floors (pipelines) that routes incoming messages, in constant-time,
 * to the appropriate floors based on keys obtained from the messages.
 *
 * @param <K> is the type of the keys, which identify the floors.
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public final class TableTower<K, I, O>
        implements Pipeline<I, O>
{

    /**
     * This function will extract a key, from an incoming message,
     * which will be used to route the message to the corresponding floor.
     */
    private final Function<I, K> keyFunction;

    /**
     * If this tower is capable of automatically adding floors,
     * then this factory will be used to create those floors.
     */
    private final Function<I, ? extends Pipeline<I, O>> autoAdder;

    /**
     * If this tower is capable of automatically removing floors,
     * then this function will be used to identify messages
     * that are intended to trigger such removals.
     */
    private final Predicate<I> autoRemover;

    /**
     * This is the maximum number of floors that can be in the tower at once.
     */
    private final int maximumFloorCount;

    /**
     * This key, if any, identifies messages destined for all the floors.
     */
    private final K broadcastKey;

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

    /**
     * This lock prevents duplicate floors from being added or removed.
     * This lock is almost always non-contended, so the performance impact is minimal.
     */
    private final Object lock = new Object();

    private TableTower (final Builder<K, I, O> builder)
    {
        this.inputConnector = Processor.fromConsumerScript(builder.stage, this::onInput);
        this.outputConnector = Processor.fromIdentityScript(builder.stage);
        this.dropsConnector = Processor.fromIdentityScript(builder.stage);
        this.keyFunction = builder.keyFunction;
        this.autoAdder = builder.autoAdder;
        this.autoRemover = builder.autoRemover;
        this.maximumFloorCount = builder.maximumFloorCount;
        this.broadcastKey = builder.broadcastKey;

        for (var floor : builder.floors.entrySet())
        {
            add(floor.getKey(), floor.getValue());
        }
    }

    private void onInput (final I message)
    {
        synchronized (lock)
        {
            /**
             * Given the incoming message, obtain the key,
             * which identifies the corresponding floor.
             */
            final K key = keyFunction.apply(message);

            /**
             * Is this message destined for deliver to all of the floors?
             */
            final boolean broadcast = Objects.equals(broadcastKey, key);

            if (broadcast)
            {
                sendToAllFloors(message);
            }
            else
            {
                sendToOneFloorOnly(key, message);
            }
        }
    }

    private void sendToAllFloors (final I message)
    {
        for (var floor : floors().values())
        {
            floor.accept(message);
        }
    }

    private void sendToOneFloorOnly (final K key,
                                     final I message)
    {
        /**
         * If the floor does not exist, but this tower is capable
         * of creating new floors on-demand, then create the floor.
         */
        final boolean canAutoAddFloor = autoAdder != null;
        final boolean floorDoesNotExist = !floors.containsKey(key);
        final boolean autoAddFloor = canAutoAddFloor & floorDoesNotExist;

        if (autoAddFloor)
        {
            final Pipeline<I, O> newFloor = autoAdder.apply(message);
            add(key, newFloor);
        }

        /**
         * If this tower is capable of automatically removing floors,
         * then determine whether the message indicates that a floor
         * with the given key needs to be removed.
         */
        final boolean canAutoRemoveFloor = autoRemover != null;
        final boolean removalNeeded = canAutoRemoveFloor && autoRemover.test(message);

        if (removalNeeded)
        {
            remove(key);
        }

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
    public TableTower<K, I, O> add (final K key,
                                    final Pipeline<I, O> floor)
    {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(floor, "floor");

        synchronized (lock)
        {
            if (Objects.equals(broadcastKey, key))
            {
                throw new IllegalArgumentException("A floor cannot be identified by the broadcast-key.");
            }
            else if (floors.size() < maximumFloorCount)
            {
                floor.dataOut().connect(outputConnector.dataIn());
                floors.put(key, floor);
            }
        }

        return this;
    }

    /**
     * Remove a floor from the tower.
     *
     * @param key identifies the floor.
     * @return this.
     */
    public TableTower<K, I, O> remove (final K key)
    {
        Objects.requireNonNull(key, "key");

        synchronized (lock)
        {
            final Pipeline<I, O> floor = floors.remove(key);

            if (floor != null)
            {
                floor.dataOut().disconnect(outputConnector.dataIn());
            }
        }

        return this;
    }

    /**
     * Get the floors contains in this tower.
     *
     * @return an unmodifiable map that maps keys to floors.
     */
    public Map<K, Pipeline<I, O>> floors ()
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
     * Output Connection.
     *
     * @return the output that receives the dropped input messages.
     */
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

        private Function<I, ? extends Pipeline<I, O>> autoAdder;

        private Predicate<I> autoRemover;

        private int maximumFloorCount = Integer.MAX_VALUE;

        private K broadcastKey = null;

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
         * Cause the tower to automatically add new floors, rather than dropping messages,
         * when new keys are encountered for which no corresponding floor exists.
         *
         * <p>
         * This function will not be used to create a floor corresponding to the <i>broadcast key</i>,
         * if any, since broadcasting routes messages to all floors, rather than a single floor.
         * </p>
         *
         * @param functor will be used to create new floors as needed.
         * @return this.
         */
        public Builder<K, I, O> withAutoExpansion (final Function<I, ? extends Pipeline<I, O>> functor)
        {
            this.autoAdder = Objects.requireNonNull(functor, "functor");
            return this;
        }

        /**
         * Cause the tower to automatically remove floors whenever
         * input messages arrive that match this predicate.
         *
         * @param condition will be used to trigger the removal of floors.
         * @return this.
         */
        public Builder<K, I, O> withAutoRemoval (final Predicate<I> condition)
        {
            this.autoRemover = Objects.requireNonNull(condition, "condition");
            return this;
        }

        /**
         * Specify the maximum number of floors that the tower is allowed to have.
         *
         * @param count is the maximum floor capacity of a tower.
         * @return this.
         */
        public Builder<K, I, O> withMaximumFloors (final int count)
        {
            this.maximumFloorCount = count;
            return this;
        }

        /**
         * Specify a key that will identify messages that need to
         * be routed to all of the floors of the tower concurrently.
         *
         * @param key identifies messages destined for all the floors.
         * @return
         */
        public Builder<K, I, O> withBroadcastKey (final K key)
        {
            this.broadcastKey = Objects.requireNonNull(key, "key");
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
