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

import com.mackenziehigh.socius.Valve;
import com.mackenziehigh.socius.AsyncTestTool;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class ValveTest
{
    @Test
    public void test1 ()
            throws Throwable
    {
        final var tester = new AsyncTestTool();
        final Valve<Character> valve = Valve.newOpenValve(tester.stage());

        tester.connect(valve.dataOut());
        tester.connect(valve.toggleOut());

        /**
         * The valve is initially open.
         */
        assertTrue(valve.isOpen());
        assertFalse(valve.isClosed());
        valve.dataIn().send('A');
        tester.awaitEquals(valve.dataOut(), 'A');

        /**
         * Close the valve via a message.
         */
        valve.toggleIn().send(false);
        tester.awaitEquals(valve.toggleOut(), false);

        /**
         * No data gets through a closed valve.
         */
        assertFalse(valve.isOpen());
        assertTrue(valve.isClosed());
        valve.dataIn().send('A');

        /**
         * Open the valve via a message.
         */
        valve.toggleIn().send(true);
        tester.awaitEquals(valve.toggleOut(), true);

        /**
         * Data can flow through a re-opened valve.
         */
        assertTrue(valve.isOpen());
        assertFalse(valve.isClosed());
        valve.dataIn().send('C');
        tester.awaitEquals(valve.dataOut(), 'C');

        /**
         * Close the valve via the toggle() method.
         * Verify that no data gets through subsequently.
         * Notice that a toggle message is sent out however.
         */
        valve.toggle(false);
        tester.awaitEquals(valve.toggleOut(), false);
        valve.dataIn().send('D');

        /**
         * Open the valve via the toggle() method.
         * Verify that data can get through subsequently.
         * Notice that a toggle message is sent out.
         */
        valve.toggle(true);
        tester.awaitEquals(valve.toggleOut(), true);
        valve.dataIn().send('E');
        tester.awaitEquals(valve.dataOut(), 'E');
    }

    @Test
    public void test2 ()
            throws Throwable
    {
        final var tester = new AsyncTestTool();
        final Valve<Character> valve = Valve.newClosedValve(tester.stage());

        tester.connect(valve.dataOut());
        tester.connect(valve.toggleOut());

        /**
         * The valve is initially closed.
         */
        assertFalse(valve.isOpen());
        assertTrue(valve.isClosed());
        valve.dataIn().send('A');

        /**
         * Open the valve via a message.
         */
        valve.toggleIn().send(true);
        tester.awaitEquals(valve.toggleOut(), true);

        /**
         * Data can flow through a re-opened valve.
         */
        assertTrue(valve.isOpen());
        assertFalse(valve.isClosed());
        valve.dataIn().send('C');
        tester.awaitEquals(valve.dataOut(), 'C');
    }
}
