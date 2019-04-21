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

import com.mackenziehigh.socius.util.ActorTester;
import com.mackenziehigh.socius.time.DelayedSender;
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
    private final class TypeA
    {
        // Pass.
    }

    /**
     * Fake Data Type.
     */
    private final class TypeB
    {
        public final TypeA id;

        public final String value;

        public TypeB (final TypeA id,
                      final String value)
        {
            this.id = id;
            this.value = value;
        }
    }

    /**
     * Fake Data Type.
     */
    private final class TypeC
    {
        public final TypeA id;

        public final String value;

        public TypeC (final TypeA id,
                      final String value)
        {
            this.id = id;
            this.value = value;
        }
    }

    /**
     * Fake Data Type.
     */
    private final class TypeD
    {
        public final TypeB request;

        public final TypeC reply;

        public TypeD (final TypeB request,
                      final TypeC reply)
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
            final TypeD other = (TypeD) obj;
            return Objects.equals(request, other.request) && Objects.equals(reply, other.reply);
        }
    }

    private static final long TIMEOUT_MILLIS = 200;

    private final ActorTester tester = new ActorTester();

    private final Requester<TypeA, TypeB, TypeC, TypeD> requester = Requester
            .<TypeA, TypeB, TypeC, TypeD>newRequester(tester.stage())
            .withTries(1 + 5) // Initial + Up to (5) Retries
            .withTimeout(Duration.ofMillis(TIMEOUT_MILLIS))
            .withComposer((TypeB x, TypeC y) -> new TypeD(x, y))
            .withRequestKeyFunction(x -> x.id)
            .withReplyKeyFunction(x -> x.id)
            .build();

    /**
     * Test: 20190102033656219973
     *
     * <p>
     * Case: Throughput of Normal Request with Normal Reply.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190102033656219973 ()
            throws Throwable
    {
        final TypeA id = new TypeA();
        final TypeB request = new TypeB(id, "Neptune");
        final TypeC reply = new TypeC(id, "Jovian");
        final TypeD result = new TypeD(request, reply);

        tester.require(() -> requester.pendingRequestCount() == 0);
        tester.send(requester.requestIn(), request);
        tester.require(() -> requester.pendingRequestCount() == 1);
        tester.expect(requester.requestOut(), request);
        tester.send(requester.replyIn(), reply);
        tester.expect(requester.resultOut(), result);
        tester.requireEmptyOutputs();
        tester.require(() -> requester.pendingRequestCount() == 0);
        tester.run();
    }

    /**
     * Test: 20190102033656220076
     *
     * <p>
     * Case: Request Timed Out, Retry Successful.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190102033656220076 ()
            throws Throwable
    {
        final TypeA id = new TypeA();
        final TypeB request = new TypeB(id, "Neptune");
        final TypeC reply = new TypeC(id, "Jovian");
        final TypeD result = new TypeD(request, reply);

        tester.require(() -> requester.pendingRequestCount() == 0);
        tester.send(requester.requestIn(), request);
        tester.require(() -> requester.pendingRequestCount() == 1);
        tester.expect(requester.requestOut(), request);
        tester.execute(() -> Thread.sleep((long) (TIMEOUT_MILLIS * 1.5)));
        tester.expect(requester.requestOut(), request); // After the request timed out, it was sent again (retried).
        tester.send(requester.replyIn(), reply);
        tester.expect(requester.resultOut(), result);
        tester.requireEmptyOutputs();
        tester.require(() -> requester.pendingRequestCount() == 0);
        tester.run();
    }

    /**
     * Test: 20190102043344891456
     *
     * <p>
     * Case: Duplicate Request, Normal Reply.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190102043344891456 ()
            throws Throwable
    {
        final TypeA id = new TypeA();
        final TypeB request = new TypeB(id, "Neptune");
        final TypeC reply = new TypeC(id, "Jovian");
        final TypeD result = new TypeD(request, reply);

        tester.require(() -> requester.pendingRequestCount() == 0);
        tester.send(requester.requestIn(), request);
        tester.send(requester.requestIn(), request); // Duplicate Request.
        tester.expect(requester.droppedRequestOut(), request); // The duplicate request was dropped.
        tester.require(() -> requester.pendingRequestCount() == 1);
        tester.expect(requester.requestOut(), request);
        tester.send(requester.replyIn(), reply);
        tester.expect(requester.resultOut(), result);
        tester.requireEmptyOutputs();
        tester.require(() -> requester.pendingRequestCount() == 0);
        tester.run();
    }

    /**
     * Test: 20190102033656220182
     * <p>
     * Case: Normal Request, Duplicate Reply.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190102033656220182 ()
            throws Throwable
    {
        final TypeA id = new TypeA();
        final TypeB request = new TypeB(id, "Neptune");
        final TypeC reply = new TypeC(id, "Jovian");
        final TypeD result = new TypeD(request, reply);

        tester.require(() -> requester.pendingRequestCount() == 0);
        tester.send(requester.requestIn(), request);
        tester.require(() -> requester.pendingRequestCount() == 1);
        tester.expect(requester.requestOut(), request);
        tester.send(requester.replyIn(), reply);
        tester.send(requester.replyIn(), reply); // Duplicate Reply.
        tester.expect(requester.droppedReplyOut(), reply); // The duplicate was dropped.
        tester.expect(requester.resultOut(), result);
        tester.requireEmptyOutputs();
        tester.require(() -> requester.pendingRequestCount() == 0);
        tester.run();
    }

    /**
     * Test: 20190102050927530236
     *
     * <p>
     * Case: Multiple Retries.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190102050927530236 ()
            throws Throwable
    {
        final TypeA id = new TypeA();
        final TypeB request = new TypeB(id, "Neptune");
        final TypeC reply = new TypeC(id, "Jovian");

        tester.require(() -> requester.pendingRequestCount() == 0);
        tester.send(requester.requestIn(), request);
        tester.require(() -> requester.pendingRequestCount() == 1);
        tester.expect(requester.requestOut(), request);
        tester.execute(() -> Thread.sleep((long) (TIMEOUT_MILLIS * 1.5)));
        tester.expect(requester.requestOut(), request); // Retry #1.
        tester.execute(() -> Thread.sleep((long) (TIMEOUT_MILLIS * 1.5)));
        tester.expect(requester.requestOut(), request); // Retry #2.
        tester.execute(() -> Thread.sleep((long) (TIMEOUT_MILLIS * 1.5)));
        tester.expect(requester.requestOut(), request); // Retry #3.
        tester.execute(() -> Thread.sleep((long) (TIMEOUT_MILLIS * 1.5)));
        tester.expect(requester.requestOut(), request); // Retry #4.
        tester.execute(() -> Thread.sleep((long) (TIMEOUT_MILLIS * 1.5)));
        tester.expect(requester.requestOut(), request); // Retry #5.
        tester.execute(() -> Thread.sleep((long) (TIMEOUT_MILLIS * 1.5))); // Retry Limit Exceeded.
        tester.send(requester.replyIn(), reply);
        tester.expect(requester.droppedReplyOut(), reply);
        tester.requireEmptyOutputs();
        tester.require(() -> requester.pendingRequestCount() == 0);
        tester.run();
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
        final Requester<TypeA, TypeB, TypeC, TypeD> requesterX = Requester
                .<TypeA, TypeB, TypeC, TypeD>newRequester(tester.stage())
                .withTries(5)
                .withTimeout(Duration.ofMillis(TIMEOUT_MILLIS))
                .withComposer((TypeB x, TypeC y) -> new TypeD(x, y))
                .withRequestKeyFunction(x -> x.id)
                .withReplyKeyFunction(x -> x.id)
                .build();

        final Requester<TypeA, TypeB, TypeC, TypeD> requesterY = Requester
                .<TypeA, TypeB, TypeC, TypeD>newRequester(tester.stage())
                .withTries(5)
                .withTimeout(Duration.ofMillis(TIMEOUT_MILLIS))
                .withComposer((TypeB x, TypeC y) -> new TypeD(x, y))
                .withRequestKeyFunction(x -> x.id)
                .withReplyKeyFunction(x -> x.id)
                .withDelayedSender(DelayedSender.newDelayedSender(Executors.newScheduledThreadPool(1)))
                .build();

        assertTrue(requesterX.isUsingDefaultDelayedSender());
        assertFalse(requesterY.isUsingDefaultDelayedSender());
    }
}
