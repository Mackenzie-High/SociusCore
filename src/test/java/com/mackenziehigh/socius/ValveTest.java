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
import com.mackenziehigh.socius.util.ActorTester;
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
        final ActorTester tester = new ActorTester();
        final Valve<Character> valve = Valve.newOpenValve(tester.stage());

        /**
         * The valve is initially open.
         */
        tester.require(() -> valve.isOpen());
        tester.require(() -> !valve.isClosed());
        tester.send(valve.dataIn(), 'A');
        tester.expect(valve.dataOut(), 'A');
        tester.requireEmptyOutputs();

        /**
         * Close the valve via a message.
         */
        tester.send(valve.toggleIn(), false);
        tester.expect(valve.toggleOut(), false);
        tester.requireEmptyOutputs();

        /**
         * No data gets through a closed valve.
         */
        tester.require(() -> !valve.isOpen());
        tester.require(() -> valve.isClosed());
        tester.send(valve.dataIn(), 'B');
        tester.requireEmptyOutputs();

        /**
         * Open the valve via a message.
         */
        tester.send(valve.toggleIn(), true);
        tester.expect(valve.toggleOut(), true);
        tester.requireEmptyOutputs();

        /**
         * Data can flow through a re-opened valve.
         */
        tester.require(() -> valve.isOpen());
        tester.require(() -> !valve.isClosed());
        tester.send(valve.dataIn(), 'C');
        tester.expect(valve.dataOut(), 'C');
        tester.requireEmptyOutputs();

        /**
         * Close the valve via the toggle() method.
         * Verify that no data gets through subsequently.
         * Notice that a toggle message is sent out however.
         */
        tester.execute(() -> valve.toggle(false));
        tester.expect(valve.toggleOut(), false);
        tester.requireEmptyOutputs();
        tester.send(valve.dataIn(), 'D');
        tester.requireEmptyOutputs();

        /**
         * Open the valve via the toggle() method.
         * Verify that data can get through subsequently.
         * Notice that a toggle message is sent out.
         */
        tester.execute(() -> valve.toggle(true));
        tester.expect(valve.toggleOut(), true);
        tester.requireEmptyOutputs();
        tester.send(valve.dataIn(), 'E');
        tester.expect(valve.dataOut(), 'E');
        tester.requireEmptyOutputs();

        tester.run();
    }

    @Test
    public void test2 ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final Valve<Character> valve = Valve.newClosedValve(tester.stage());

        /**
         * The valve is initially closed.
         */
        tester.require(() -> !valve.isOpen());
        tester.require(() -> valve.isClosed());
        tester.send(valve.dataIn(), 'A');
        tester.requireEmptyOutputs();

        /**
         * Open the valve via a message.
         */
        tester.send(valve.toggleIn(), true);
        tester.expect(valve.toggleOut(), true);
        tester.requireEmptyOutputs();

        /**
         * Data can flow through a re-opened valve.
         */
        tester.require(() -> valve.isOpen());
        tester.require(() -> !valve.isClosed());
        tester.send(valve.dataIn(), 'C');
        tester.expect(valve.dataOut(), 'C');
        tester.requireEmptyOutputs();

        tester.run();
    }
}
