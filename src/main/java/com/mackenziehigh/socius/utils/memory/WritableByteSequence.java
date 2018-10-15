package com.mackenziehigh.socius.utils.memory;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * A generic sequence of bytes that can be written-to.
 */
public interface WritableByteSequence
{
    /**
     * Get the number of bytes in this sequence.
     *
     * @return the length of this sequence.
     */
    public long length ();

    /**
     * Set the byte at the given index in this sequence to the given value.
     *
     * @param index is the location of the byte to set.
     * @param value is the new value of the indexed byte.
     */
    public void set (long index,
                     byte value);

    /**
     * Set all the bytes in the sequence to zero.
     */
    public default void zfill ()
    {
        for (long i = 0; i < length(); i++)
        {
            set(i, (byte) 0);
        }
    }

    /**
     * Copy bytes from the given array to this sequence.
     *
     * @param src provides the bytes to copy-in.
     * @return the number of copied bytes.
     */
    public default long copyFrom (final byte[] src)
    {
        return copyFrom(src, 0, 0, src.length);
    }

    /**
     * Copy bytes from the given array to this sequence.
     *
     * @param src provides the bytes to copy-in.
     * @param srcOffset is the location in the source to begin copying from.
     * @param destOffset is the location in this sequence to begin copying to.
     * @param length is the exact number of bytes to copy.
     * @return the number of copied bytes.
     */
    public default long copyFrom (final byte[] src,
                                  final long srcOffset,
                                  final long destOffset,
                                  final long length)
    {
        final ByteSequence other = ByteSequences.wrap(src);
        return copyFrom(other, srcOffset, destOffset, length);
    }

    /**
     * Copy bytes from the given buffer to this sequence.
     *
     * @param src provides the bytes to copy-in.
     * @return the number of copied bytes.
     */
    public default long copyFrom (final ByteBuffer src)
    {
        final ReadableByteSequence other = ByteSequences.wrap(src);
        return copyFrom(other);
    }

    /**
     * Copy bytes from the given buffer to this sequence.
     *
     * @param src provides the bytes to copy-in.
     * @param srcOffset is the location in the source to begin copying from.
     * @param destOffset is the location in this sequence to begin copying to.
     * @param length is the exact number of bytes to copy.
     * @return the number of copied bytes.
     */
    public default long copyFrom (final ByteBuffer src,
                                  final long srcOffset,
                                  final long destOffset,
                                  final long length)
    {
        final ReadableByteSequence other = ByteSequences.wrap(src);
        return copyFrom(other, srcOffset, destOffset, length);
    }

    /**
     * Copy bytes from the given sequence to this sequence.
     *
     * @param src provides the bytes to copy-in.
     * @return the number of copied bytes.
     */
    public default long copyFrom (final ReadableByteSequence src)
    {
        final long length = src.length();
        return copyFrom(src, 0, 0, length);
    }

    /**
     * Copy bytes from the given sequence to this sequence.
     *
     * @param src provides the bytes to copy-in.
     * @param srcOffset is the location in the source to begin copying from.
     * @param destOffset is the location in this sequence to begin copying to.
     * @param length is the exact number of bytes to copy.
     * @return the number of copied bytes.
     */
    public default long copyFrom (final ReadableByteSequence src,
                                  final long srcOffset,
                                  final long destOffset,
                                  final long length)
    {
        for (long i = 0; i < length; i++)
        {
            final long srcIdx = srcOffset + i;
            final long destIdx = srcOffset + i;
            final byte value = src.get(srcIdx);
            set(destIdx, value);
        }

        return length;
    }

    /**
     * Copy the bytes from the given stream to this sequence.
     *
     * @param src provides the bytes to copy-in.
     * @return the number of copied bytes.
     * @throws java.io.IOException
     */
    public default long copyFrom (final InputStream src)
            throws IOException
    {
        return copyFrom(src, 0, Integer.MAX_VALUE);
    }

    /**
     * Copy the bytes from the given stream to this sequence.
     *
     * @param src provides the bytes to copy-in.
     * @param offset is the location in the sequence to begin copying to.
     * @param length is the maximum number of bytes to copy.
     * @return the number of copied bytes.
     * @throws java.io.IOException
     */
    public default long copyFrom (final InputStream src,
                                  final long offset,
                                  final long length)
            throws IOException
    {
        if (length == 0)
        {
            return 0;
        }

        long count = 0;

        for (long data = src.read(); count < length && data >= 0; data = src.read())
        {
            set(offset + count++, (byte) (data - 128));
        }

        return count;
    }

    /**
     * Create an <code>OutputStream</code> that writes bytes to this sequence.
     *
     * @return the new stream.
     */
    public default DataOutputStream asOutputStream ()
    {
        return asOutputStream(0, length());
    }

    /**
     * Create an <code>OutputStream</code> that writes bytes to this sequence.
     *
     * @param offset is the location in the sequence to begin copying to.
     * @param length is the exact number of bytes to copy.
     * @return the new stream.
     */
    public default DataOutputStream asOutputStream (final long offset,
                                                    final long length)
    {
        final OutputStream delegate = new OutputStream()
        {
            private long index = offset;

            private final long end = offset + length;

            @Override
            public void write (final int value)
                    throws IOException
            {
                if (index > end)
                {
                    throw new EOFException();
                }
                else
                {
                    set(index++, (byte) value);
                }
            }
        };

        return new DataOutputStream(delegate);
    }
}
