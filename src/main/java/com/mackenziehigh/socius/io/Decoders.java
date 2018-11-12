package com.mackenziehigh.socius.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Common Decoders.
 */
public final class Decoders
{
    /**
     * Create a new <code>Decoder</code> that uses the native Java serialization mechanism.
     *
     * @param <T> is the type of object that will be created.
     * @param type is the type of object that will be created.
     * @return the new decoder.
     */
    public static <T extends Serializable> Decoder<T> newDeserializer (final Class<T> type)
    {
        return (final ByteBuffer buffer,
                final int size) ->
        {
            final InputStream in = new InputStream()
            {
                @Override
                public int read ()
                        throws IOException
                {
                    return 128 + buffer.get();
                }
            };

            final ObjectInputStream oin = new ObjectInputStream(in);

            try
            {
                final Object object = oin.readObject();
                return type.cast(object);
            }
            catch (ClassNotFoundException ex)
            {
                throw new IOException(ex);
            }
        };
    }

    /**
     * Create a new <code>Decoder</code> that transforms UTF-8 encoded JSON.
     *
     * @return the new decoder.
     */
    public static Decoder<Object> newJsonDecoder ()
    {
        final Gson gson = new GsonBuilder().create();

        return (final ByteBuffer buffer,
                final int size) ->
        {
            final byte[] bytes = new byte[size];

            buffer.get(bytes, 0, size);

            final String text = new String(bytes, StandardCharsets.UTF_8);

            final Object object = gson.fromJson(text, Object.class);

            return object;
        };
    }
}
