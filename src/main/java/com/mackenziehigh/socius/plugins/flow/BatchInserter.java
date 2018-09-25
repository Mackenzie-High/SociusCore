package com.mackenziehigh.socius.plugins.flow;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * Conditionally selects messages, combines the selections into a batch,
 * and then forwards the batch as a single message.
 *
 * @param <T> is the type of the messages flowing through the inserter.
 */
public final class BatchInserter<T>
{
    private static final boolean ADDED_TO_BATCH = true;

    private static final boolean NOT_ADDED_TO_BATCH = false;

    private final Processor<T> dataIn;

    private final Processor<T> dataOut;

    private final Processor<List<T>> batchOut;

    private final List<Action<T>> actions;

    private final AtomicInteger counter = new AtomicInteger();

    private BatchInserter (final Stage stage,
                           final List<Action<T>> actions)
    {
        this.actions = ImmutableList.copyOf(actions);
        this.dataIn = Processor.newProcessor(stage, this::onMessage);
        this.dataOut = Processor.newProcessor(stage);
        this.batchOut = Processor.newProcessor(stage);
    }

    private void onMessage (final T message)
    {
        counter.set(0);

        boolean received = false;

        /**
         * Try to add the message to the batch.
         */
        for (Action<T> action : actions)
        {
            if (action.receive(message) == ADDED_TO_BATCH)
            {
                action.tally(counter);
                received = ADDED_TO_BATCH;
                break;
            }
        }

        /**
         * If the message was not added to the batch,
         * then forward the message to the data-out.
         */
        if (received == NOT_ADDED_TO_BATCH)
        {
            dataOut.dataIn().send(message);
            return;
        }

        /**
         * If the message was added to the batch and the batch is now full,
         * then go ahead and transmit the batch.
         */
        final boolean batchIsFull = actions.size() == counter.get();

        if (batchIsFull)
        {
            final List<T> batchList = new ArrayList<>(actions.size());

            for (Action<T> action : actions)
            {
                action.append(batchList);
            }

            final List<T> batch = ImmutableList.copyOf(batchList); // TODO: Use list builder.

            batchOut.dataIn().send(batch);
        }
    }

    /**
     * This is the stream of messages from which items will be selected.
     *
     * @return the input connector.
     */
    public Input<T> dataIn ()
    {
        return dataIn.dataIn();
    }

    /**
     * This is the stream of messages that were received, but not selected.
     *
     * @return the output connector.
     */
    public Output<T> dataOut ()
    {
        return dataOut.dataOut();
    }

    /**
     * This is the stream of batches created from selected messages.
     *
     * @return the output connector.
     */
    public Output<List<T>> batchOut ()
    {
        return batchOut.dataOut();
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of messages flowing through the inserter.
     * @param stage will be used to create private actors.
     * @return the new inserter.
     */
    public static <T> Builder<T> newInserter (final Stage stage)
    {
        return new Builder<>(stage);
    }

    /**
     * Builder of Batch Inserter objects.
     *
     * @param <T> is the type of messages that will flow through the inserter.
     */
    public static final class Builder<T>
    {
        private final Stage stage;

        private final List<Action<T>> actions = Lists.newLinkedList();

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        /**
         * Each batch will contain one element that specifically matches this condition.
         *
         * <p>
         * This method may be invoked multiple times, with the same condition,
         * in order to specify multiple required items for a batch.
         * </p>
         *
         * @param condition will identify an item to add to the batch.
         * @return this.
         */
        public Builder<T> require (final Predicate<T> condition)
        {
            actions.add(new Action<>(condition));
            return this;
        }

        /**
         * Build the inserter.
         *
         * @return the new inserter.
         */
        public BatchInserter<T> build ()
        {
            return new BatchInserter<>(stage, actions);
        }
    }

    /**
     * There is one instance of this class per item needed in a batch.
     * Whenever a message is received by the inserter, the receive() method is invoked.
     * If the receive() method determines that the message matches the predicate,
     * then the message will be stored in this object until the batch is ready for creation.
     * Until then, the receive() method will ignore subsequent messages,
     * even if the messages match the predicate, in order to avoid accepting undesired duplicates.
     *
     * @param <T> is the type of the messages flowing through the inserter.
     */
    private static final class Action<T>
    {
        private final Predicate<T> condition;

        private final AtomicReference<T> data = new AtomicReference<>();

        private Action (final Predicate<T> condition)
        {
            this.condition = condition;
        }

        public boolean receive (final T message)
        {
            final boolean alreadyInBatch = data.get() != null;

            if (alreadyInBatch)
            {
                return true;
            }
            else if (condition.test(message))
            {
                data.set(message);
                return ADDED_TO_BATCH;
            }
            else
            {
                return NOT_ADDED_TO_BATCH;
            }
        }

        public void tally (final AtomicInteger counter)
        {
            final boolean alreadyInBatch = data.get() != null;

            if (alreadyInBatch)
            {
                counter.incrementAndGet();
            }
        }

        public void append (final List<T> list)
        {
            list.add(data.get());
        }
    }

}
