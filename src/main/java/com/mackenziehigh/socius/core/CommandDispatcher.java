/*
 * Copyright 2019 Michael Mackenzie High
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mackenziehigh.socius.core;

import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

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
    /**
     * This function will extract a key, from an incoming message,
     * which will be used to route the message to the corresponding function.
     */
    private final Function<I, K> keyFunction;

    /**
     * This map maps the names of functions to those functions.
     */
    private final Map<K, Function<I, O>> namedFunctions;

    /**
     * This actor receives the input messages and invokes
     * the corresponding functions synchronously.
     */
    private final Pipeline<I, O> engine;

    /**
     * This actor provides the drops-out connector.
     */
    private final Processor<I> dropsConnector;

    private CommandDispatcher (final Builder<K, I, O> builder)
    {
        this.engine = Pipeline.fromFunctionScript(builder.stage, this::onInput);
        this.dropsConnector = Processor.fromIdentityScript(builder.stage);
        this.keyFunction = builder.keyFunction;
        this.namedFunctions = Map.copyOf(builder.namedFunctions);
    }

    private O onInput (final I message)
    {
        /**
         * Obtain the name of the function to execute.
         */
        final K key = keyFunction.apply(message);

        /**
         * Obtain that function, if such a function was declared.
         */
        final Function<I, O> namedFunction = namedFunctions.get(key);

        if (namedFunction == null)
        {
            /**
             * Drop the message, because no corresponding function exists.
             */
            dropsConnector.accept(message);
            return null;
        }
        else
        {
            /**
             * Process the message, since a corresponding function exists.
             */
            final O output = namedFunction.apply(message);
            return output;
        }
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
     * Output Connection.
     *
     * @return the output that receives the dropped input messages.
     */
    public Output<I> dropsOut ()
    {
        return dropsConnector.dataOut();
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

        /**
         * Declare a function that can be invoked by name only.
         *
         * <p>
         * When an input message arrives, the key-function will be used
         * to obtain the name of the function to invoke therefrom.
         * If that name matches the name declared here,
         * then the function declared here will be invoked.
         * </p>
         *
         * @param key is the name of the function being declared.
         * @param functor is the function being declared.
         * @return this.
         */
        public Builder<K, I, O> declareFunction (final K key,
                                                 final Function<I, O> functor)
        {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(functor, "functor");
            namedFunctions.put(key, functor);
            return this;
        }

        /**
         * Declare a function that can be invoked by name only.
         *
         * <p>
         * When an input message arrives, the key-function will be used
         * to obtain the name of the function to invoke therefrom.
         * If that name matches the name declared here,
         * then the function declared here will be invoked.
         * </p>
         *
         * @param key is the name of the function being declared.
         * @param functor is the function being declared.
         * @return this.
         */
        public Builder<K, I, O> declareConsumer (final K key,
                                                 final Consumer<I> functor)
        {
            final Function<I, O> wrapper = (x) ->
            {
                functor.accept(x);
                return null;
            };

            return declareFunction(key, wrapper);
        }

        /**
         * Construct the new dispatcher.
         *
         * @return the new object.
         */
        public CommandDispatcher<K, I, O> build ()
        {
            Objects.requireNonNull("A key-function is required.");
            return new CommandDispatcher<>(this);
        }
    }
}
