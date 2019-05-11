package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A <code>DataTower</code> that processes each incoming message on a new floor.
 *
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public final class ServerTower<I, O>
        implements DataTower<I, O>
{
    private final Stage stage;

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
     * This function will be used to create new floors, as needed.
     */
    private final Supplier<Pipeline<I, O>> factory;

    /**
     * A floor will be destroyed, when this predicate evaluates to true.
     */
    private final Predicate<O> terminationCondition;

    /**
     * This tower can never have more than this number of floors simultaneously.
     */
    private final int maximumFloorCount;

    /**
     * These are the floors that this tower consists of.
     */
    private final Set<Pipeline<I, O>> floors = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * These are the floors that this tower consists of, as an unmodifiable collection.
     */
    private final Set<Pipeline<I, O>> unmodFloors = Collections.unmodifiableSet(floors);

    private ServerTower (final Builder<I, O> builder)
    {
        this.stage = builder.stage;
        this.inputConnector = Processor.fromConsumerScript(builder.stage, this::onInput);
        this.outputConnector = Processor.fromIdentityScript(builder.stage);
        this.dropsConnector = Processor.fromIdentityScript(builder.stage);
        this.factory = builder.factory;
        this.terminationCondition = builder.terminationCondition;
        this.maximumFloorCount = builder.maximumFloorCount;
    }

    private void onInput (final I message)
    {
        /**
         * If too many floors exist, then drop the message.
         */
        if (floors.size() >= maximumFloorCount)
        {
            dropsConnector.accept(message);
            return;
        }

        /**
         * Create a floor to handle the incoming message.
         */
        final Pipeline<I, O> floor = factory.get();

        /**
         * This actor will monitor the outputs of the floor.
         * Once the termination condition is satisfied,
         * then the actor will destroy the floor.
         */
        final Processor<O> terminator = Processor.fromConsumerScript(stage, msg -> onOutput(floor, msg));
        floor.dataOut().connect(terminator.dataIn());

        /**
         * Keep track of the floors that currently exist,
         * so that the floors() method can be implemented.
         */
        floors.add(floor);

        /**
         * Send the message to the new floor for processing.
         */
        floor.accept(message);
    }

    private void onOutput (final Pipeline<I, O> floor,
                           final O message)
    {
        /**
         * If the termination condition has been satisfied,
         * then go ahead and destroy the floor.
         */
        if (terminationCondition.test(message))
        {
            floors.remove(floor);
        }

        /**
         * Send all messages from the floor out of the tower.
         */
        outputConnector.accept(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Pipeline<I, O>> floors ()
    {
        return unmodFloors;
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
     * Builder.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     */
    public static final class Builder<I, O>
    {
        private final Stage stage;

        private Supplier<Pipeline<I, O>> factory;

        private Predicate<O> terminationCondition = x -> true;

        private int maximumFloorCount = Integer.MAX_VALUE;

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        /**
         * Specify a predicate that will identify the last message produced by each floor.
         *
         * @param condition identifies the termination messages.
         * @return this.
         */
        public Builder<I, O> withTerminationWhen (final Predicate<O> condition)
        {
            this.terminationCondition = Objects.requireNonNull(condition, "condition");
            return this;
        }

        /**
         * Specify a message that will signal that a floor can be destroyed.
         *
         * @param message signals the end-of-life of a floor.
         * @return this.
         */
        public Builder<I, O> withTerminationAt (final O message)
        {
            withTerminationWhen(msg -> Objects.equals(msg, message));
            return this;
        }

        /**
         * Specify the maximum number of floors that can simultaneously exist.
         *
         * @param count is the maximum height of this tower.
         * @return this.
         */
        public Builder<I, O> withMaximumFloorCount (final int count)
        {
            this.maximumFloorCount = count;
            return this;
        }

        /**
         * Build the tower.
         *
         * @return the new tower.
         */
        public ServerTower<I, O> build ()
        {
            Objects.requireNonNull(factory, "factory");
            return new ServerTower<>(this);
        }
    }
}
