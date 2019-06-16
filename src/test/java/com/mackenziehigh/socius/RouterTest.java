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

import com.mackenziehigh.socius.Router.Publisher;
import com.mackenziehigh.socius.Router.Subscriber;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class RouterTest
{
    private final AsyncTestTool tester = new AsyncTestTool();

    private final Router<String, Integer> router = Router.newRouter(tester.stage());

    private final Publisher<String, Integer> P1 = router.newPublisher("A");

    private final Publisher<String, Integer> P2 = router.newPublisher("A");

    private final Publisher<String, Integer> P3 = router.newPublisher("B");

    private final Publisher<String, Integer> P4 = router.newPublisher("B");

    private final Publisher<String, Integer> P5 = router.newPublisher("C");

    private final Subscriber<String, Integer> S1 = router.newSubscriber("A");

    private final Subscriber<String, Integer> S2 = router.newSubscriber("A");

    private final Subscriber<String, Integer> S3 = router.newSubscriber("B");

    private final Subscriber<String, Integer> S4 = router.newSubscriber("B");

    private final Subscriber<String, Integer> S5 = router.newSubscriber("A", "B");

    private final Subscriber<String, Integer> S6 = router.newSubscriber();


    {
        tester.connect(S1.dataOut());
        tester.connect(S2.dataOut());
        tester.connect(S3.dataOut());
        tester.connect(S4.dataOut());
        tester.connect(S5.dataOut());
        tester.connect(S6.dataOut());
        tester.connect(router.sinkAll());
        tester.connect(router.sinkDead());
    }

    /**
     * Test: 20190512202551636174
     *
     * <p>
     * Case: Initial Configuration.
     * </p>
     */
    @Test
    public void test20190512202551636174 ()
    {
        assertFalse(router.isSynchronous());

        assertTrue(P1.isActive());
        assertTrue(P2.isActive());
        assertTrue(P3.isActive());
        assertTrue(P4.isActive());
        assertTrue(P5.isActive());

        assertTrue(S1.isActive());
        assertTrue(S2.isActive());
        assertTrue(S3.isActive());
        assertTrue(S4.isActive());
        assertTrue(S5.isActive());
        assertTrue(S6.isActive());

        assertEquals(1, S1.routingKeys().size());
        assertEquals(1, S2.routingKeys().size());
        assertEquals(1, S3.routingKeys().size());
        assertEquals(1, S4.routingKeys().size());
        assertEquals(2, S5.routingKeys().size());
        assertEquals(0, S6.routingKeys().size());

        assertTrue(S1.routingKeys().contains("A"));
        assertTrue(S2.routingKeys().contains("A"));
        assertTrue(S3.routingKeys().contains("B"));
        assertTrue(S4.routingKeys().contains("B"));
        assertTrue(S5.routingKeys().contains("A"));
        assertTrue(S5.routingKeys().contains("B"));
    }

    /**
     * Test: 20190512203430482456
     *
     * <p>
     * Case: Basic Non-Synchronous Throughput.
     * </p>
     */
    @Test
    public void test20190512203430482456 ()
    {
        assertFalse(router.isSynchronous());

        P5.dataIn().send(500);
        tester.awaitEquals(router.sinkDead(), 500);

        P4.dataIn().send(400);
        tester.awaitEquals(S3.dataOut(), 400);
        tester.awaitEquals(S4.dataOut(), 400);
        tester.awaitEquals(S5.dataOut(), 400);

        P3.dataIn().send(300);
        tester.awaitEquals(S3.dataOut(), 300);
        tester.awaitEquals(S4.dataOut(), 300);
        tester.awaitEquals(S5.dataOut(), 300);

        P2.dataIn().send(200);
        tester.awaitEquals(S1.dataOut(), 200);
        tester.awaitEquals(S2.dataOut(), 200);
        tester.awaitEquals(S5.dataOut(), 200);

        P1.dataIn().send(100);
        tester.awaitEquals(S1.dataOut(), 100);
        tester.awaitEquals(S2.dataOut(), 100);
        tester.awaitEquals(S5.dataOut(), 100);

        tester.awaitEquals(router.sinkAll(), 500);
        tester.awaitEquals(router.sinkAll(), 400);
        tester.awaitEquals(router.sinkAll(), 300);
        tester.awaitEquals(router.sinkAll(), 200);
        tester.awaitEquals(router.sinkAll(), 100);
        tester.assertEmptyOutputs();
    }

    /**
     * Test: 20190512204330273952
     *
     * <p>
     * Case: Basic Synchronous Throughput.
     * </p>
     */
    @Test
    public void test20190512204330273952 ()
    {
        router.synchronize();
        assertTrue(router.isSynchronous());

        P5.dataIn().send(500);
        tester.awaitEquals(router.sinkDead(), 500);

        P4.dataIn().send(400);
        tester.awaitEquals(S3.dataOut(), 400);
        tester.awaitEquals(S4.dataOut(), 400);
        tester.awaitEquals(S5.dataOut(), 400);

        P3.dataIn().send(300);
        tester.awaitEquals(S3.dataOut(), 300);
        tester.awaitEquals(S4.dataOut(), 300);
        tester.awaitEquals(S5.dataOut(), 300);

        P2.dataIn().send(200);
        tester.awaitEquals(S1.dataOut(), 200);
        tester.awaitEquals(S2.dataOut(), 200);
        tester.awaitEquals(S5.dataOut(), 200);

        P1.dataIn().send(100);
        tester.awaitEquals(S1.dataOut(), 100);
        tester.awaitEquals(S2.dataOut(), 100);
        tester.awaitEquals(S5.dataOut(), 100);

        tester.awaitEquals(router.sinkAll(), 500);
        tester.awaitEquals(router.sinkAll(), 400);
        tester.awaitEquals(router.sinkAll(), 300);
        tester.awaitEquals(router.sinkAll(), 200);
        tester.awaitEquals(router.sinkAll(), 100);
        tester.assertEmptyOutputs();
    }

    /**
     * Test: 20190512204629707305
     *
     * <p>
     * Case: Deactivated Publisher.
     * </p>
     */
    @Test
    public void test20190512204629707305 ()
    {
        /**
         * Activating an active publisher is idempotent.
         */
        assertTrue(P1.isActive());
        P1.activate();
        assertTrue(P1.isActive());
        tester.awaitSteadyState();

        /**
         * Send a message while the publisher is active.
         */
        assertTrue(P1.isActive());
        P1.accept(500);
        tester.awaitSteadyState();

        /**
         * Deactivate the publisher.
         */
        P1.deactivate();
        assertFalse(P1.isActive());
        tester.awaitSteadyState();

        /**
         * Deactivating an inactive publisher is idempotent.
         */
        P1.deactivate();
        assertFalse(P1.isActive());
        tester.awaitSteadyState();

        /**
         * Send a message while the publisher is inactive.
         * The message will be silently dropped.
         */
        P1.accept(400);
        tester.awaitSteadyState();

        /**
         * Activate the publisher.
         */
        P1.activate();
        assertTrue(P1.isActive());
        tester.awaitSteadyState();

        /**
         * Send a message, now that the publisher was reactivated.
         */
        P1.accept(300);
        tester.awaitSteadyState();

        /**
         * The message, that was sent while the publisher was inactive, was dropped.
         * All of the other messages were transmitted as expected.
         */
        tester.awaitEquals(router.sinkAll(), 500);
        tester.awaitEquals(router.sinkAll(), 300);
        tester.awaitEquals(S1.dataOut(), 500);
        tester.awaitEquals(S1.dataOut(), 300);
        tester.awaitEquals(S2.dataOut(), 500);
        tester.awaitEquals(S2.dataOut(), 300);
        tester.awaitEquals(S5.dataOut(), 500);
        tester.awaitEquals(S5.dataOut(), 300);
        tester.assertEmptyOutputs();
    }

    /**
     * Test: 20190512204947002456
     *
     * <p>
     * Case: Deactivated Subscriber.
     * </p>
     */
    @Test
    public void test20190512204947002456 ()
    {
        /**
         * Activating an active subscriber is idempotent.
         */
        assertTrue(S1.isActive());
        S1.activate();
        assertTrue(S1.isActive());
        tester.awaitSteadyState();

        /**
         * Send a message while the subscriber is active.
         */
        assertTrue(S1.isActive());
        P1.accept(500);
        tester.awaitSteadyState();

        /**
         * Deactivate the subscriber.
         */
        S1.deactivate();
        assertFalse(S1.isActive());
        tester.awaitSteadyState();

        /**
         * Deactivating an inactive subscriber is idempotent.
         */
        S1.deactivate();
        assertFalse(S1.isActive());
        tester.awaitSteadyState();

        /**
         * Send a message while the subscriber is inactive.
         * The message will be silently dropped by the subscriber.
         */
        P1.accept(400);
        tester.awaitSteadyState();

        /**
         * Activate the subscriber.
         */
        S1.activate();
        assertTrue(S1.isActive());
        tester.awaitSteadyState();

        /**
         * Send a message, now that the subscriber was reactivated.
         */
        P1.accept(300);
        tester.awaitSteadyState();

        /**
         * The message, that was sent while the subscriber was inactive, was dropped.
         * All of the other messages were received as expected.
         */
        tester.awaitEquals(router.sinkAll(), 500);
        tester.awaitEquals(router.sinkAll(), 400);
        tester.awaitEquals(router.sinkAll(), 300);
        tester.awaitEquals(S1.dataOut(), 500);
        tester.awaitEquals(S1.dataOut(), 300);
        tester.awaitEquals(S2.dataOut(), 500);
        tester.awaitEquals(S2.dataOut(), 400);
        tester.awaitEquals(S2.dataOut(), 300);
        tester.awaitEquals(S5.dataOut(), 500);
        tester.awaitEquals(S5.dataOut(), 400);
        tester.awaitEquals(S5.dataOut(), 300);
        tester.assertEmptyOutputs();
    }

    /**
     * Test: 20190615181952774612
     *
     * <p>
     * Method: <code>newPublisher(Function)</code>
     * </p>
     *
     * <p>
     * Case: Key-Function based Publisher.
     * </p>
     */
    @Test
    public void test20190615181952774612 ()
    {
        final Publisher<String, Integer> pub = router.newPublisher(x -> x < 0 ? "A" : "B");

        pub.accept(-100);
        pub.accept(-200);
        pub.accept(+300);
        pub.accept(+400);

        tester.awaitEquals(S1.dataOut(), -100);
        tester.awaitEquals(S1.dataOut(), -200);
        tester.awaitEquals(S2.dataOut(), -100);
        tester.awaitEquals(S2.dataOut(), -200);
        tester.awaitEquals(S3.dataOut(), +300);
        tester.awaitEquals(S3.dataOut(), +400);
        tester.awaitEquals(S4.dataOut(), +300);
        tester.awaitEquals(S4.dataOut(), +400);
        tester.awaitEquals(S5.dataOut(), -100);
        tester.awaitEquals(S5.dataOut(), -200);
        tester.awaitEquals(S5.dataOut(), +300);
        tester.awaitEquals(S5.dataOut(), +400);
        tester.awaitEquals(router.sinkAll(), -100);
        tester.awaitEquals(router.sinkAll(), -200);
        tester.awaitEquals(router.sinkAll(), +300);
        tester.awaitEquals(router.sinkAll(), +400);
        tester.assertEmptyOutputs();
    }

}
