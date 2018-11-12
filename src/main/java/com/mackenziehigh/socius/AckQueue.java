package com.mackenziehigh.socius;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.Queues;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Requires acknowledgment of previous messages before forwarding newer messages.
 *
 * @param <T> is the type of messages passing through this object.
 * @param <R> is the type of the acknowledgments sent back to this object.
 */
public final class AckQueue<T, R>
{
    /**
     * This is the maximum number of messages that can be either
     * pending forwarding or pending acknowledgment simultaneously.
     */
    private final int backlog;

    /**
     * This is the maximum number of outstanding unacknowledged
     * messages that can be n-flight at any any moment in time.
     */
    private final int permits;

    /**
     * This is the number of outstanding unacknowledged
     * messages that are currently in-flight.
     */
    private final AtomicInteger outstanding = new AtomicInteger();

    /**
     * This is the number of messages that have been forwarded, thus far.
     */
    private final AtomicLong forwards = new AtomicLong();

    /**
     * This is the number of acknowledgments that have been received, thus far.
     */
    private final AtomicLong acks = new AtomicLong();

    /**
     * This is the number of incoming messages that have been dropped
     * immediately upon reception, because the internal queue was full.
     */
    private final AtomicLong overflows = new AtomicLong();

    /**
     * This is the internal queue of messages that are waiting to be forwarded.
     */
    private final Queue<T> backlogQueue;

    /**
     * This lock is used to synchronize the various actor herein.
     * The critical sections are short and always advancing;
     * therefore, the actors would not be waiting on each other much.
     */
    private final Object lock = new Object();

    private final Processor<T> procDataIn;

    private final Processor<T> procDataOut;

    private final Processor<T> procOverflowOut;

    private final Processor<R> procAcksIn;

    private AckQueue (final Builder builder)
    {
        this.permits = builder.permits;
        this.backlog = builder.backlog;
        this.backlogQueue = builder.queue;
        this.procDataIn = Processor.newProcessor(builder.stage, this::onData);
        this.procDataOut = Processor.newProcessor(builder.stage);
        this.procAcksIn = Processor.newProcessor(builder.stage, this::onAck);
        this.procOverflowOut = Processor.newProcessor(builder.stage);
    }

    private void onData (final T message)
    {
        /**
         * Synchronize, since we may get an acknowledgment,
         * while we are executing this critical section.
         */
        synchronized (lock)
        {
            /**
             * If the queue is already full, we cannot accept another message.
             * We must drop the message due to capacity constraints.
             */
            final boolean overflow = backlogQueue.size() == backlog;

            if (overflow)
            {
                overflows.incrementAndGet();
                procOverflowOut.dataIn().send(message);
            }
            else if (backlogQueue.offer(message) == false)
            {
                overflows.incrementAndGet();
                procOverflowOut.dataIn().send(message);
            }
            else
            {
                fixup();
            }
        }
    }

    private void onAck (final R message)
    {
        /**
         * Synchronize, since we may get a new incoming message,
         * while we are processing the acknowledgment.
         */
        synchronized (lock)
        {
            outstanding.decrementAndGet();
            fixup();
        }
    }

    /**
     * This method performs both forwarding of messages
     * and processes acknowledgments. Therefore,
     * this method is where the real work happens.
     */
    private void fixup ()
    {
        /**
         * Throw an exception, if we receive more acknowledgments
         * than messages that we have forwarded.
         */
        Preconditions.checkState(outstanding.get() >= 0, "Unbalanced Acks");

        /**
         * Has the maximum number of in-flight messages been reached?
         */
        final boolean permitAvailable = outstanding.get() < permits;

        /**
         * Does the queue have a message that needs to be forwarded?
         */
        final boolean dataAvailable = !backlogQueue.isEmpty();

        /**
         * If we have a message to send and there are not too many
         * messages already in-flight, then forward the message.
         */
        final boolean readyToSend = permitAvailable && dataAvailable;

        if (readyToSend)
        {
            Verify.verify(forwards.get() - acks.get() <= permits);
            outstanding.incrementAndGet();
            procDataOut.dataIn().send(backlogQueue.poll());
        }
    }

