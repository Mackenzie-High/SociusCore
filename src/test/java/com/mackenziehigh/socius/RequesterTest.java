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
package com.mackenziehigh.socius;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class RequesterTest
{
    /**
     * Fake Data Type.
     */
    private final class IdentifierClass
    {
        // Pass.
    }

    /**
     * Fake Data Type.
     */
    private final class RequestClass
    {
        public final IdentifierClass id;

        public final String value;

        public RequestClass (final IdentifierClass id,
                             final String value)
        {
            this.id = id;
            this.value = value;
        }
    }

    /**
     * Fake Data Type.
     */
    private final class ReplyClass
    {
        public final IdentifierClass id;

        public final String value;

        public ReplyClass (final IdentifierClass id,
                           final String value)
        {
            this.id = id;
            this.value = value;
        }
    }

    /**
     * Fake Data Type.
     */
    private final class ResultClass
    {
        public final RequestClass request;

        public final ReplyClass reply;

        public ResultClass (final RequestClass request,
                            final ReplyClass reply)
        {
            this.request = request;
            this.reply = reply;
        }

        @Override
        public int hashCode ()
        {
            int hash = 7;
            hash = 17 * hash + Objects.hashCode(this.request);
            hash = 17 * hash + Objects.hashCode(this.reply);
            return hash;
        }

        @Override
        public boolean equals (Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            final ResultClass other = (ResultClass) obj;
            return Objects.equals(request, other.request) && Objects.equals(reply, other.reply);
        }
    }

    private static final long TIMEOUT_MILLIS = 200;

    private final AsyncTestTool tester = new AsyncTestTool();

    private final Requester<IdentifierClass, RequestClass, ReplyClass, ResultClass> requester = Requester
            .<IdentifierClass, RequestClass, ReplyClass, ResultClass>newRequester(tester.stage())
            .withTries(1 + 5) // Initial + Up to (5) Retries
            .withTimeout(Duration.ofMillis(TIMEOUT_MILLIS))
            .withCorrelator((RequestClass x, ReplyClass y) -> new ResultClass(x, y))
            .withRequestKeyFunction(x -> x.id)
            .withReplyKeyFunction(x -> x.id)
            .build();

    /**
     * Test: 20190102033656219973
     *
     * <p>
     * Case: Throughput of Normal Request with Normal Reply.
     * </p>
     */
    @Test
    public void test20190102033656219973 ()
    {
        final IdentifierClass id = new IdentifierClass();
        final RequestClass request = new RequestClass(id, "Neptune");
        final ReplyClass reply = new ReplyClass(id, "Jovian");
        final ResultClass result = new ResultClass(request, reply);

        tester.connect(requester.droppedReplyOut());
        tester.connect(requester.droppedRequestOut());
        tester.connect(requester.requestOut());
        tester.connect(requester.resultOut());

        /**
         * Send the request through the requester.
         * The requester will forward the request.
         */
        assertEquals(0, requester.pendingRequestCount());
        requester.requestIn().send(request);
        tester.awaitEquals(requester.requestOut(), request);
        assertEquals(1, requester.pendingRequestCount());

        /**
         * Send a reply back to the requester.
         */
        requester.replyIn().send(reply);

        /**
         * The requester will correlate the request and reply.
         * The requester will then send the combined result.
         */
        tester.awaitEquals(requester.resultOut(), result);
        assertEquals(0, requester.pendingRequestCount());
    }

    /**
     * Test: 20190102033656220076
     *
     * <p>
     * Case: Request Timed Out, Retry Successful.
     * </p>
     */
    @Test
    public void test20190102033656220076 ()
    {
        final IdentifierClass id = new IdentifierClass();
        final RequestClass request = new RequestClass(id, "Neptune");
        final ReplyClass reply = new ReplyClass(id, "Jovian");
        final ResultClass result = new ResultClass(request, reply);

        tester.connect(requester.droppedReplyOut());
        tester.connect(requester.droppedRequestOut());
        tester.connect(requester.requestOut());
        tester.connect(requester.resultOut());

        /**
         * Send the request through the requester.
         * The requester will forward the request.
         */
        assertEquals(0, requester.pendingRequestCount());
        requester.requestIn().send(request);
        tester.awaitEquals(requester.requestOut(), request);
        assertEquals(1, requester.pendingRequestCount());

        /**
         * After the request times-out, it will be sent again (retried).
         */
        tester.awaitEquals(requester.requestOut(), request);

        /**
         * Send a reply back to the requester.
         */
        requester.replyIn().send(reply);

        /**
         * The requester will correlate the request and reply.
         * The requester will then send the combined result.
         */
        tester.awaitEquals(requester.resultOut(), result);
        assertEquals(0, requester.pendingRequestCount());
    }

    /**
     * Test: 20190102043344891456
     *
     * <p>
     * Case: Duplicate Request, Normal Reply.
     * </p>
     */
    @Test
    public void test20190102043344891456 ()
    {
        final IdentifierClass id = new IdentifierClass();
        final RequestClass request = new RequestClass(id, "Neptune");
        final ReplyClass reply = new ReplyClass(id, "Jovian");
        final ResultClass result = new ResultClass(request, reply);

        tester.connect(requester.droppedReplyOut());
        tester.connect(requester.droppedRequestOut());
        tester.connect(requester.requestOut());
        tester.connect(requester.resultOut());

        /**
         * Send the request through the requester.
         * The requester will forward the request.
         */
        assertEquals(0, requester.pendingRequestCount());
        requester.requestIn().send(request);
        requester.requestIn().send(request); // Duplicate Request.
        tester.awaitEquals(requester.droppedRequestOut(), request); // The duplicate request was dropped.
        tester.awaitEquals(requester.requestOut(), request); // The first request was forwarded.
        assertEquals(1, requester.pendingRequestCount());

        /**
         * Send a reply back to the requester.
         */
        requester.replyIn().send(reply);

        /**
         * The requester will correlate the request and reply.
         * The requester will then send the combined result.
         */
        tester.awaitEquals(requester.resultOut(), result);
        assertEquals(0, requester.pendingRequestCount());
    }

    /**
     * Test: 20190102033656220182
     * <p>
     * Case: Normal Request, Duplicate Reply.
     * </p>
     */
    @Test
    public void test20190102033656220182 ()
    {
        final IdentifierClass id = new IdentifierClass();
        final RequestClass request = new RequestClass(id, "Neptune");
        final ReplyClass reply = new ReplyClass(id, "Jovian");
        final ResultClass result = new ResultClass(request, reply);

        tester.connect(requester.droppedReplyOut());
        tester.connect(requester.droppedRequestOut());
        tester.connect(requester.requestOut());
        tester.connect(requester.resultOut());

        /**
         * Send the request through the requester.
         * The requester will forward the request.
         */
        assertEquals(0, requester.pendingRequestCount());
        requester.requestIn().send(request);
        tester.awaitEquals(requester.requestOut(), request);
        assertEquals(1, requester.pendingRequestCount());

        /**
         * Send a reply back to the requester.
         */
        requester.replyIn().send(reply);
        requester.replyIn().send(reply); // Duplicate Reply.
        tester.awaitEquals(requester.droppedReplyOut(), reply); // The duplicate was dropped.

        /**
         * The requester will correlate the request and reply.
         * The requester will then send the combined result.
         */
        tester.awaitEquals(requester.resultOut(), result);
        assertEquals(0, requester.pendingRequestCount());
    }

    /**
     * Test: 20190102050927530236
     *
     * <p>
     * Case: Multiple Retries.
     * </p>
     */
    @Test
    public void test20190102050927530236 ()
    {
        final IdentifierClass id = new IdentifierClass();
        final RequestClass request = new RequestClass(id, "Neptune");
        final ReplyClass reply = new ReplyClass(id, "Jovian");

        tester.connect(requester.droppedReplyOut());
        tester.connect(requester.droppedRequestOut());
        tester.connect(requester.requestOut());
        tester.connect(requester.resultOut());

        /**
         * Send the request through the requester.
         * The requester will forward the request.
         */
        assertEquals(0, requester.pendingRequestCount());
        requester.requestIn().send(request);
        tester.awaitEquals(requester.requestOut(), request);
        tester.awaitEquals(requester.requestOut(), request); // Retry #1.
        assertEquals(1, requester.pendingRequestCount());
        tester.awaitEquals(requester.requestOut(), request); // Retry #2.
        assertEquals(1, requester.pendingRequestCount());
        tester.awaitEquals(requester.requestOut(), request); // Retry #3.
        assertEquals(1, requester.pendingRequestCount());
        tester.awaitEquals(requester.requestOut(), request); // Retry #4.
        assertEquals(1, requester.pendingRequestCount());
        tester.awaitEquals(requester.requestOut(), request); // Retry #5.
        assertEquals(1, requester.pendingRequestCount());

        /**
         * The request timed-out.
         */
        tester.awaitEquals(requester.droppedRequestOut(), request);
        assertEquals(0, requester.pendingRequestCount());

        /**
         * Send a reply back to the requester.
         */
        requester.replyIn().send(reply);

        /**
         * The requester will drop the reply,
         * because the request timed-out.
         */
        tester.awaitEquals(requester.droppedReplyOut(), reply);
        assertEquals(0, requester.pendingRequestCount());
    }

    /**
     * Test: 20190102052221744099
     *
     * <p>
     * Case: Which Delayed Sender is in use?
     * </p>
     */
    @Test
    public void test20190102052221744099 ()
    {
        final Requester<IdentifierClass, RequestClass, ReplyClass, ResultClass> requesterX = Requester
                .<IdentifierClass, RequestClass, ReplyClass, ResultClass>newRequester(tester.stage())
                .withTries(5)
                .withTimeout(Duration.ofMillis(TIMEOUT_MILLIS))
                .withCorrelator((RequestClass x, ReplyClass y) -> new ResultClass(x, y))
                .withRequestKeyFunction(x -> x.id)
                .withReplyKeyFunction(x -> x.id)
                .build();

        final Requester<IdentifierClass, RequestClass, ReplyClass, ResultClass> requesterY = Requester
                .<IdentifierClass, RequestClass, ReplyClass, ResultClass>newRequester(tester.stage())
                .withTries(5)
                .withTimeout(Duration.ofMillis(TIMEOUT_MILLIS))
                .withCorrelator((RequestClass x, ReplyClass y) -> new ResultClass(x, y))
                .withRequestKeyFunction(x -> x.id)
                .withReplyKeyFunction(x -> x.id)
                .withDelayedSender(DelayedSender.newDelayedSender(Executors.newScheduledThreadPool(1)))
                .build();

        assertTrue(requesterX.isUsingDefaultDelayedSender());
        assertFalse(requesterY.isUsingDefaultDelayedSender());
    }

    /**
     * Test: 20190615193718730457
     *
     * <p>
     * Method: <code>withTries</code>
     * </p>
     *
     * <p>
     * Case: Invalid Retry Limit.
     * </p>
     */
    @Test (expected = IllegalArgumentException.class)
    public void test20190615193718730457 ()
    {
        Requester.newRequester(tester.stage()).withTries(0);
    }
}
