package com.mackenziehigh.socius.flow;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.FunctionScript;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Routes a message to the first-matching option (best-match) from a set of options.
 *
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public final class OrderedChoice<I, O>
{
    /**
     * An option that may receive messages.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     */
    public interface Option<I, O>
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
        public Input<I> dataIn ();

        /**
         * The option-handler will send responses via this output.
         *
         * @return the data-output of this option-handler.
         */
        public Output<O> dataOut ();
    }

    /**
     * These are the available options for processing incoming messages.
     */
    private final List<Option<I, O>> options;

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

    private OrderedChoice (final Stage stage,
                           final List<Option<I, O>> mappers)
    {
        this.router = Processor.newConsumer(stage, this::onInput);
        this.deadDrop = Processor.newConnector(stage);
        this.funnel = Funnel.newFunnel(stage);
        this.options = ImmutableList.copyOf(mappers);

        for (Option<I, O> option : options)
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
        for (Option<I, O> option : options)
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
    public Input<I> dataIn ()
    {
        return router.dataIn();
    }

    /**
     * Messages will be sent to this output, after they are successfully processed.
     *
     * @return the data-output.
     */
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
     * Factory method.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     * @param stage will be used to create private actors.
     * @return a new builder that can build an option-hierarchy.
     */
    public static <I, O> Builder<I, O> newBuilder (final Stage stage)
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
        private final Stage stage;

        private final List<Option<I, O>> options = Lists.newLinkedList();

        private Builder (final Stage stage)
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
        public Builder<I, O> withOption (final Option<I, O> option)
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
            final Mapper<I, O> mapper = Mapper.newFunction(stage, transform);
            final Option<I, O> option = new Option<I, O>()
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
            options.add(option);
            return this;
        }

        /**
         * Construct the new object.
         *
         * @return the new option-hierarchy.
         */
        public OrderedChoice<I, O> build ()
        {
            return new OrderedChoice<>(stage, options);
        }
    }
}
