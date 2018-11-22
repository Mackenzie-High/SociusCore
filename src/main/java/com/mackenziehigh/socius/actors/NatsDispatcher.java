package com.mackenziehigh.socius.actors;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.actors.ChannelBilink;
import com.mackenziehigh.socius.actors.HashDispatcher;
import com.mackenziehigh.socius.actors.Processor;
import io.nats.client.Connection;
import io.nats.client.Nats;
import java.io.IOException;

/**
 * No wildcards? Memory leaks due to no un-publish/un-subscribe. Encoder/Decoder support?
 */
public final class NatsDispatcher
        implements ChannelBilink<String, byte[]>
{
    private final Stage stage;

    private final HashDispatcher<String, byte[]> routingTable;

    private final Connection connection;

    private NatsDispatcher (final Stage stage,
                            final Connection conn)
    {
        this.stage = stage;
        this.routingTable = HashDispatcher.newDispatcher(stage);
        this.connection = conn;
    }

    public static NatsDispatcher create (final Stage stage)
            throws IOException,
                   InterruptedException
    {
        return new NatsDispatcher(stage, Nats.connect());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NatsDispatcher publish (final Actor<?, byte[]> connector,
                                   final String key)
    {
        return publish(connector.output(), key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NatsDispatcher publish (final Output<byte[]> connector,
                                   final String key)
    {
        final Processor<byte[]> proc = Processor.newProcessor(stage, (byte[] msg) -> connection.publish(key, msg));
        connector.connect(proc.dataIn());
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NatsDispatcher unpublish (final Actor<?, byte[]> connector,
                                     final String key)
    {
        return unpublish(connector.output(), key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NatsDispatcher unpublish (final Output<byte[]> connector,
                                     final String key)
    {
        // TODO
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NatsDispatcher subscribe (final Actor<byte[], ?> connector,
                                     final String key)
    {
        return subscribe(connector.input(), key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NatsDispatcher subscribe (final Input<byte[]> connector,
                                     final String key)
    {
        routingTable.subscribe(connector, key);
        connection.createDispatcher(msg -> routingTable.send(key, msg.getData())).subscribe(key);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NatsDispatcher unsubscribe (final Actor<byte[], ?> connector,
                                       final String key)
    {
        return unsubscribe(connector.input(), key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NatsDispatcher unsubscribe (final Input<byte[]> connector,
                                       final String key)
    {
        // TODO:
        return this;
    }

}
