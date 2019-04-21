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
package com.mackenziehigh.socius.flow;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mackenziehigh.cascade.Cascade.ActorFactory;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.FunctionScript;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Routes a message to the first-matching option (best-match) from a set of options.
 *
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public final class LookupYard<I, O>
        implements DataYard<I, O, LookupYard.LookupSiding<I, O>>
{
    /**
     * An option that may receive messages.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     */
    public interface LookupSiding<I, O>
            extends DataYard.Siding<I, O>
    {
        /**
         * Determines whether an incoming message should be routed to this option-handler.
         *
         * <p>
         * If this method returns true, then subsequently,
         * the message will be sent to the data-input.
         * </p>
         *
         * @param message is the message that may get routed hereto.
         * @return true, if the message should be routed hereto.
         */
        public boolean isMatch (I message);

        /**
         * This input will receive messages that match this option.
         *
         * @return the data-input of this option-handler.
         */
        @Override
        public Input<I> dataIn ();

        /**
         * The option-handler will send responses via this output.
         *
         * @return the data-output of this option-handler.
         */
        @Override
        public Output<O> dataOut ();
    }

    /**
     * These are the available options for processing incoming messages.
     */
    private final List<LookupSiding<I, O>> options;

    /**
     * These are the available options for processing incoming messages, as a Set.
     */
    private final Set<LookupSiding<I, O>> optionsSet;

    /**
     * This object will route incoming messages to the appropriate option-handlers.
     */
    private final Processor<I> router;

    /**
     * This object will receive any incoming messages with no corresponding option-handler.
     */
    private final Processor<I> deadDrop;

    /**
     * This object will route outgoing messages from the option-handlers to the common output.
     */
    private final Funnel<O> funnel;

    private LookupYard (final ActorFactory stage,
                        final List<LookupSiding<I, O>> mappers)
    {
        this.router = Processor.fromConsumerScript(stage, this::onInput);
        this.deadDrop = Processor.fromIdentityScript(stage);
        this.funnel = Funnel.newFunnel(stage);
        this.options = ImmutableList.copyOf(mappers);
        this.optionsSet = ImmutableSet.copyOf(mappers);

        for (LookupSiding<I, O> option : options)
        {
            option.dataOut().connect(funnel.dataIn(new Object()));
        }
    }

    private void onInput (final I message)
    {
        /**
         * Route the message to the first option
         * that is willing to accept it.
         */
        for (LookupSiding<I, O> option : options)
        {

            if (option.isMatch(message))
            {
                option.dataIn().send(message);
                return;
            }
        }

        /**
         * None of the options were willing to accept the message.
         * Therefore, route the message to the default output.
         */
        deadDrop.dataIn().send(message);
    }

    /**
     * Send messages to this input in order to cause them to be processed.
     *
     * @return the data-input.
     */
    @Override
    public Input<I> dataIn ()
    {
        return router.dataIn();
    }

    /**
     * Messages will be sent to this output, after they are successfully processed.
     *
     * @return the data-output.
     */
    @Override
    public Output<O> dataOut ()
    {
        return funnel.dataOut();
    }

    /**
     * Messages will be sent to this output, if they do not correspond to any option.
     *
     * @return the drop-output.
     */
    public Output<I> dropsOut ()
    {
        return deadDrop.dataOut();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<LookupSiding<I, O>> options ()
    {
        return optionsSet;
    }

    /**
     * Factory method.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     * @param stage will be used to create private actors.
     * @return a new builder that can build an option-hierarchy.
     */
    public static <I, O> Builder<I, O> newBuilder (final ActorFactory stage)
    {
        return new Builder(stage);
    }

    /**
     * Builder.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     */
    public static final class Builder<I, O>
    {
        private final ActorFactory stage;

        private final List<LookupSiding<I, O>> options = Lists.newLinkedList();

        private Builder (final ActorFactory stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        /**
         * Add an option to the hierarchy, which will have lower
         * priority than any option added previously.
         *
         * @param option is the additional option.
         * @return this.
         */
        public Builder<I, O> withOption (final LookupSiding<I, O> option)
        {
            Objects.requireNonNull(option, "option");
            options.add(option);
            return this;
        }

        /**
         * Add an option to the hierarchy, which will have lower
         * priority than any option added previously.
         *
         * @param condition provides the implementation of <code>isMatch()</code>.
         * @param transform will process messages sent to the option.
         * @return this.
         */
        public Builder<I, O> withOption (final Predicate<I> condition,
                                         final FunctionScript<I, O> transform)
        {
            Objects.requireNonNull(condition, "condition");
            Objects.requireNonNull(transform, "transform");
            final Mapper<I, O> mapper = Mapper.fromFunctionScript(stage, transform);
            final LookupSiding<I, O> option = new LookupSiding<I, O>()
            {
                @Override
                public boolean isMatch (final I message)
                {
                    return condition.test(message);
                }

                @Override
                public Input<I> dataIn ()
                {
                    return mapper.dataIn();
                }

                @Override
                public Output<O> dataOut ()
                {
                    return mapper.dataOut();
                }
            };
            withOption(option);
            return this;
        }

        /**
         * Construct the new object.
         *
         * @return the new option-hierarchy.
         */
        public LookupYard<I, O> build ()
        {
            return new LookupYard<>(stage, options);
        }
    }
}