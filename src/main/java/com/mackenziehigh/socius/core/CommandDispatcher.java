package com.mackenziehigh.socius.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Dispatches messages to previously declared functions.
 *
 * @param <K> is the type of the keys that identify the named functions.
 * @param <I> is the type of messages that the dispatcher will consume.
 * @param <O> is the type of messages that the dispatcher will produce.
 */
public final class CommandDispatcher<K, I, O>
        implements Pipeline<I, O>
{
    private final Function<I, K> keyFunction;

    private final Map<K, Function<I, O>> namedFunctions;

    private final List<Predicate<I>> anonPredicates;

    private final List<Function<I, O>> anonFunctions;

    private final Pipeline<I, O> engine;

    private CommandDispatcher (final Builder<K, I, O> builder)
    {
        this.engine = Pipeline.fromFunctionScript(builder.stage, this::onInput);
        this.keyFunction = builder.keyFunction;
        this.namedFunctions = Map.copyOf(builder.namedFunctions);
        this.anonPredicates = List.copyOf(builder.anonPredicates);
        this.anonFunctions = List.copyOf(builder.anonFunctions);
    }

    private O onInput (final I message)
    {
        if (keyFunction == null)
        {
            return onInputWithoutNamedFunctions(message);
        }
        else
        {
            return onInputWithNamedFunctions(message);
        }
    }

    private O onInputWithNamedFunctions (final I message)
    {
        /**
         * Obtain the name of the function to execute.
         */
        final K key = keyFunction.apply(message);

        /**
         * Obtain that function, if such a function was declared.
         */
        final Function<I, O> namedFunction = namedFunctions.get(key);

        if (namedFunction != null)
        {
            final O output = namedFunction.apply(message);
            return output;
        }
        else
        {
            final O output = onInputWithoutNamedFunctions(message);
            return output;
        }
    }

    private O onInputWithoutNamedFunctions (final I message)
    {
        final int count = anonFunctions.size();

        for (int i = 0; i < count; i++)
        {
            final Predicate<I> condition = anonPredicates.get(i);
            final Function<I, O> function = anonFunctions.get(i);

            if (condition.test(message))
            {
                return function.apply(message);
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input<I> dataIn ()
    {
        return engine.dataIn();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Output<O> dataOut ()
    {
        return engine.dataOut();
    }

    /**
     * Factory Method.
     *
     * @param <K> is the type of the keys that identify the named functions.
     * @param <I> is the type of messages that the dispatcher will consume.
     * @param <O> is the type of messages that the dispatcher will produce.
     * @param stage will be used to create private actors.
     * @return a builder that can build the desired object.
     */
    public static <K, I, O> Builder<K, I, O> newCommandDispatcher (final Stage stage)
    {
        return new Builder<>(stage);
    }

    /**
     * Builder.
     *
     * @param <K> is the type of the keys that identify the named functions.
     * @param <I> is the type of messages that the dispatcher will consume.
     * @param <O> is the type of messages that the dispatcher will produce.
     */
    public static final class Builder<K, I, O>
    {
        private final Stage stage;

        private Function<I, K> keyFunction;

        private final Map<K, Function<I, O>> namedFunctions = Maps.newHashMap();

        private final List<Predicate<I>> anonPredicates = Lists.newLinkedList();

        private final List<Function<I, O>> anonFunctions = Lists.newLinkedList();

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        /**
         * Specify a function that will extract the names of declared functions
         * from incoming messages, so that those functions can be invoked.
         *
         * @param functor extracts function names from input messages.
         * @return this.
         */
        public Builder<K, I, O> withKeyFunction (final Function<I, K> functor)
        {
            this.keyFunction = Objects.requireNonNull(functor, "functor");
            return this;
        }

        public Builder<K, I, O> declareFunction (final String key,
                                                 final Function<I, O> functor)
        {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(functor, "functor");
            return this;
        }

        public Builder<K, I, O> declareFunction (final Predicate<I> condition,
                                                 final Function<I, O> functor)
        {
            Objects.requireNonNull(condition, "condition");
            Objects.requireNonNull(functor, "functor");
            anonPredicates.add(condition);
            anonFunctions.add(functor);
            return this;
        }

        public Builder<K, I, O> declareConsumer (final String key,
                                                 final Consumer<I> functor)
        {
            final Function<I, O> wrapper = (x) ->
            {
                functor.accept(x);
                return null;
            };

            return declareFunction(key, wrapper);
        }

        public Builder<K, I, O> declareConsumer (final Predicate<I> condition,
                                                 final Consumer<I> functor)
        {
            final Function<I, O> wrapper = (x) ->
            {
                functor.accept(x);
                return null;
            };

            return declareFunction(condition, wrapper);
        }

        public CommandDispatcher<K, I, O> build ()
        {
            Preconditions.checkState(anonFunctions.isEmpty() || keyFunction != null, "A key-function is required.");
            return new CommandDispatcher<>(this);
        }
    }

    public static void main (String[] args)
    {
        var stage = Cascade.newStage();
        var actor = CommandDispatcher.<String, String, String>newCommandDispatcher(stage)
                .declareConsumer("main", x -> System.out.println("main"))
                .declareConsumer(x -> x.equals("main"), x -> System.out.println("main"))
                .build();

    }
}
