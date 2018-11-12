package com.mackenziehigh.socius;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Converts the contents of a <code>ByteBuffer</code> into an object.
 *
 * @param <T> is the type of object that will be produced.
 */
@FunctionalInterface
public interface Decoder<T>
{
    /**
     * Perform the decoding.
     *
     * @param buffer contains the bytes to decode.
     * @param size is the size of the encoded message within the buffer.
     * @return the object created by decoding the message.
     * @throws IOException as needed, if something goes wrong.
     */
    public T decode (ByteBuffer buffer,
                     int size)
            throws IOException;
}
