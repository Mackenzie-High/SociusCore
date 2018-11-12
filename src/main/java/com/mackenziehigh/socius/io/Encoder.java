package com.mackenziehigh.socius.io;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Converts an object into a sequence of bytes.
 *
 * @param <T> is the type of object that will be converted.
 */
@FunctionalInterface
public interface Encoder<T>
{

    /**
     * Perform the encoding.
     *
     * @param buffer will receive the sequence of bytes.
     * @param object will be converted to a sequence of bytes.
     * @return the length of the encoded message.
     * @throws IOException as needed, if something goes wrong.
     */
    public int encode (ByteBuffer buffer,
                       T object)
            throws IOException;

}
