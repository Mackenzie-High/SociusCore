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
package com.mackenziehigh.socius.flow;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
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
        implements DataPipeline<T, T>
{
    private static final boolean ADDED_TO_BATCH = true;

    private static final boolean NOT_ADDED_TO_BATCH = false;

    /**
     * Provides the data-input connector.
     */
    private final Processor<T> procDataIn;

    /**
     * Provides the data-output connector.
     */
    private final Processor<T> procDataOut;

    /**
     * Provides the batch-output connector.
     */
    private final Processor<List<T>> procBatchOut;

    /**
     * These objects trap and store individual messages,
     * which themselves will be part of the next outgoing batch,
     * until the batch is complete and ready to be sent out.
     */
    private final List<Selector<T>> selectors;

    /**
     * This counter is used to determine when the batch is full.
     */
    private final AtomicInteger batchSize = new AtomicInteger();

    private BatchInserter (final Stage stage,
                           final List<Selector<T>> selectors)
    {
        this.selectors = ImmutableList.copyOf(selectors);
        this.procDataIn = Processor.newConsumer(stage, this::onMessage);
        this.procDataOut = Processor.newConnector(stage);
        this.procBatchOut = Processor.newConnector(stage);
    }

    private void onMessage (final T message)
    {
        boolean received = false;

        /**
         * Try to add the message to the batch.
         */
        for (Selector<T> selector : selectors)
        {
            if (selector.receive(message) == ADDED_TO_BATCH)
            {
                received = ADDED_TO_BATCH;
                batchSize.incrementAndGet();
                break;
            }
        }

        /**
         * If the message was not added to the batch,
         * then forward the message to the data-out.
         */
        if (received == NOT_ADDED_TO_BATCH)
        {
            procDataOut.dataIn().send(message);
            return;
        }

        /**
         * If the message was added to the batch and the batch is now full,
         * then go ahead and transmit the batch.
         */
        final boolean batchIsFull = selectors.size() == batchSize.get();

        if (batchIsFull)
        {
            final ImmutableList.Builder<T> batchList = ImmutableList.builder();

            for (Selector<T> selector : selectors)
            {
                selector.append(batchList);
                selector.clear();
            }

            final ImmutableList<T> batch = batchList.build();

            procBatchOut.dataIn().send(batch);
            batchSize.set(0);
        }
    }

    /**
     * This is the stream of messages from which items will be selected.
     *
     * @return the input connector.
     */
    @Override
    public Input<T> dataIn ()
    {
        return procDataIn.dataIn();
    }

    /**
     * This is the stream of messages that were received, but not selected.
     *
     * @return the output connector.
     */
    @Override
    public Output<T> dataOut ()
    {
        return procDataOut.dataOut();
    }

    /**
     * This is the stream of batches created from selected messages.
     *
     * @return the output connector.
     */
    public Output<List<T>> batchOut ()
    {
        return procBatchOut.dataOut();
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of messages flowing through the inserter.
     * @param stage will be used to create private actors.
     * @return a builder that can create a new inserter.
     */
    public static <T> Builder<T> newBatchInserter (final Stage stage)
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

        private final List<Selector<T>> actions = Lists.newLinkedList();

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
            actions.add(new Selector<>(condition));
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
    private static final class Selector<T>
    {
        private final Predicate<T> condition;

        private final AtomicReference<T> data = new AtomicReference<>();

        private Selector (final Predicate<T> condition)
        {
            this.condition = condition;
        }

        public boolean receive (final T message)
        {
            final boolean alreadyInBatch = data.get() != null;

            if (alreadyInBatch)
            {
                return NOT_ADDED_TO_BATCH;
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

        public void append (final ImmutableList.Builder<T> list)
        {
            list.add(data.get());
        }

        public void clear ()
        {
            data.set(null);
        }
    }

}
