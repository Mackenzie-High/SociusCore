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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Provides a request/reply mechanism.
 *
 * @param <K> is the type of the key used to correlate requests and replies.
 * @param <I> is the type of the request messages.
 * @param <R> is the type of the reply messages.
 * @param <O> is the type of message created by combining a request and a reply.
 */
public final class Requester<K, I, R, O>
{
    /**
     * All of the actors contained herein are on this stage.
     */
    private final Stage stage;

    /**
     * This actor handles incoming requests.
     */
    private final Processor<I> dataIn;

    /**
     * This actor provides the requests-out connector.
     */
    private final Processor<I> requestOut;

    /**
     * This actor handles incoming replies.
     */
    private final Processor<R> replyIn;

    /**
     * This actor provides the responses-out connector.
     */
    private final Processor<O> resultOut;

    /**
     * This actor provides the drops-requests connector.
     */
    private final Processor<I> droppedRequestOut;

    /**
     * This actor provides the drops-replies connector.
     */
    private final Processor<R> droppedReplyOut;

    /**
     * This is the amount of time to wait after forwarding a request,
     * before the request is forwarded again (retry occurs).
     */
    private final Duration timeout;

    /**
     * This is the number of times to forward a request before giving up.
     */
    private final int tries;

    /**
     * This function is used to extract the key from request-messages.
     */
    private final Function<I, K> keyFuncI;

    /**
     * This function is used to extract the key from reply-messages.
     */
    private final Function<R, K> keyFuncR;

    /**
     * This function is used to merge a request-message and a
     * reply-message into a single output message to forward.
     */
    private final BiFunction<I, R, O> composer;

    /**
     * This lock prevents the actors herein from stomping on one another.
     */
    private final Object lock = new Object();

    /**
     * This map maps a key to the the handler that is handling the
     * request/reply process for the messages identified by that key.
     */
    private final Map<K, Handler> handlers = Maps.newConcurrentMap();

    /**
     * This actor is used to request a callback at a specified time in the future.
     */
    private final DelayedSender delayedSender;

    private Requester (final Builder<K, I, R, O> builder)
    {
        this.stage = builder.stage;
        this.dataIn = Processor.fromConsumerScript(stage, this::onRequestIn);
        this.requestOut = Processor.fromIdentityScript(stage);
        this.replyIn = Processor.fromConsumerScript(stage, this::onReplyIn);
        this.resultOut = Processor.fromIdentityScript(stage);
        this.droppedRequestOut = Processor.fromIdentityScript(stage);
        this.droppedReplyOut = Processor.fromIdentityScript(stage);
        this.keyFuncI = builder.keyFuncI;
        this.keyFuncR = builder.keyFuncR;
        this.composer = builder.composer;
        this.timeout = builder.timeout;
        this.tries = builder.tries;
        this.delayedSender = builder.delayedSender != null ? builder.delayedSender : DelayedSender.newDelayedSender();
    }

    /**
     * Getter.
     *
     * @return the number of pending requests.
     */
    public int pendingRequestCount ()
    {
        return handlers.size();
    }

    /**
     * Determine whether the default <code>DelayedSender</code> object is being used.
     *
     * @return true, if the default executor under the covers.
     */
    public boolean isUsingDefaultDelayedSender ()
    {
        return delayedSender.isUsingDefaultExecutor();
    }

    /**
     * Send requests to this input.
     *
     * @return the request-input.
     */
    public Input<I> requestIn ()
    {
        return dataIn.dataIn();
    }

    /**
     * Requests will be forwarded to this output.
     *
     * @return the request-output.
     */
    public Output<I> requestOut ()
    {
        return requestOut.dataOut();
    }

    /**
     * Send replies to this input.
     *
     * @return the reply-input.
     */
    public Input<R> replyIn ()
    {
        return replyIn.dataIn();
    }

    /**
     * The result of correlating a request and a
     * reply will be transmitted via this output.
     *
     * @return the result-output.
     */
    public Output<O> resultOut ()
    {
        return resultOut.dataOut();
    }

    /**
     * Dropped requests will be sent to this output.
     *
     * @return the output.
     */
    public Output<I> droppedRequestOut ()
    {
        return droppedRequestOut.dataOut();
    }

    /**
     * Dropped replies will be sent to this output.
     *
     * @return the output.
     */
    public Output<R> droppedReplyOut ()
    {
        return droppedReplyOut.dataOut();
    }

    private void onRequestIn (final I message)
    {
        /**
         * Obtain the key that identifies the message.
         * If the message is already being handled (duplicate message),
         * then drop the message; otherwise, create a handler that
         * will handle the request/reply sequence of operations.
         */
        synchronized (lock)
        {
            final K key = keyFuncI.apply(message);

            if (handlers.containsKey(key) == false)
            {
                final Handler handler = new Handler(key, message);
                handlers.put(key, handler);
                handler.send();
            }
            else
            {
                droppedRequestOut.accept(message);
            }
        }
    }

    private void onReplyIn (final R message)
    {
        /**
         * Obtain the key that identifies the message.
         * If we are still interested in the message,
         * then there will be an associated handler.
         * Notify the handler of the received reply.
         */
        synchronized (lock)
        {
            final K key = keyFuncR.apply(message);

            final Handler handler = handlers.get(key);

            if (handler != null)
            {
                handler.recv(message);
            }
            else
            {
                droppedReplyOut.accept(message);
            }
        }
    }

    /**
     * Factory Method.
     *
     * @param stage will be used to create private actors.
     * @return a new builder that can build the desired object.
     * @param <K> is the type of the key used to correlate requests and replies.
     * @param <I> is the type of the request messages.
     * @param <R> is the type of the reply messages.
     * @param <O> is the type of message created by combining a request and a reply.
     */
    public static <K, I, R, O> Builder<K, I, R, O> newRequester (final Stage stage)
    {
        return new Builder(stage);
    }

