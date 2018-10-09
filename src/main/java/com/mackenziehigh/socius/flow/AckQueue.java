package com.mackenziehigh.socius.flow;

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
 * Acknowledgment Queue.
 */
public final class AckQueue<T, R>
{
    private final int backlog;

    private final int permits;

    private final AtomicInteger tracker = new AtomicInteger();

    private final AtomicLong sends = new AtomicLong();

    private final AtomicLong acks = new AtomicLong();

    private final AtomicLong overflows = new AtomicLong();

    private final Queue<T> backlogQueue;

    private final Processor<T> procDataIn;

    private final Processor<T> procDataOut;

    private final Processor<T> procOverflowOut;

    private final Processor<R> procAcksIn;

    private final Object lock = new Object();

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
        synchronized (lock)
        {
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
        synchronized (lock)
        {
            tracker.decrementAndGet();
            fixup();
        }
    }

    private void fixup ()
    {
        if (tracker.get() < 0)
        {
            throw new IllegalStateException("Unbalanced Acks");
        }

        final boolean permitAvailable = tracker.get() < permits;
        final boolean dataAvailable = !backlogQueue.isEmpty();
        final boolean readyToSend = permitAvailable && dataAvailable;

        if (readyToSend)
        {
            Verify.verify(sends.get() - acks.get() <= permits);
            tracker.incrementAndGet();
            procDataOut.dataIn().send(backlogQueue.poll());
        }
    }

    public Input<T> dataIn ()
    {
        return procDataIn.dataIn();
    }

    public Output<T> dataOut ()
    {
        return procDataOut.dataOut();
    }

    public Output<T> overflowOut ()
    {
        return procOverflowOut.dataOut();
    }

    public Input<R> acksIn ()
    {
        return procAcksIn.dataIn();
    }

    public long sends ()
    {
        return sends.get();
    }

    public long acks ()
    {
        return acks.get();
    }

    public long overflow ()
    {
        return overflows.get();
    }

    public static <T, R> Builder<T, R> newAckQueue (final Stage stage)
    {
        return new Builder<>(stage);
    }

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

        public Builder<T, R> withQueue (final Queue<T> queue)
        {
            this.queue = Objects.requireNonNull(queue, "queue");
            return this;
        }

        public Builder<T, R> withBacklogCapacity (final int limit)
        {
            Preconditions.checkArgument(limit >= 0, "limit < 0");
            this.backlog = limit;
            return this;
        }

        public Builder<T, R> withPermits (final int count)
        {
            Preconditions.checkArgument(count >= 0, "count < 0");
            this.permits = count;
            return this;
        }

        public AckQueue<T, R> build ()
        {
            return new AckQueue<>(this);
        }
    }
}
