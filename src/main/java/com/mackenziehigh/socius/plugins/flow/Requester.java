package com.mackenziehigh.socius.plugins.flow;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 *
 */
public final class Requester<K, I, R, O>
{
    private final Actor<I, I> dataIn;

    private final Actor<I, I> requestOut;

    private final Actor<R, R> responseIn;

    private final Actor<O, O> resonseOut;

    private final Actor<I, I> dropOut;

    private final Duration timeout;

    private final int tries;

    private final int capacity;

    private final Function<I, K> keyFuncI;

    private final Function<R, K> keyFuncR;

    private final BiFunction<I, R, O> composer;

    private final Object lock = new Object();

    private final LinkedHashMap<K, I> requestObjects = new LinkedHashMap<>();

    private final LinkedHashMap<K, Instant> requestTimes = new LinkedHashMap<>();

    private Requester (final Stage stage,
                       final Function<I, K> keyFuncI,
                       final Function<R, K> keyFuncR,
                       final BiFunction<I, R, O> composer,
                       final Duration timeout,
                       final int tries,
                       final int capacity)
    {
        this.dataIn = stage.newActor().withScript(this::onDataIn).create();
        this.requestOut = stage.newActor().withScript(this::onRequestOut).create();
        this.responseIn = stage.newActor().withScript(this::onResponseIn).create();
        this.resonseOut = stage.newActor().withScript(this::onResponseOut).create();
        this.dropOut = stage.newActor().withScript(this::onDropOut).create();
        this.keyFuncI = keyFuncI;
        this.keyFuncR = keyFuncR;
        this.composer = composer;
        this.timeout = timeout;
        this.tries = tries;
        this.capacity = capacity;
    }

    public Input<I> dataIn ()
    {
        return dataIn.input();
    }

    public Output<I> requestOut ()
    {
        return requestOut.output();
    }

    public Input<R> responseIn ()
    {
        return responseIn.input();
    }

    public Output<O> responseOut ()
    {
        return resonseOut.output();
    }

    public Output<I> dropOut ()
    {
        return dropOut.output();
    }

    private void onDataIn (final I message)
    {
        synchronized (lock)
        {
            final K key = keyFuncI.apply(message);
            final Instant now = Instant.now();

            if (requestObjects.containsKey(key))
            {
                dropOut.accept(message);
                return;
            }
            else
            {
                requestObjects.put(key, message);
                requestTimes.put(key, now);
                requestOut.accept(message);
            }
        }
    }

    private I onRequestOut (final I message)
    {
        return message;
    }

    private void onResponseIn (final R message)
    {
        final I request = null;
        final R response = message;
        final O composite = composer.apply(request, response);
        resonseOut.accept(composite);
    }

    private O onResponseOut (final O message)
    {
        return message;
    }

    private I onDropOut (final I message)
    {
        return message;
    }

    public static Builder newRequester (final Stage stage)
    {
        return new Builder(stage);
    }

    public static final class Builder<K, I, R, O>
    {
        private final Stage stage;

        private Function<I, K> keyFuncI;

        private Function<R, K> keyFuncR;

        private BiFunction<I, R, O> composer;

        private Duration timeout = Duration.ofSeconds(1);

        private int tries = 1;

        private int capacity = 1000;

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        public Builder<K, I, R, O> withRequestKeyFunction (final Function<I, K> functor)
        {
            this.keyFuncI = Objects.requireNonNull(functor, "functor");
            return this;
        }

        public Builder<K, I, R, O> withResponseKeyFunction (final Function<R, K> functor)
        {
            this.keyFuncR = Objects.requireNonNull(functor, "functor");
            return this;
        }

        public Builder<K, I, R, O> withComposer (final BiFunction<I, R, O> functor)
        {
            this.composer = Objects.requireNonNull(functor, "functor");
            return this;
        }

        public Builder<K, I, R, O> withTimeout (final Duration timeout)
        {
            this.timeout = Objects.requireNonNull(timeout, "timeout");
            return this;
        }

        public Builder<K, I, R, O> withTries (final int limit)
        {
            this.tries = limit;
            return this;
        }

        public Builder<K, I, R, O> withCapacity (final int capacity)
        {
            this.capacity = capacity;
            return this;
        }

        public Requester<K, I, R, O> build ()
        {
            return new Requester<>(stage, keyFuncI, keyFuncR, composer, timeout, tries, capacity);
        }
    }

    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();
        final Requester<String, String, String, String> req = Requester
                .newRequester(stage)
                .withCapacity(100)
                .withTries(5)
                .withTimeout(Duration.ofSeconds(1))
                .withRequestKeyFunction(x -> x)
                .withResponseKeyFunction(x -> x)
                .build();
    }
}
