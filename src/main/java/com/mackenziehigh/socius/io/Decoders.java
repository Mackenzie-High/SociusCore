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
    public static <T extends Serializable> Decoder<T> newDeserializer ()
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
                return (T) object;
            }
            catch (ClassNotFoundException ex)
            {
                throw new IOException(ex);
            }
        };
    }

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