    /**
     * Handles the request/reply sequence of actions for a single message.
     */
    private final class Handler
    {
        /**
         * This key identifies both the request and the corresponding reply.
         */
        private final K key;

        /**
         * This is the request that is being handled.
         * This request is identified by the (above) key.
         */
        private final I request;

        /**
         * This flag will become true, when either:
         * (1) a reply is received,
         * (2) the request times-out and all retries are exhausted.
         */
        private boolean cancelled = false;

        /**
         * This is the number of times that we have forwarded the request.
         * In between each send, we will wait for the timeout to expire.
         * Once the number of sends exceeds the retry-limit,
         * we will give up and drop the request.
         */
        private int sent = 0;

        /**
         * This is the reply that was received, if any.
         */
        private R reply;

        /**
         * This actor will receive a message whenever the timeout expires.
         */
        private final Processor<Object> callback = Processor.fromConsumerScript(stage, this::onTimeoutExpired);

        public Handler (final K key,
                        final I request)
        {
            this.key = key;
            this.request = request;
        }

        public void send ()
        {
            synchronized (lock)
            {
                if (cancelled)
                {
                    /**
                     * Since we always schedule a callback when forwarding a request,
                     * we will hit this case one-time, once we receive a reply.
                     * Although the reply was already processed, the delayed-sender
                     * will send us our requested wakeup call after we processed the reply.
                     * In that case, we do not want to process the reply again.
                     * Likewise, we do not want to report the request as dropped,
                     * when we already reported that a reply was received.
                     */
                    return;
                }
                else if (reply != null)
                {
                    /**
                     * We got a reply.
                     * Create a single message from the request and the reply pair.
                     * Forward the combined message.
                     * Prevent this method from executing again unintentionally.
                     */
                    final O response = composer.apply(request, reply);
                    resultOut.dataIn().send(response);
                    cancel();
                }
                else if (sent >= tries)
                {
                    /**
                     * The request has timed-out multiple times.
                     * Each time, we resent the request and waited.
                     * Now, we have exceeded the retry limit.
                     * Give up and drop the request.
                     * Prevent this method from executing again unintentionally.
                     */
                    droppedRequestOut.dataIn().send(request);
                    cancel();
                }
                else
                {
                    /**
                     * Forward the request.
                     * Schedule this method to execute again, when the request times-out.
                     */
                    ++sent;
                    requestOut.dataIn().send(request);
                    delayedSender.send(callback.dataIn(), this, timeout);
                }
            }
        }

        public void recv (final R replyMsg)
        {
            synchronized (lock)
            {
                if (cancelled == false)
                {
                    reply = replyMsg;
                    send();
                }
            }
        }

        private void onTimeoutExpired (final Object ignored)
        {
            send();
        }

        private void cancel ()
        {
            cancelled = true;
            handlers.remove(key);
        }
    }

    /**
     * Builder.
     *
     * @param <K> is the type of the key used to correlate requests and replies.
     * @param <I> is the type of the request messages.
     * @param <R> is the type of the reply messages.
     * @param <O> is the type of message created by combining a request and a reply.
     */
    public static final class Builder<K, I, R, O>
    {
        private final Stage stage;

        private Function<I, K> keyFuncI;

        private Function<R, K> keyFuncR;

        private BiFunction<I, R, O> composer;

        private Integer tries;

        private Duration timeout;

        private DelayedSender delayedSender;

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        /**
         * Specify how to extract the key identifier from request-messages.
         *
         * @param functor will be used to identify request-messages.
         * @return this.
         */
        public Builder<K, I, R, O> withRequestKeyFunction (final Function<I, K> functor)
        {
            this.keyFuncI = Objects.requireNonNull(functor, "functor");
            return this;
        }

        /**
         * Specify how to extract the key identifier from reply-messages.
         *
         * @param functor will be used to identify reply-messages.
         * @return this.
         */
        public Builder<K, I, R, O> withReplyKeyFunction (final Function<R, K> functor)
        {
            this.keyFuncR = Objects.requireNonNull(functor, "functor");
            return this;
        }

        /**
         * Specify how to combine a request and a reply into a single result.
         *
         * @param functor will be used to combine requests and replies.
         * @return this.
         */
        public Builder<K, I, R, O> withComposer (final BiFunction<I, R, O> functor)
        {
            this.composer = Objects.requireNonNull(functor, "functor");
            return this;
        }

        /**
         * Specify how long to wait for a reply before forwarding the request again.
         *
         * @param timeout is how long to wait for replies.
         * @return this.
         */
        public Builder<K, I, R, O> withTimeout (final Duration timeout)
        {
            this.timeout = Objects.requireNonNull(timeout, "timeout");
            return this;
        }

        /**
         * Specify the maximum number of times to send a request,
         * which includes the first send, plus any necessary retries.
         *
         * @param limit is maximum number of times a request will be sent.
         * @return this.
         */
        public Builder<K, I, R, O> withTries (final int limit)
        {
            Preconditions.checkArgument(limit >= 1, "limit < 1");
            this.tries = limit;
            return this;
        }

        /**
         * Provide an actor for internal use.
         *
         * <p>
         * You may want to provide this actor explicitly in
         * order to minimize the number of threads in-use.
         * </p>
         *
         * @param sender will be used to generate callbacks.
         * @return this.
         */
        public Builder<K, I, R, O> withDelayedSender (final DelayedSender sender)
        {
            this.delayedSender = Objects.requireNonNull(sender, "sender");
            return this;
        }

        /**
         * Build.
         *
         * @return the new object.
         */
        public Requester<K, I, R, O> build ()
        {
            return new Requester<>(this);
        }
    }
}
