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

import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.ContextScript;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class ProcessorTest
{
    private final AsyncTestTool tester = new AsyncTestTool();

    /**
     * Test: 20190615155625059448
     *
     * <p>
     * Method: <code>fromActor</code>
     * </p>
     */
    @Test
    public void test20190615155625059448 ()
    {
        final Actor<Integer, Integer> actor = tester
                .stage()
                .newActor()
                .withFunctionScript((Integer x) -> x * x)
                .create();

        final Processor<Integer> processor = Processor.fromActor(actor);

        tester.connect(processor.dataOut());

        processor.accept(2);
        processor.accept(3);
        processor.accept(4);
        processor.accept(5);
        processor.accept(6);

        tester.awaitEquals(processor.dataOut(), 4);
        tester.awaitEquals(processor.dataOut(), 9);
        tester.awaitEquals(processor.dataOut(), 16);
        tester.awaitEquals(processor.dataOut(), 25);
        tester.awaitEquals(processor.dataOut(), 36);
    }

    /**
     * Test: 20190615155625059499
     *
     * <p>
     * Method: <code>fromContextScript</code>
     * </p>
     */
    @Test
    public void test20190615155625059499 ()
    {
        final ContextScript<Integer, Integer> script = (ctx, msg) ->
        {
            ctx.sendFrom(msg * msg); // square
            ctx.sendFrom(msg * msg * msg); // cube
        };

        final Processor<Integer> actor = Processor.fromContextScript(tester.stage(), script);

        tester.connect(actor.dataOut());

        actor.accept(2);
        actor.accept(3);
        actor.accept(4);
        actor.accept(5);
        actor.accept(6);

        tester.awaitEquals(actor.dataOut(), 4); // square = 2 ** 2
        tester.awaitEquals(actor.dataOut(), 8); // cube = 2 ** 3
        tester.awaitEquals(actor.dataOut(), 9); // square = 3 ** 2
        tester.awaitEquals(actor.dataOut(), 27); // cube = 3 ** 3
        tester.awaitEquals(actor.dataOut(), 16); // square = 4 ** 2
        tester.awaitEquals(actor.dataOut(), 64); // cube = 4 ** 3
        tester.awaitEquals(actor.dataOut(), 25); // square = 5 ** 2
        tester.awaitEquals(actor.dataOut(), 125); // cube = 5 ** 3
        tester.awaitEquals(actor.dataOut(), 36); // square = 6 ** 2
        tester.awaitEquals(actor.dataOut(), 216); // cube = 6 ** 3
    }

    /**
     * Test: 20190615155625059523
     *
     * <p>
     * Method: <code>fromFunctionScript</code>
     * </p>
     */
    @Test
    public void test20190615155625059523 ()
    {
        final Processor<Integer> actor = Processor.fromFunctionScript(tester.stage(), (Integer x) -> x * x);

        tester.connect(actor.dataOut());

        actor.accept(2);
        actor.accept(3);
        actor.accept(4);
        actor.accept(5);
        actor.accept(6);

        tester.awaitEquals(actor.dataOut(), 4);
        tester.awaitEquals(actor.dataOut(), 9);
        tester.awaitEquals(actor.dataOut(), 16);
        tester.awaitEquals(actor.dataOut(), 25);
        tester.awaitEquals(actor.dataOut(), 36);
    }

    /**
     * Test: 20190615155625059549
     *
     * <p>
     * Method: <code>fromConsumerScript</code>
     * </p>
     */
    @Test
    public void test20190615155625059549 ()
    {
        final Queue<Integer> squares = new LinkedBlockingQueue<>();

        final Processor<Integer> actor = Processor.fromConsumerScript(tester.stage(), (Integer x) -> squares.add(x * x));

        tester.connect(actor.dataOut());

        actor.accept(2);
        actor.accept(3);
        actor.accept(4);
        actor.accept(5);
        actor.accept(6);

        tester.awaitSteadyState();

        assertEquals(4, (int) squares.poll());
        assertEquals(9, (int) squares.poll());
        assertEquals(16, (int) squares.poll());
        assertEquals(25, (int) squares.poll());
        assertEquals(36, (int) squares.poll());
        assertTrue(squares.isEmpty());
    }

    /**
     * Test: 20190615155625059574
     *
     * <p>
     * Method: <code>fromFilter</code>
     * </p>
     */
    @Test
    public void test20190615155625059574 ()
    {
        final Processor<String> actor = Processor.fromFilter(tester.stage(), x -> !x.contains("e"));

        tester.connect(actor.dataOut());

        actor.accept("avril");
        actor.accept("emma");
        actor.accept("erin");
        actor.accept("t'pol");
        actor.accept("elle");
        actor.accept("olivia");

        tester.awaitEquals(actor.dataOut(), "avril");
        tester.awaitEquals(actor.dataOut(), "t'pol");
        tester.awaitEquals(actor.dataOut(), "olivia");
    }

    /**
     * Test: 20190615155625059595
     *
     * <p>
     * Method: <code>fromIO</code>
     * </p>
     */
    @Test
    public void test20190615155625059595 ()
    {
        final Processor<Integer> delegate = Processor.fromFunctionScript(tester.stage(), (Integer x) -> x * x);

        final Processor<Integer> actor = Processor.fromIO(delegate.dataIn(), delegate.dataOut());

        tester.connect(actor.dataOut());

        actor.accept(2);
        actor.accept(3);
        actor.accept(4);
        actor.accept(5);
        actor.accept(6);

        tester.awaitEquals(actor.dataOut(), 4);
        tester.awaitEquals(actor.dataOut(), 9);
        tester.awaitEquals(actor.dataOut(), 16);
        tester.awaitEquals(actor.dataOut(), 25);
        tester.awaitEquals(actor.dataOut(), 36);
    }

    /**
     * Test: 20190615155625059612
     *
     * <p>
     * Method: <code>fromIdentityScript</code>
     * </p>
     */
    @Test
    public void test20190615155625059612 ()
    {
        final Processor<Integer> actor = Processor.fromIdentityScript(tester.stage());

        tester.connect(actor.dataOut());

        actor.accept(2);
        actor.accept(3);
        actor.accept(4);
        actor.accept(5);
        actor.accept(6);

        tester.awaitEquals(actor.dataOut(), 2);
        tester.awaitEquals(actor.dataOut(), 3);
        tester.awaitEquals(actor.dataOut(), 4);
        tester.awaitEquals(actor.dataOut(), 5);
        tester.awaitEquals(actor.dataOut(), 6);
    }
}
