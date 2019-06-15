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

import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class AbstractPushdownAutomatonTest
{
    private final AsyncTestTool tester = new AsyncTestTool();

    private final AtomicBoolean executedOnInitial = new AtomicBoolean();

    private final AtomicBoolean executedOnError = new AtomicBoolean();

    private final AbstractPushdownAutomaton<Integer, String> machine = new AbstractPushdownAutomaton<Integer, String>(tester.stage())
    {
        @Override
        protected void onInitial (final Integer message)
                throws Throwable
        {
            executedOnInitial.set(true);

            if (message == 13)
            {
                throw new RuntimeException();
            }
            else
            {
                sendFrom("I" + message + "I");
            }
        }

        @Override
        protected void onError (final Integer message,
                                final Throwable cause)
                throws Throwable
        {
            executedOnError.set(true);

            if (cause.getClass() == Throwable.class)
            {
                throw cause;
            }
        }
    };


    {
        tester.connect(machine.dataOut());
    }

    /**
     * Test: 20190514000607352095
     *
     * <p>
     * Method: <code>push(State)</code>
     * </p>
     */
    @Test
    public void test20190514000607352095 ()
    {
        machine.push(message -> machine.sendFrom("X" + message + "X"));
        machine.push(message -> machine.sendFrom("Y" + message + "Y"));
        machine.push(message -> machine.sendFrom("Z" + message + "Z"));

        machine.dataIn().send(100);
        machine.dataIn().send(200);
        machine.dataIn().send(300);
        machine.dataIn().send(400);

        tester.awaitEquals(machine.dataOut(), "Z100Z");
        tester.awaitEquals(machine.dataOut(), "Y200Y");
        tester.awaitEquals(machine.dataOut(), "X300X");
        tester.awaitEquals(machine.dataOut(), "I400I");

        assertTrue(executedOnInitial.get());
        assertFalse(executedOnError.get());
    }

    /**
     * Test: 20190514000607352140
     *
     * <p>
     * Method: <code>push(SideEffect)</code>
     * </p>
     */
    @Test
    public void test20190514000607352140 ()
    {
        machine.push(() -> machine.sendFrom("X"));
        machine.push(() -> machine.sendFrom("Y"));
        machine.push(() -> machine.sendFrom("Z"));

        machine.dataIn().send(100);

        tester.awaitEquals(machine.dataOut(), "Z");
        tester.awaitEquals(machine.dataOut(), "Y");
        tester.awaitEquals(machine.dataOut(), "X");
        tester.awaitEquals(machine.dataOut(), "I100I");

        assertTrue(executedOnInitial.get());
        assertFalse(executedOnError.get());
    }

    /**
     * Test: 20190514000607352158
     *
     * <p>
     * Method: <code>then(State)</code>
     * </p>
     */
    @Test
    public void test20190514000607352158 ()
    {
        machine.then(message -> machine.sendFrom("X" + message + "X"));
        machine.then(message -> machine.sendFrom("Y" + message + "Y"));
        machine.then(message -> machine.sendFrom("Z" + message + "Z"));

        machine.dataIn().send(100);
        machine.dataIn().send(200);
        machine.dataIn().send(300);
        machine.dataIn().send(400);

        tester.awaitEquals(machine.dataOut(), "I100I");
        tester.awaitEquals(machine.dataOut(), "X200X");
        tester.awaitEquals(machine.dataOut(), "Y300Y");
        tester.awaitEquals(machine.dataOut(), "Z400Z");

        assertTrue(executedOnInitial.get());
        assertFalse(executedOnError.get());
    }

    /**
     * Test: 20190514000607352175
     *
     * <p>
     * Method: <code>then(SideEffect)</code>
     * </p>
     */
    @Test
    public void test20190514000607352175 ()
    {
        machine.then(() -> machine.sendFrom("X"));
        machine.then(() -> machine.sendFrom("Y"));
        machine.then(() -> machine.sendFrom("Z"));

        machine.dataIn().send(100);

        tester.awaitEquals(machine.dataOut(), "I100I");
        tester.awaitEquals(machine.dataOut(), "X");
        tester.awaitEquals(machine.dataOut(), "Y");
        tester.awaitEquals(machine.dataOut(), "Z");

        assertTrue(executedOnInitial.get());
        assertFalse(executedOnError.get());
    }

    /**
     * Test: 20190514000726589899
     *
     * <p>
     * Case: Exception in State.
     * </p>
     */
    @Test
    public void test20190514000726589899 ()
    {
        machine.then(n -> machine.sendFrom("X" + (1 / (n - 200) + "X"))); // div by zero, if n == 200.

        machine.dataIn().send(100); // initial state
        machine.dataIn().send(200); // n = 200

        tester.awaitEquals(machine.dataOut(), "I100I");
        tester.awaitTrue(() -> executedOnError.get());

        assertTrue(executedOnInitial.get());
        assertTrue(executedOnError.get());
    }

    /**
     * Test: 20190514000726589949
     *
     * <p>
     * Case: Exception in Side Effect.
     * </p>
     */
    @Test
    public void test20190514000726589949 ()
    {
        machine.then(() -> machine.sendFrom("X" + (1 / ("X".length() - 1)))); // div by zero.

        machine.dataIn().send(100); // initial state

        tester.awaitEquals(machine.dataOut(), "I100I");
        tester.awaitTrue(() -> executedOnError.get());

        assertTrue(executedOnInitial.get());
        assertTrue(executedOnError.get());
    }
}
