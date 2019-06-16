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

import java.time.Instant;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class VariableTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final var tester = new AsyncTestTool();

        final Variable<Character> variable = Variable.newVariable(tester.stage(), 'A');

        /**
         * None of these are equal to each other,
         * so that they can be distinguished.
         */
        final Instant now1 = Instant.now().plusMillis(1);
        final Instant now2 = Instant.now().plusMillis(2);
        final Instant now3 = Instant.now().plusMillis(3);
        final Instant now4 = Instant.now().plusMillis(4);

        tester.connect(variable.clockOut());
        tester.connect(variable.dataOut());

        /**
         * Verify the initial state.
         */
        assertEquals('A', (char) variable.get());

        /**
         * Sending a clock-pulse retrieves the value in the variable.
         */
        variable.clockIn().send(now1);
        tester.awaitEquals(variable.dataOut(), 'A');
        tester.awaitEquals(variable.clockOut(), now1);

        /**
         * The value may be retrieved multiple times.
         */
        variable.clockIn().send(now2);
        tester.awaitEquals(variable.dataOut(), 'A');
        tester.awaitEquals(variable.clockOut(), now2);

        /**
         * Change the value stored in the variable via a message.
         */
        variable.dataIn().send('B');
        tester.awaitEquals(variable.dataOut(), 'B');
        assertEquals('B', (char) variable.get());

        /**
         * The new value may be retrieved by sending a clock-pulse.
         */
        variable.clockIn().send(now3);
        tester.awaitEquals(variable.dataOut(), 'B');
        tester.awaitEquals(variable.clockOut(), now3);

        /**
         * Change the value via the set() method.
         */
        variable.set('C');
        tester.awaitEquals(variable.dataOut(), 'C');
        assertEquals('C', (char) variable.get());

        /**
         * The new value may be retrieved by sending a clock-pulse.
         */
        variable.clockIn().send(now4);
        tester.awaitEquals(variable.dataOut(), 'C');
        tester.awaitEquals(variable.clockOut(), now4);
    }
}