    /**
     * Send messages that need to be forwarded to this input.
     *
     * <p>
     * The messages will be forwarded, when the messages that
     * were previously forwarded are successfully acknowledged.
     * </p>
     *
     * <p>
     * If the internal queue cannot accept the incoming messages
     * from this input due to capacity constraints,
     * then the messages will be forwarded to the overflow-output.
     * </p>
     *
     * @return the data-input.
     */
    public Input<T> dataIn ()
    {
        return procDataIn.dataIn();
    }

    /**
     * This object will forward messages from the data-input to this output.
     *
     * <p>
     * All messages that pass through this output must be acknowledged.
     * Otherwise, this object may become permanently blocked.
     * </p>
     *
     * @return the data-output.
     */
    public Output<T> dataOut ()
    {
        return procDataOut.dataOut();
    }

    /**
     * Messages will be forwarded from the data-input to this output,
     * whenever the internal queue of unacknowledged messages overflows.
     *
     * @return the overflow-output.
     */
    public Output<T> overflowOut ()
    {
        return procOverflowOut.dataOut();
    }

    /**
     * Send acknowledgments to this input.
     *
     * @return the ack-input.
     */
    public Input<R> acksIn ()
    {
        return procAcksIn.dataIn();
    }

    /**
     * Getter.
     *
     * @return the number of messages that have been forwarded,
     * including both acknowledged and unacknowledged messages.
     */
    public long forwards ()
    {
        return forwards.get();
    }

    /**
     * Getter.
     *
     * @return the number of acknowledgments that have been received.
     */
    public long acks ()
    {
        return acks.get();
    }

    /**
     * Getter.
     *
     * @return the number of messages that were dropped and never forwarded.
     */
    public long overflow ()
    {
        return overflows.get();
    }

    /**
     * Create a builder object that can build a <code>AckQueue</code>.
     *
     * @param <T> is the type of messages passing through the ack-queue.
     * @param <R> is the type of acknowledgments send to the ack-queue.
     * @param stage will be used to create private actors.
     * @return the new builder.
     */
    public static <T, R> Builder<T, R> newAckQueue (final Stage stage)
    {
        return new Builder<>(stage);
    }

    /**
     * Builder.
     *
     * @param <T> is the type of messages passing through the ack-queue.
     * @param <R> is the type of acknowledgments send to the ack-queue.
     */
    public static final class Builder<T, R>
    {
        private final Stage stage;

        private int backlog = Integer.MAX_VALUE;

        private int permits = 1;

        private Queue<T> queue = Queues.newLinkedBlockingQueue();

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        /**
         * Specify the queue to use to store messages pending forwarding.
         *
         * <p>
         * By default, an unbounded <code>LinkedBlockingQueue</code> is used.
         *
         * @param queue will be used to store the backlog.
         * @return this.
         */
        public Builder<T, R> withQueue (final Queue<T> queue)
        {
            this.queue = Objects.requireNonNull(queue, "queue");
            return this;
        }

        /**
         * Specify the maximum number of messages that can be pending forwarding.
         *
         * @param limit is he desired limit.
         * @return this.
         */
        public Builder<T, R> withBacklogCapacity (final int limit)
        {
            Preconditions.checkArgument(limit >= 0, "limit < 0");
            this.backlog = limit;
            return this;
        }

        /**
         * Specify the maximum number of unacknowledged messages
         * that can be in-flight at aby one moment in time.
         *
         * @param limit is the desired limit.
         * @return this.
         */
        public Builder<T, R> withInFlightPermits (final int limit)
        {
            Preconditions.checkArgument(limit >= 0, "limit < 0");
            this.permits = limit;
            return this;
        }

        /**
         * Build.
         *
         * @return the new ack-queue.
         */
        public AckQueue<T, R> build ()
        {
            return new AckQueue<>(this);
        }
    }
}
