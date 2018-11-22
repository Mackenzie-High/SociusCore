package com.mackenziehigh.socius.actors;

import com.google.common.base.Verify;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

/**
 * Filter out duplicates from a stream of messages.
 *
 * @param <K> is the type of the identifiers (keys) derived from the messages.
 * @param <T> is the type of the messages themselves.
 */
public final class Deduplicator<K, T>
{
    private final Actor<T, T> actor;

    private final Function<T, K> keyFunction;

    private final Queue<K> queue;

    private final Set<K> set;

    public Deduplicator (final Stage stage,
                         final Function<T, K> keyFunction,
                         final int capacity)
    {
        this.actor = stage.newActor().withScript(this::onMessage).create();
        this.keyFunction = keyFunction;
        this.set = Sets.newHashSetWithExpectedSize(capacity);
        this.queue = Queues.newArrayBlockingQueue(capacity);
    }

    /**
     * Messages will flow from the stream into this data-input,
     * and will later be forwarded to the data-output,
     * if they are non-duplicates.
     *
     * @return the data-input.
     */
    public Input<T> dataIn ()
    {
        return actor.input();
    }

    /**
     * Non-duplicate messages will be forwarded to this output.
     *
     * @return the data-output.
     */
    public Output<T> dataOut ()
    {
        return actor.output();
    }

    private T onMessage (final T message)
    {
        final K key = keyFunction.apply(message);

        if (set.contains(key))
        {
            return null;
        }
        else
        {
            prune();
            queue.add(key);
            set.add(key);
            return message;
        }
    }

    private void prune ()
    {
        if (set.isEmpty() == false)
        {
            set.remove(queue.poll());
        }

        Verify.verify(queue.size() == set.size());
    }

    /**
     * Create a builder that can build a new <code>Deduplicator</code>.
     *
     * @param <K> is the type of the identifiers (keys) derived from the messages.
     * @param <T> is the type of the messages themselves.
     * @param stage will be used to create private actors.
     * @return the new builder.
     */
    public static <K, T> Builder<K, T> newDeduplicator (final Stage stage)
    {
        return new Builder<>(stage);
    }

    /**
     * Builder.
     *
     * @param <K> is the type of the identifiers (keys) derived from the messages.
     * @param <T> is the type of the messages themselves.
     */
    public static final class Builder<K, T>
    {
        private final Stage stage;

        private Function<T, K> keyFunction;

        private int capacity = 100;

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        /**
         * Specify a function that will assign an identifier (key)
         * to each messages that passes through the <code>Deduplicator</code>.
         *
         * @param functor will assign identifiers.
         * @return this.
         */
        public Builder<K, T> withKeyFunction (final Function<T, K> functor)
        {
            this.keyFunction = Objects.requireNonNull(functor, "functor");
            return this;
        }

        /**
         * Specify how many identifiers can be stored in memory simultaneously.
         *
         * @param capacity is the maximum number of stored identifiers.
         * @return this.
         */
        public Builder<K, T> withCapacity (final int capacity)
        {
            this.capacity = capacity;
            return this;
        }

        /**
         * Build.
         *
         * @return the newly constructed object.
         */
        public Deduplicator<K, T> build ()
        {
            return new Deduplicator<>(stage, keyFunction, capacity);
        }
    }

}
