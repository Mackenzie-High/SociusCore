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

import com.google.common.collect.Lists;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.ActorTester.StepException;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class ActorTesterTest
{
    private final ActorTester tester = new ActorTester();

    /**
     * Test: 20190102182745669544
     *
     * <p>
     * Method: <code>execute</code>
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190102182745669544 ()
            throws Throwable
    {
        final List<String> list = new ArrayList<>();

        tester.execute(() -> assertEquals(0, list.size()));
        tester.execute(() -> list.add("X"));
        tester.execute(() -> assertEquals(1, list.size()));
        tester.execute(() -> list.add("Y"));
        tester.execute(() -> assertEquals(2, list.size()));
        tester.execute(() -> list.add("Z"));
        tester.execute(() -> assertEquals(3, list.size()));

        assertEquals(0, list.size());
        tester.run();
        assertEquals(3, list.size());
    }

    /**
     * Test: 20190102182745669578
     *
     * <p>
     * Method: <code>send</code>
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190102182745669578 ()
            throws Throwable
    {
        final List<String> list = Lists.newCopyOnWriteArrayList();
        final CollectionSink<String> sink = CollectionSink.newCollectionSink(tester.stage(), list);

        tester.execute(() -> assertEquals(0, list.size()));
        tester.send(sink.dataIn(), "X");
        tester.execute(() -> assertEquals(1, list.size()));
        tester.send(sink.dataIn(), "Y");
        tester.execute(() -> assertEquals(2, list.size()));
        tester.send(sink.dataIn(), "Z");
        tester.execute(() -> assertEquals(3, list.size()));

        assertEquals(0, list.size());
        tester.run();
        assertEquals(3, list.size());
    }

    /**
     * Test: 20190102182745669634
     *
     * <p>
     * Method: <code>require</code>
     * </p>
     *
     * <p>
     * Case: Requirement Obeyed.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190102182745669634 ()
            throws Throwable
    {
        tester.require(() -> true);
        tester.run();
    }

    /**
     * Test: 20190102184936726325
     *
     * <p>
     * Method: <code>require</code>
     * </p>
     *
     * <p>
     * Case: Requirement Violated.
     * </p>
     */
    @Test
    public void test20190102184936726325 ()
    {
        try
        {
            tester.require(() -> false);
            tester.run();
            fail();
        }
        catch (Throwable ex)
        {
            assertTrue(ex instanceof StepException);
            assertTrue(ex.getMessage().startsWith("(class = com.mackenziehigh.socius.ActorTesterTest, method = test20190102184936726325"));
        }
    }

    /**
     * Test: 20190102182745669682
     *
     * <p>
     * Method: <code>expectLike</code>
     * </p>
     *
     * <p>
     * Case: Predicate Based.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190102182745669682 ()
            throws Throwable
    {
        final Processor<String> actor = Processor.fromIdentityScript(tester.stage());

        tester.send(actor.dataIn(), "AXE");
        tester.expectLike(actor.dataOut(), x -> x.contains("X"), "No X");
        tester.run();
    }

    /**
     * Test: 20190102190926244317
     *
     * <p>
     * Method: <code>expectLike</code>
     * </p>
     *
     * <p>
     * Case: Requirement Violated.
     * </p>
     */
    @Test
    public void test20190102190926244317 ()
    {
        final Processor<String> actor = Processor.fromIdentityScript(tester.stage());

        try
        {
            tester.send(actor.dataIn(), "AYE");
            tester.expectLike(actor.dataOut(), x -> x.contains("X"), "No X");
            tester.run();
            fail();
        }
        catch (Throwable ex)
        {
            assertTrue(ex instanceof StepException);
        }
    }

    /**
     * Test: 20190102183009191768
     *
     * <p>
     * Method: <code>expect</code>
     * </p>
     *
     * <p>
     * Case: Requirement Obeyed.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190102183009191768 ()
            throws Throwable
    {
        final Processor<String> actor = Processor.fromIdentityScript(tester.stage());

        tester.send(actor.dataIn(), "X");
        tester.expect(actor.dataOut(), "X");
        tester.run();
    }

    /**
     * Test: 20190102193050481608
     *
     * <p>
     * Method: <code>expect</code>
     * </p>
     *
     * <p>
     * Case: Requirement Violated.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190102193050481608 ()
            throws Throwable
    {
        final Processor<String> actor = Processor.fromIdentityScript(tester.stage());

        try
        {
            tester.send(actor.dataIn(), "Y");
            tester.expect(actor.dataOut(), "X");
            tester.run();
            fail();
        }
        catch (Throwable ex)
        {
            assertTrue(ex instanceof StepException);
        }
    }

    /**
     * Test: 20190102193344267336
     *
     * <p>
     * Case: Attempt to re-run test.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test (expected = IllegalStateException.class)
    public void test20190102193344267336 ()
            throws Throwable
    {
        tester.run();
        tester.run();
    }

    /**
     * Test: 20190102193541097937
     *
     * <p>
     * Case: Using an actor that is on a different stage.
     * </p>
     */
    @Test (expected = IllegalArgumentException.class)
    public void test20190102193541097937 ()
    {
        final Stage stage = Cascade.newStage();
        final Processor<String> proc = Processor.fromIdentityScript(stage);
        tester.connect(proc.dataOut());
    }
}
