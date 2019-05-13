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

    private final Router<Integer> router = Router.newRouter(tester.stage());

    private final Processor<Integer> actor1 = Processor.fromIdentityScript(tester.stage());

    private final Processor<Integer> actor2 = Processor.fromIdentityScript(tester.stage());

    private final Processor<Integer> actor3 = Processor.fromIdentityScript(tester.stage());

    private final Processor<Integer> actor4 = Processor.fromIdentityScript(tester.stage());

    private final Processor<Integer> actor5 = Processor.fromIdentityScript(tester.stage());

    private final Processor<Integer> actor6 = Processor.fromIdentityScript(tester.stage());

    private final Publisher<Integer> P1 = router.newPublisher("A");

    private final Publisher<Integer> P2 = router.newPublisher("A");

    private final Publisher<Integer> P3 = router.newPublisher("B");

    private final Publisher<Integer> P4 = router.newPublisher("B");

    private final Publisher<Integer> P5 = router.newPublisher("C");

    private final Subscriber<Integer> S1 = router.newSubscriber("A");

    private final Subscriber<Integer> S2 = router.newSubscriber("A");

    private final Subscriber<Integer> S3 = router.newSubscriber("B");

    private final Subscriber<Integer> S4 = router.newSubscriber("B");


    {
        tester.connect(S1.dataOut());
        tester.connect(S2.dataOut());
        tester.connect(S3.dataOut());
        tester.connect(S4.dataOut());
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

        assertEquals("A", P1.routingKey());
        assertEquals("A", P2.routingKey());
        assertEquals("A", S1.routingKey());
        assertEquals("A", S2.routingKey());

        assertEquals("B", P3.routingKey());
        assertEquals("B", P4.routingKey());
        assertEquals("B", S3.routingKey());
        assertEquals("B", S4.routingKey());
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
        tester.expect(router.sinkDead(), 500);

        P4.dataIn().send(400);
        tester.expect(S3.dataOut(), 400);
        tester.expect(S4.dataOut(), 400);

        P3.dataIn().send(300);
        tester.expect(S3.dataOut(), 300);
        tester.expect(S4.dataOut(), 300);

        P2.dataIn().send(200);
        tester.expect(S1.dataOut(), 200);
        tester.expect(S2.dataOut(), 200);

        P1.dataIn().send(100);
        tester.expect(S1.dataOut(), 100);
        tester.expect(S2.dataOut(), 100);

        tester.expect(router.sinkAll(), 500);
        tester.expect(router.sinkAll(), 400);
        tester.expect(router.sinkAll(), 300);
        tester.expect(router.sinkAll(), 200);
        tester.expect(router.sinkAll(), 100);
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
        tester.expect(router.sinkDead(), 500);

        P4.dataIn().send(400);
        tester.expect(S3.dataOut(), 400);
        tester.expect(S4.dataOut(), 400);

        P3.dataIn().send(300);
        tester.expect(S3.dataOut(), 300);
        tester.expect(S4.dataOut(), 300);

        P2.dataIn().send(200);
        tester.expect(S1.dataOut(), 200);
        tester.expect(S2.dataOut(), 200);

        P1.dataIn().send(100);
        tester.expect(S1.dataOut(), 100);
        tester.expect(S2.dataOut(), 100);

        tester.expect(router.sinkAll(), 500);
        tester.expect(router.sinkAll(), 400);
        tester.expect(router.sinkAll(), 300);
        tester.expect(router.sinkAll(), 200);
        tester.expect(router.sinkAll(), 100);
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
        P1.deactivate();
        assertFalse(P1.isActive());
        assertTrue(P2.isActive());
        assertTrue(P5.isActive());

        P5.dataIn().send(500);
        tester.expect(router.sinkDead(), 500);

        P1.dataIn().send(100); // silently dropped.

        P2.dataIn().send(200);
        tester.expect(S1.dataOut(), 200);
        tester.expect(S2.dataOut(), 200);

        tester.expect(router.sinkAll(), 500);
        tester.expect(router.sinkAll(), 200);
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
        fail();
    }
}
