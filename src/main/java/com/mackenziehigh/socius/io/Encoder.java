package com.mackenziehigh.socius.io;

import java.io.IOException;
import java.nio.ByteBuffer;

@FunctionalInterface
public interface Encoder<T>
{

    public int encode (ByteBuffer buffer,
                       T data)
            throws IOException;

}
