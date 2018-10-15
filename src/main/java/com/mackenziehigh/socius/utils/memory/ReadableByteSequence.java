package com.mackenziehigh.socius.utils.memory;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * A generic sequence of bytes that can be read-from.
 */
public interface ReadableByteSequence
{
    /**
     * Get the number of bytes in this sequence.
     *
     * @return the length of this sequence.
     */
    public long length ();

    /**
     * Get the byte at the given index.
     *
     * <p>
     * If the index is out of bounds, then an exception will be thrown;
     * however, the type of the exception is implementation-specific.
     * </p>
     *
     * @param index identifies the byte to return.
     * @return the byte at the given index.
     */
    public byte get (long index);

    /**
     * Copy the bytes from this sequence to the given array.
     *
     * @param dest will receive the content of this sequence.
     * @return the number of copied bytes.
     */
    public default long copyTo (final byte[] dest)
    {
        return copyTo(dest, 0, 0, length());
    }

    /**
     * Copy the bytes from this sequence to the given array.
     *
     * @param dest will receive the content of this sequence.
     * @param srcOffset is the location in this sequence to begin copying from.
     * @param destOffset is the location in this sequence to begin copying to.
     * @param length is the exact number of bytes to copy.
     * @return the number of copied bytes.
     */
    public default long copyTo (final byte[] dest,
                                final long srcOffset,
                                final long destOffset,
                                final long length)
    {
        final WritableByteSequence other = ByteSequences.wrap(dest);
        return copyTo(other, srcOffset, destOffset, length);
    }

    /**
     * Copy the bytes from this sequence to the given buffer.
     *
     * @param dest will receive the content of this sequence.
     * @return the number of copied bytes.
     */
    public default long copyTo (final ByteBuffer dest)
    {
        return copyTo(dest, 0, 0, length());
    }

    /**
     * Copy the bytes from this sequence to the given buffer.
     *
     * @param dest will receive the content of this sequence.
     * @param srcOffset is the location in this sequence to begin copying from.
     * @param destOffset is the location in this sequence to begin copying to.
     * @param length is the exact number of bytes to copy.
     * @return the number of copied bytes.
     */
    public default long copyTo (final ByteBuffer dest,
                                final long srcOffset,
                                final long destOffset,
                                final long length)
    {
        final WritableByteSequence other = ByteSequences.wrap(dest);
        return copyTo(other, srcOffset, destOffset, length);
    }

    /**
     * Copy the bytes from this sequence to the given sequence.
     *
     * @param dest will receive the content of this sequence.
     * @return the number of copied bytes.
     */
    public default long copyTo (final WritableByteSequence dest)
    {
        return copyTo(dest, 0, 0, length());
    }

    /**
     * Copy the bytes from this sequence to the given sequence.
     *
     * @param dest will receive the content of this sequence.
     * @param srcOffset is the location in this sequence to begin copying from.
     * @param destOffset is the location in this sequence to begin copying to.
     * @param length is the exact number of bytes to copy.
     * @return the number of copied bytes.
     */
    public default long copyTo (final WritableByteSequence dest,
                                final long srcOffset,
                                final long destOffset,
                                final long length)
    {
        return dest.copyFrom(this, srcOffset, destOffset, length);
    }

    /**
     * Copy the bytes from this sequence to the given stream.
     *
     * @param dest will receive the content of this sequence.
     * @return the number of copied bytes.
     * @throws IOException
     */
    public default long copyTo (final OutputStream dest)
            throws IOException
    {
        return copyTo(dest, 0, length());
    }

    /**
     * Copy the bytes from this sequence to the given stream.
     *
     * @param dest will receive the content of this sequence.
     * @param offset is the location in this sequence to begin copying from.
     * @param length is the exact number of bytes to copy.
     * @return the number of copied bytes.
     * @throws IOException
     */
    public default long copyTo (final OutputStream dest,
                                final long offset,
                                final long length)
            throws IOException
    {
        for (int i = 0; i < length; i++)
        {
            dest.write(get(offset + i));
        }

        return length;
    }

    /**
     * Create an <code>InputStream</code> that reads from this sequence.
     *
     * @return the new stream.
     */
    public default DataInputStream asInputStream ()
    {
        return asInputStream(0, length());
    }

    /**
     * Create an <code>InputStream</code> that reads from this sequence.
     *
     * @param offset is the location in this sequence to begin reading.
     * @param length is the number of bytes to read.
     * @return the new stream.
     */
    public default DataInputStream asInputStream (final long offset,
                                                  final long length)
    {
        final InputStream delegate = new InputStream()
        {
            private int count;

            @Override
            public int read ()
                    throws IOException
            {
                return count < length ? get(offset + (count++)) + 128 : -1;
            }
        };

        return new DataInputStream(delegate);
    }

    /**
     * Convert this sequence to a new byte-array.
     *
     * @return a copy of this sequence.
     */
    public default byte[] toByteArray ()
    {
        if (length() > ByteSequences.MAX_ARRAY_SIZE)
        {
            throw new IllegalStateException(String.format("Too Large For Conversion: %d > %d", length(), ByteSequences.MAX_ARRAY_SIZE));
        }
        else
        {
            final byte[] array = new byte[(int) length()];
            copyTo(array);
            return array;
        }
    }

    /**
     * Convert this sequence to a new <code>ByteBuffer</code>.
     *
     * @return a copy of this sequence.
     */
    public default ByteBuffer toByteBuffer ()
    {
        if (length() > ByteSequences.MAX_ARRAY_SIZE)
        {
            throw new IllegalStateException(String.format("Too Large For Conversion: %d > %d", length(), ByteSequences.MAX_ARRAY_SIZE));
        }
        else
        {
            final ByteBuffer buffer = ByteBuffer.allocate((int) length());
            copyTo(buffer);
            return buffer;
        }
    }

    /**
     * Convert this sequence to a <code>String</code> using the given <code>Charset</code>.
     *
     * @param charset specifies the encoding of the bytes in this sequence.
     * @return the new string.
     */
    public default String toString (final Charset charset)
    {
        return new String(toByteArray(), charset);
    }

    /**
     * Convert this sequence to a <code>String</code> using the UTF-8 <code>Charset</code>.
     *
     * <p>
     * Equivalent: <code>toString(StandardCharsets.UTF_8)</code>
     * </>
     *
     * @return the new string.
     */
    @Override
    public String toString ();
}
