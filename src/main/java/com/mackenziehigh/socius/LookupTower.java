package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;

/**
 * A <code>DataTower</code> that routes incoming messages, in linear-time,
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
    private final Processor<O> outputConnector;

    /**
     * This actor provides the drops-out connector.
     */
    private final Processor<I> dropsConnector;

    /**
     * These are the floors that this tower consists of.
     */
    private final Deque<PredicatedFloor<I, O>> floors = new ConcurrentLinkedDeque<>();

    private LookupTower (final Builder<I, O> builder)
    {
        this.inputConnector = Processor.fromConsumerScript(builder.stage, this::onInput);
        this.outputConnector = Processor.fromIdentityScript(builder.stage);
        this.dropsConnector = Processor.fromIdentityScript(builder.stage);
        this.floors.addAll(builder.floors);
    }

    private void onInput (final I message)
    {

        /**
         * Iterate through each of the floors until either one is found that
         * is willing to accept the message or the last floor is reached.
         */
        for (PredicatedFloor<I, O> floor : floors)
        {
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
    public static <I, O> Builder<I, O> newTableTower (final Stage stage)
    {
        return new Builder<>(stage);
    }

    /**
     * Add a floor to the tower.
     *
     * @param condition will determine whether the floor should handle given messages.
     * @param floor will only receive the messages that the condition approves of.
     * @return this.
     */
    public LookupTower<I, O> pushFloor (final Predicate<I> condition,
                                        final Pipeline<I, O> floor)
    {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(floor, "floor");
        floor.dataOut().connect(outputConnector.dataIn());
        floors.push(newPredicatedFloor(condition, floor));
        return this;
    }

    /**
     * Remove a floor from the tower.
     *
     * @return this.
     */
    public LookupTower<I, O> popFloor ()
    {
        final Pipeline<I, O> floor = floors.pop();
        floor.dataOut().disconnect(outputConnector.dataIn());
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Pipeline<I, O>> floors ()
    {
        return Collections.unmodifiableList(new ArrayList<>(floors));
    }

    /**
     * {@inheritDoc}
     */
    public Input<I> dataIn ()
    {
        return inputConnector.dataIn();
    }

    /**
     * {@inheritDoc}
     */
    public Output<O> dataOut ()
    {
        return outputConnector.dataOut();
    }

    /**
     * {@inheritDoc}
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

        private final Deque<PredicatedFloor<I, O>> floors = new ArrayDeque<>();

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