package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Message Bus for (N x N) <i>Intra</i>-Process-Communication.
 *
 * @param <M> is the type of messages that flow through the message-bus.
 */
public final class Bus<M>
{
    private final Stage stage;

    /**
     * This processor is used to connect the inputs to the outputs.
     */
    private final Processor<M> hub;

    /**
     * Messages are routed from these processors to the hub.
     *
     * <p>
     * In theory, the inputs could have been connected directly to the hub.
     * However, in practice, that would be inefficient,
     * because actors use copy-on-write lists in their connectors.
     * </p>
     */
    private final Map<Object, Processor<M>> inputs = new ConcurrentHashMap<>();

    /**
     * Messages are routed from the hub to these processors.
     *
     * <p>
     * In theory, the outputs could have been connected directly to the hub.
     * However, in practice, that would be inefficient,
     * because actors use copy-on-write lists in their connectors.
     * </p>
     */
    private final Map<Object, Processor<M>> outputs = new ConcurrentHashMap<>();

    private final Collection<Processor<M>> outputsView = outputs.values();

    private Bus (final Stage stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.hub = Processor.newProcessor(stage, this::forwardFromHub);
    }

    /**
     * Get a named input that supplies messages to this message-bus.
     *
     * @param key identifies the input to retrieve.
     * @return the named input.
     */
    public Input<M> dataIn (final Object key)
    {
        // Thread Safe.
        inputs.putIfAbsent(key, Processor.newProcessor(stage, this::forwardToHub));
        return inputs.get(key).dataIn();
    }

    /**
     * Get a named output that transmits messages from this message-bus.
     *
     * @param key identifies the output to retrieve.
     * @return the named output.
     */
    public Output<M> dataOut (final Object key)
    {
        // Thread Safe.
        outputs.putIfAbsent(key, Processor.newProcessor(stage));
        return outputs.get(key).dataOut();
    }

    /**
     * Factory Method.
     *
     * @param <M> is the type of messages that flow through the message-bus.
     * @param stage will be used to create private actors.
     * @return the new message-bus.
     */
    public static <M> Bus<M> newBus (final Stage stage)
    {
        return new Bus<>(stage);
    }

    private void forwardToHub (final M message)
    {
        hub.dataIn().send(message);
    }

    private void forwardFromHub (final M message)
    {
        for (Processor<M> output : outputsView)
        {
            output.dataIn().send(message);
        }
    }

}
