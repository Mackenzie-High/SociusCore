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
package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A stack of floors (pipelines) that routes incoming messages, in linear-time,
 * to the appropriate floors based on predicates defined for each floor.
 *
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public final class LookupTower<I, O>
        implements Pipeline<I, O>
{

    /**
     * This actor routes incoming messages to the appropriate floor.
     */
    private final Processor<I> inputConnector;

    /**
     * This actor provides the data-out connector.
     */
    private final Funnel<O> outputConnector;

    /**
     * This actor provides the drops-out connector.
     */
    private final Processor<I> dropsConnector;

    /**
     * These are the floors that this tower consists of.
     */
    private final List<PredicatedFloor<I, O>> floors;

    private LookupTower (final Builder<I, O> builder)
    {
        this.inputConnector = Processor.fromConsumerScript(builder.stage, this::onInput);
        this.outputConnector = Funnel.newFunnel(builder.stage);
        this.dropsConnector = Processor.fromIdentityScript(builder.stage);
        this.floors = List.copyOf(builder.floors);

        for (PredicatedFloor<I, O> floor : floors)
        {
            floor.dataOut().connect(outputConnector.dataIn(new Object()));
        }
    }

    private void onInput (final I message)
    {

        /**
         * Iterate through each of the floors until either one is found that
         * is willing to accept the message or the last floor is reached.
         */
        for (int i = 0; i < floors.size(); i++)
        {
            var floor = floors.get(i);

            if (floor.test(message))
            {
                floor.accept(message);
                return;
            }
        }

        /**
         * No floor was willing to accept the message.
         */
        dropsConnector.accept(message);
    }

    /**
     * Factory method.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     * @param stage will be used to create private actors.
     * @return a new builder.
     */
    public static <I, O> Builder<I, O> newLookupTower (final Stage stage)
    {
        return new Builder<>(stage);
    }

    /**
     * {@inheritDoc}
     */
    public List<PredicatedFloor<I, O>> floors ()
    {
        return floors;
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

    private static <I, O> PredicatedFloor newPredicatedFloor (final Predicate<I> condition,
                                                              final Pipeline<I, O> floor)
    {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(floor, "floor");

        final PredicatedFloor<I, O> wrapper = new PredicatedFloor<I, O>()
        {
            @Override
            public boolean test (final I message)
            {
                return condition.test(message);
            }

            @Override
            public Input<I> dataIn ()
            {
                return floor.dataIn();
            }

            @Override
            public Output<O> dataOut ()
            {
                return floor.dataOut();
            }
        };

        return wrapper;
    }

    /**
     * A <code>DataPipeline</code> that only conditionally accepts messages.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     */
    public interface PredicatedFloor<I, O>
            extends Pipeline<I, O>
    {
        /**
         * Determine whether this floor is willing to handle the message.
         *
         * @param message is the message that this floor may or may-not accept.
         * @return true, only if this floor shall accept the given message.
         */
        public boolean test (I message);
    }

    /**
     * Builder.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     */
    public static final class Builder<I, O>
    {
        private final Stage stage;

        private final List<PredicatedFloor<I, O>> floors = new LinkedList<>();

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        /**
         * Add a floor to the tower.
         *
         * @param condition will determine whether the floor should handle given messages.
         * @param floor will only receive the messages that the condition approves of.
         * @return this.
         */
        public Builder<I, O> withFloor (final Predicate<I> condition,
                                        final Pipeline<I, O> floor)
        {
            withFloor(newPredicatedFloor(condition, floor));
            return this;
        }

        /**
         * Add a floor to the tower.
         *
         * @param floor will be added to the tower.
         * @return this.
         */
        public Builder<I, O> withFloor (final PredicatedFloor<I, O> floor)
        {
            Objects.requireNonNull(floor, "floor");
            this.floors.add(floor);
            return this;
        }

        /**
         * Build the tower.
         *
         * @return the new tower.
         */
        public LookupTower<I, O> build ()
        {
            return new LookupTower<>(this);
        }
    }
}
