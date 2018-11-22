package com.mackenziehigh.socius.actors;

import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Map;
import java.util.Objects;

/**
 *
 */
public final class Multiplexer<K, M>
{
    private final Stage stage;

    private final Map<K, Processor<M>> inputs = Maps.newConcurrentMap();

    private final Processor<MultiplexedMessage<K, M>> output;

    private Multiplexer (final Stage stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.output = Processor.newProcessor(stage);
    }

    public Input<M> dataIn (final K key)
    {
        inputs.putIfAbsent(key, Processor.newProcessor(stage, (M msg) -> onMessage(key, msg)));
        return inputs.get(key).dataIn();
    }

    private void onMessage (final K key,
                            final M message)
    {
        final MultiplexedMessage<K, M> entry = newMessage(key, message);
        output.dataIn().send(entry);
    }

    public Output<MultiplexedMessage<K, M>> dataOut ()
    {
        return output.dataOut();
    }

    public static <K, M> Multiplexer<K, M> newMultiplexer (final Stage stage)
    {
        return new Multiplexer<>(stage);
    }

    public static <K, M> MultiplexedMessage<K, M> newMessage (final K key,
                                                   final M message)
    {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(message, "message");

        return new MultiplexedMessage<K, M>()
        {
            @Override
            public K key ()
            {
                return key;
            }

            @Override
            public M message ()
            {
                return message;
            }

            @Override
            public boolean equals (final Object o)
            {
                return o instanceof MultiplexedMessage
                       && Objects.equals(key, ((MultiplexedMessage) o).key())
                       && Objects.equals(message, ((MultiplexedMessage) o).message());
            }

            @Override
            public int hashCode ()
            {
                return 73 * key.hashCode() + 93 * message.hashCode();
            }

            @Override
            public String toString ()
            {
                return String.format("%s => %s", key, message);
            }
        };
    }

    public interface MultiplexedMessage<K, M>
    {
        public K key ();

        public M message ();
    }
}
