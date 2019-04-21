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

import com.google.common.base.Preconditions;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Duplicates sequences of one-or-more messages as they pass through.
 *
 * <p>
 * In the most basic case (sequence length = 1, repeat count = 2),
 * a duplicator merely duplicates each incoming message and sends both
 * the original and the duplicate to the output stream. For example,
 * given the input sequence (X, Y, Z), then the output sequence
 * would be (X, X, Y, Y, Z, Z).
 * </p>
 *
 * <p>
 * In more advanced use-cases, subsequences will be duplicated.
 * For example, let (sequence length = 3, repeat count = 2).
 * Given the input sequence (A, B, C, D, E, F), then the output
 * sequence would be (A, B, C, A, B, C, D, E, F, D, E, F).
 * As can see, the two sub-sequences of length (3) were found.
 * The first subsequence of length (3) was (A, B, C).
 * The second subsequence of length (3) was (D, E, F).
 * Each subsequence was repeated twice and then forwarded.
 * </p>
 *
 * <p>
 * Notice that a duplicator is effectively a no-op,
 * if (sequence length = 1, repeat count = 1).
 * </p>
 *
 * @param <T> is the type of the incoming and outgoing messages.
 */
public final class Duplicator<T>
        implements DataPipeline<T, T>
{

    /**
     * Provides the data-output connector.
     */
    private final Processor<T> procDataIn;

    /**
     * Provides the data-input connector.
     */
    private final Processor<T> procDataOut;

    /**
     * This is used to hold the elements of the sequence
     * that will be repeated and forwarded.
     * We have to build this up until it reaches the sequence-length,
     * then we can repeat it, forward it, and reset it.
     */
    private final Deque<T> sequence;

    /**
     * This is the user-specified length of the subsequence.
     */
    private final int sequenceLength;

    /**
     * This is the user-specified number of times to repeat the subsequence.
     */
    private final int repeatCount;

    private Duplicator (final Builder<T> builder)
    {
        this.procDataIn = Processor.newConsumer(builder.stage, this::onMessage);
        this.procDataOut = Processor.newConnector(builder.stage);
        this.sequence = new ArrayDeque<>(builder.sequenceLength);
        this.sequenceLength = builder.sequenceLength;
        this.repeatCount = builder.repeatCount;
    }

    private void onMessage (final T message)
    {
        /**
         * Add the message to the subsequence,
         * until we reach the desired length.
         */
        sequence.add(message);

        /**
         * If the subsequence has reached the desired length,
         * then repeat it, forward it, and reset it.
         */
        if (sequenceLength == sequence.size())
        {
            for (int i = 0; i < repeatCount; i++)
            {
                for (T element : sequence)
                {
                    procDataOut.dataIn().send(element);
                }
            }

            sequence.clear();
        }
    }

    /**
     * Input Connection.
     *
     * @return the data-input that provides the messages to repeat.
     */
    @Override
    public Input<T> dataIn ()
    {
        return procDataIn.dataIn();
    }

    /**
     * Output Connection.
     *
     * @return the data-output that receives the repeated subsequences.
     */
    @Override
    public Output<T> dataOut ()
    {
        return procDataOut.dataOut();
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @return a builder that can construct the new duplicator.
     */
    public static <T> Builder<T> newDuplicator (final Stage stage)
    {
        return new Builder<>(stage);
    }

    /**
     * Builder.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     */
    public static final class Builder<T>
    {
        private final Stage stage;

        private int sequenceLength = 1;

        private int repeatCount = 1;

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        /**
         * Specify the length of the subsequences to repeat.
         *
         * @param value is the length of each subsequence.
         * @return this.
         */
        public Builder<T> withSequenceLength (final int value)
        {
            Preconditions.checkArgument(value >= 0, "sequence length < 0");
            this.sequenceLength = value;
            return this;
        }

        /**
         * Specify the number of times to repeat each of the subsequences.
         *
         * @param value is the number of repetitions.
         * @return this.
         */
        public Builder<T> withRepeatCount (final int value)
        {
            Preconditions.checkArgument(value >= 0, "repeat count < 0");
            this.repeatCount = value;
            return this;
        }

        /**
         * Build the new object.
         *
         * @return the new object.
         */
        public Duplicator<T> build ()
        {
            return new Duplicator<>(this);
        }
    }
}
