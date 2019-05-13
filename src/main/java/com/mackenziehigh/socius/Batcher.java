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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.mackenziehigh.cascade.Cascade.ActorFactory;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

/**
 * Selects one message from each of (N) data-inputs,
 * combines the selections into a single message,
 * and then forwards the combined message.
 *
 * <p>
 * A series of internal queues are used to store incoming messages.
 * There is one queue for each of the (N) data-inputs.
 * When all of the queues contain at least one message each,
 * then one message will be removed from each queue and
 * a batch will be formed from those (N) messages.
 * Finally, the batch will be sent out.
 * </p>
 *
 * <p>
 * If the producers have differing message-rates,
 * then one queue may become much fully than another.
 * In effect, this can lead to a memory-leak like situation.
 * Thus, you may set a maximum capacity for each queue.
 * If a message is received, when the queue is at capacity,
 * then the message will simply be dropped.
 * </p>
 *
 * @param <T> is the type of the messages flowing through.
 */
public final class Batcher<T>
        implements Source<List<T>>
{
    /**
     * Provides the data-output connector.
     */
    private final Processor<List<T>> dataOut;

    /**
     * Provides the data-input connectors.
     */
    private final ImmutableList<Processor<T>> dataIn;

    /**
     * These queues store the incoming messages, until a batch can be formed.
     * There is one queue per data-input.
     * Whenever a message is received from data-input (N),
     * then the message will be added to the (Nth) queue.
     */
    private final ImmutableList<Deque<T>> queues;

    /**
     * This is the maximum number of messages in a single queue at any one time.
     */
    private final int capacity;

    /**
     * This flag is true, if the oldest message should be dropped when a queue overflows.
     * Thus flag is false, if the newest message should be dropped when a queue overflows.
     */
    private final boolean dropOldest;

    private Batcher (final Builder<T> builder)
    {
        this.dataOut = Processor.fromIdentityScript(builder.stage);
        this.capacity = builder.capacity;
        this.dropOldest = builder.dropOldest;

        final ImmutableList.Builder<Processor<T>> builderDataIn = ImmutableList.builder();
        final ImmutableList.Builder<Deque<T>> builderQueues = ImmutableList.builder();

        for (int i = 0; i < builder.arity; i++)
        {
            final int idx = i;
            builderDataIn.add(Processor.fromConsumerScript(builder.stage, (T msg) -> onMessage(idx, msg)));
            builderQueues.add(Queues.newArrayDeque());
        }

        this.dataIn = builderDataIn.build();
        this.queues = builderQueues.build();
    }

    /**
     * This method may be executed by multiple actors concurrently!
     *
     * @param index identifies a data-input.
     * @param message was just received via the data-input.
     */
    private void onMessage (final int index,
                            final T message)
    {
        /**
         * Find the queue corresponding to the indexed data-input.
         */
        final Deque<T> queue = queues.get(index);

        /**
         * If the queue is already full, then drop the incoming message.
         * Otherwise, enqueue the message and continue.
         */
        if (queue.size() < capacity)
        {
            queue.add(message);
        }
        else if (dropOldest)
        {
            queue.peekFirst();
        }
        else // drop newest
        {
            queue.peekLast();
        }

        /**
         * Determine whether there is at least one message in each queue.
         */
        final boolean readyToSend = queues.stream().noneMatch(x -> x.isEmpty());

        /**
         * If a message is available from each queue,
         * then we are ready to create and send a batch.
         */
        if (readyToSend)
        {
            /**
             * Create the batch.
             */
            final ImmutableList.Builder<T> batchList = ImmutableList.builderWithExpectedSize(queues.size());

            for (Queue<T> member : queues)
            {
                batchList.add(member.poll());
            }

            final ImmutableList<T> batch = batchList.build();

            /**
             * Send the batch.
             */
            dataOut.dataIn().send(batch);
        }
    }

    /**
     * Get the number of input connectors.
     *
     * @return the number of input.
     */
    public int arity ()
    {
        return dataIn.size();
    }

    /**
     * The batch will be created from these data-inputs.
     *
     * @param index identifies the data-input.
     * @return the desired data-input.
     */
    public Input<T> dataIn (final int index)
    {
        return dataIn.get(index).dataIn();
    }

    /**
     * The combined messages will be forwarded to this output.
     *
     * <p>
     * The combined messages are immutable lists containing
     * the component messages, such that the <code>Nth</code>
     * message was obtained from the <code>Nth</code> data-input.
     * </p>
     *
     * @return the data-output.
     */
    @Override
    public Output<List<T>> dataOut ()
    {
        return dataOut.dataOut();
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of the messages flowing through.
     * @param stage will be used to create private actors.
     * @return a builder that can build the batcher.
     */
    public static <T> Builder<T> newBatcher (final ActorFactory stage)
    {
        return new Builder<>(stage);
    }

    /**
     * Builder.
     *
     * @param <T> is the type of the messages flowing through.
     */
    public static final class Builder<T>
    {
        private final ActorFactory stage;

        private int arity;

        private int capacity = Integer.MAX_VALUE;

        private boolean dropOldest = true;

        private Builder (final ActorFactory stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        /**
         * Specify the number of data-inputs.
         *
         * @param value will be the number of input queues.
         * @return this.
         */
        public Builder<T> withArity (final int value)
        {
            Preconditions.checkArgument(value >= 0, "arity < 0");
            this.arity = value;
            return this;
        }

        /**
         * Specify the capacity of each of the input queues.
         *
         * @param value will be the maximum number of pending messages, per data-input.
         * @return this.
         */
        public Builder<T> withCapacity (final int value)
        {
            Preconditions.checkArgument(value >= 0, "capacity < 0");
            this.capacity = value;
            return this;
        }

        /**
         * Specify that the oldest message should be dropped,
         * whenever an internal queue overflows the capacity.
         *
         * @return this.
         */
        public Builder<T> withDropOldest ()
        {
            dropOldest = true;
            return this;
        }

        /**
         * Specify that the oldest message should be dropped,
         * whenever an internal queue overflows the capacity.
         *
         * @return this.
         */
        public Builder<T> withDropNewest ()
        {
            dropOldest = false;
            return this;
        }

        /**
         * construct the new object.
         *
         * @return the new object.
         */
        public Batcher<T> build ()
        {
            return new Batcher<>(this);
        }
    }
}
