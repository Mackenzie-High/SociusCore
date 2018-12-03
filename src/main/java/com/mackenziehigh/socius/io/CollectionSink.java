package com.mackenziehigh.socius.io;

import com.google.common.base.Preconditions;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.Processor;
import java.util.Collection;

/**
 * Adds incoming messages to a specified <code>Collection</code>.
 *
 * @param <T> is the type of messages in the collection.
 */
public final class CollectionSink<T>
{
    private final Processor<T> actor;

    private CollectionSink (final Processor<T> actor)
    {
        this.actor = actor;
    }

    /**
     * Input Connection.
     *
     * @return the input that provides the messages to add to the collection.
     */
    public Input<T> dataIn ()
    {
        return actor.dataIn();
    }

    /**
     * Output Connection.
     *
     * @return the output that merely forwards the messages from data-in.
     */
    public Output<T> dataOut ()
    {
        return actor.dataOut();
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of messages in the collection.
     * @param stage will be used to create private actors.
     * @param collection will receive the messages from data-in.
     * @return the new sink.
     */
    public static <T> CollectionSink<T> newCollectionSink (final Cascade.Stage stage,
                                                           final Collection<T> collection)
    {
        Preconditions.checkNotNull(stage, "stage");
        Preconditions.checkNotNull(collection, "collection");
        final Processor<T> proc = Processor.newConsumer(stage, (T x) -> collection.add(x));
        return new CollectionSink<>(proc);
    }
}
