package com.mackenziehigh.socius.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Common Encoders.
 */
public final class Encoders
{
    /**
     * Create a new <code>Encoder</code> that uses the native Java serialization mechanism.
     *
     * @param <T> is the type of object that will be encoded.
     * @return the new encoder.
     */
    public static <T extends Serializable> Encoder<T> newSerializer ()
    {
        return (final ByteBuffer buffer,
                final T data) ->
        {
            final AtomicInteger count = new AtomicInteger();

            final OutputStream os = new OutputStream()
            {
                @Override
                public void write (final int value)
                        throws IOException
                {
                    count.incrementAndGet();
                    buffer.put((byte) (value & 0xFF));
                }
            };

            final ObjectOutputStream oos = new ObjectOutputStream(os);

            oos.writeObject(data);

            return count.get();
        };
    }

    /**
     * Create a new <code>Encoder</code> that transforms objects in UTF-8 encoded JSON.
     *
     * @param <T> is the type of object that will be encoded.
     * @return the new encoder.
     */
    public static <T extends CharSequence> Encoder<T> newJsonEncoder ()
    {
        final Gson gson = new GsonBuilder().create();

        return (final ByteBuffer buffer,
                final T data) ->
        {
            final AtomicInteger count = new AtomicInteger();

            final String text = gson.toJson(data);

            final byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

            buffer.put(bytes);

            return count.get();
        };
    }

}
