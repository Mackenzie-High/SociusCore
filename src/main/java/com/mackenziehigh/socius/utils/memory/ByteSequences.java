package com.mackenziehigh.socius.utils.memory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Static Utility Methods for <code>ByteSequence</code>s.
 */
public final class ByteSequences
{
    public static long MAX_ARRAY_SIZE = 2_147_483_647;

    /**
     * Create a new <code>ByteSequence</code> based on a newly allocated byte-array.
     *
     * @param size will be the size of the new sequence.
     * @return the new sequence.
     */
    public static ByteSequence allocate (final long size)
    {

        final byte[] array = new byte[(int) size];
        final ByteSequence sequence = wrap(array);
        return sequence;
    }

    /**
     * Create a new <code>ByteSequence</code> that is merely a view of the given delegate.
     *
     * @param delegate will provide the bytes in the sequence.
     * @return the new view.
     */
    public static ByteSequence wrap (final byte[] delegate)
    {
        final ByteBuffer buffer = ByteBuffer.wrap(delegate);
        return wrap(buffer);
    }

    public static ByteSequence wrap (final byte[] delegate,
                                     final long offset,
                                     final long length)
    {
        return slice(wrap(delegate), offset, length);
    }

    /**
     * Create a new <code>ByteSequence</code> that is merely a view of the given delegate.
     *
     * @param delegate will provide the bytes in the sequence.
     * @return the new view.
     */
    public static ByteSequence wrap (final ByteBuffer delegate)
    {
        return new ByteSequence()
        {
            @Override
            public long length ()
            {
                return delegate.limit();
            }

            @Override
            public byte get (final long index)
            {
                // TODO: Check Index better
                return delegate.get((int) index);
            }

            @Override
            public void set (final long index,
                             final byte value)
            {
                // TODO: Check Index better
                delegate.put((int) index, value);
            }

            @Override
            public String toString ()
            {
                return toString(StandardCharsets.UTF_8);
            }
        };
    }

    public static ByteSequence wrap (final ByteBuffer delegate,
                                     final long offset,
                                     final long length)
    {
        return slice(wrap(delegate), offset, length);
    }

    /**
     * Create a new <code>ByteSequence</code> that is merely a view of the given delegate.
     *
     * @param delegate will provide the bytes in the sequence.
     * @return the new view.
     */
    public static ByteSequence wrap (final RandomAccessFile delegate)
    {
        return new ByteSequence()
        {
            @Override
            public long length ()
            {
                try
                {
                    return delegate.length();
                }
                catch (IOException ex)
                {
                    throw new IllegalStateException(ex);
                }
            }

            @Override
            public byte get (final long index)
            {
                try
                {
                    final int value = delegate.read();
                    return 0; // TODO Shift.
                }
                catch (IOException ex)
                {
                    throw new IllegalStateException(ex);
                }
            }

            @Override
            public void set (long index,
                             byte value)
            {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public String toString ()
            {
                return toString(StandardCharsets.UTF_8);
            }
        };
    }

    public static ByteSequence wrap (final RandomAccessFile delegate,
                                     final long offset,
                                     final long length)
    {
        return slice(wrap(delegate), offset, length);
    }

    public static ByteSequence slice (final ByteSequence original,
                                      final long offset,
                                      final long length)
    {
        // TODO: Check indexes

        return new ByteSequence()
        {
            @Override
            public long length ()
            {
                return original.length();
            }

            @Override
            public byte get (final long index)
            {
                return original.get(offset + index);
            }

            @Override
            public void set (final long index,
                             final byte value)
            {
                original.set(offset + index, value);
            }
        };
    }
}
