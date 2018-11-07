package com.mackenziehigh.socius.io;

import java.io.IOException;
import java.nio.ByteBuffer;

@FunctionalInterface
public interface Decoder<T>
{
    public T decode (ByteBuffer buffer,
                     int size)
            throws IOException;
}
