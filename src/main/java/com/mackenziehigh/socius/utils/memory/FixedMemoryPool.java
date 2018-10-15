package com.mackenziehigh.socius.utils.memory;

import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public final class FixedMemoryPool
        implements MemoryPool
{
    private final Queue<Buffer> pool;

    private final AtomicLong tagger = new AtomicLong(UUID.randomUUID().hashCode());

    public FixedMemoryPool (final int bufferSize,
                            final int bufferCount)
    {
        pool = new ArrayBlockingQueue<>(bufferCount);

        for (int i = 0; i < bufferCount; i++)
        {
            final ByteSequence seq = ByteSequences.allocate(bufferSize);
            final Buffer buffer = new Buffer(this, seq);
            pool.add(buffer);
        }
    }

    @Override
    public Optional<ByteSequence> allocate (final long size)
    {
        final Buffer buffer = pool.poll();

        if (buffer == null)
        {
            return Optional.empty();
        }
        else
        {
            final Allocation allocation = new Allocation(buffer, tagger.incrementAndGet());
            return Optional.of(allocation);
        }
    }

    @Override
    public void free (final ByteSequence buffer)
    {
        final boolean applicable = buffer != null
                                   && buffer instanceof Allocation
                                   && ((Allocation) buffer).buffer.pool.equals(this);

        if (applicable)
        {
            final Allocation object = (Allocation) buffer;
            object.buffer.tag = 0;
            pool.add(object.buffer);
        }
    }

    private static final class Allocation
            implements ByteSequence
    {
        private final Buffer buffer;

        private final long tag;

        public Allocation (final Buffer buffer,
                           final long tag)
        {
            this.buffer = buffer;
            this.tag = tag;
        }

        @Override
        public long length ()
        {
            checkTag();
            return buffer.sequence.length();
        }

        @Override
        public byte get (final long index)
        {
            checkTag();
            return buffer.sequence.get(index);
        }

        @Override
        public void set (final long index,
                         final byte value)
        {
            checkTag();
            buffer.sequence.set(index, value);
        }

        @Override
        public String toString ()
        {
            checkTag();
            return buffer.sequence.toString();
        }

        private void checkTag ()
        {
            if (tag != buffer.tag)
            {
                throw new IllegalStateException("Already Freed!");
            }
        }

    }

    private static final class Buffer
    {
        public volatile long tag;

        public final FixedMemoryPool pool;

        public final ByteSequence sequence;

        private Buffer (final FixedMemoryPool pool,
                        final ByteSequence sequence)
        {
            this.pool = pool;
            this.sequence = sequence;
        }
    }
}
