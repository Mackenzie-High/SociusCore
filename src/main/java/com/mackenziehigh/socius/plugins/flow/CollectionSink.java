package com.mackenziehigh.socius.plugins.flow;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Collection;

/**
 * Adds incoming messages to a specified <code>Collection</code>.
 *
 * @param <T> is the type of messages in the collection.
 */
public final class CollectionSink<T>
{
    private final Processor<T> processor;

    private CollectionSink (final Processor<T> processor)
    {
        this.processor = processor;
    }

    /**
     * Input Connection.
     *
     * @return the input that provides the messages to add to the collection.
     */
    public Input<T> dataIn ()
    {
        return processor.dataIn();
    }

    /**
     * Output Connection.
     *
     * @return the output that merely forwards the messages from data-in.
     */
    public Output<T> dataOut ()
    {
        return processor.dataOut();
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
        final Processor<T> proc = Processor.newProcessor(stage, (T x) -> collection.add(x));
        return new CollectionSink<>(proc);
    }
}
