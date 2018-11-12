package com.mackenziehigh.socius;

import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.Multiplexer.MultiplexedMessage;
import java.util.Map;
import java.util.Objects;

/**
 * Route messages received from a <code>Multiplexor</code> to data-outputs.
 *
 * @param <K> is the type of the routing-key in the multiplexed messages.
 * @param <M> is the type of subordinate messages contained in the multiplexed messages.
 */
public final class Demultiplexer<K, M>
{
    private final Stage stage;

    private final Processor<MultiplexedMessage<K, M>> input;

    private final Map<K, Processor<M>> outputs = Maps.newConcurrentMap();

    private final Processor<M> sinkDead;

    private Demultiplexer (final Stage stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.input = Processor.newProcessor(stage, this::onMessage);
        this.sinkDead = Processor.newProcessor(stage);
    }

    private void onMessage (final MultiplexedMessage<K, M> message)
    {
        /**
         * Get the destination directly from the map,
         * instead of calling dataOut(), which is faster
         * due to the avoidance of an allocation.
         * In addition, this ensures that a memory-leak
         * cannot occur due to a sender sending messages
         * with random keys.
         */
        final Processor<M> dest = outputs.get(message.key());

        if (dest != null)
        {
            dest.dataIn().send(message.message());
        }
        else
        {
            sinkDead.dataIn().send(message.message());
        }
    }

    /**
     * Multiplexed messages that are sent to this input
     * will be routed to the appropriate data-output,
     * if possible; otherwise, they will be forwarded
     * to the sind-dead output.
     *
     * @return the data-input.
     */
    public Input<MultiplexedMessage<K, M>> dataIn ()
    {
        return input.dataIn();
    }

    /**
     * Retrieve the data-output that will receive de-multiplexed
     * messages that are identified by the given identifier (key).
     *
     * @param key identifies the data-output.
     * @return the data-output.
     */
    public Output<M> dataOut (final K key)
    {
        outputs.putIfAbsent(key, Processor.newProcessor(stage));
        return outputs.get(key).dataOut();
    }

    /**
     * Messages that cannot be routed, because no corresponding data-output
     * could be found, will be routed to this output instead.
     *
     * @return sink-dead output.
     */
    public Output<M> sinkDead ()
    {
        return sinkDead.dataOut();
    }

    /**
     * Factory Method.
     *
     * @param <K> is the type of the routing-key in the multiplexed messages.
     * @param <M> is the type of subordinate messages contained in the multiplexed messages.
     * @param stage will be used to create private actors.
     * @return the newly constructed object.
     */
    public static <K, M> Demultiplexer<K, M> newDemultiplexer (final Cascade.Stage stage)
    {
        return new Demultiplexer<>(stage);
    }
}
