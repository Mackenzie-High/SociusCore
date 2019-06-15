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

import com.mackenziehigh.socius.AsyncTestTool;
import com.mackenziehigh.socius.Variable;
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
        final Variable<Character> var = Variable.newVariable(tester.stage(), 'A');
        final Instant now1 = Instant.now();
        final Instant now2 = Instant.now();
        final Instant now3 = Instant.now();
        final Instant now4 = Instant.now();

        tester.connect(var.clockOut());
        tester.connect(var.dataOut());

        /**
         * Verify the initial state.
         */
        assertEquals('A', (char) var.get());

        /**
         * Sending a clock-pulse retrieves the value in the variable.
         */
        var.clockIn().send(now1);
        tester.awaitEquals(var.dataOut(), 'A');
        tester.awaitEquals(var.clockOut(), now1);

        /**
         * The value may be retrieved multiple times.
         */
        var.clockIn().send(now2);
        tester.awaitEquals(var.dataOut(), 'A');
        tester.awaitEquals(var.clockOut(), now2);

        /**
         * Change the value stored in the variable via a message.
         */
        var.dataIn().send('B');
        tester.awaitEquals(var.dataOut(), 'B');
        assertEquals('B', (char) var.get());

        /**
         * The new value may be retrieved by sending a clock-pulse.
         */
        var.clockIn().send(now3);
        tester.awaitEquals(var.dataOut(), 'B');
        tester.awaitEquals(var.clockOut(), now3);

        /**
         * Change the value via the set() method.
         */
        var.set('C');
        tester.awaitEquals(var.dataOut(), 'C');
        assertEquals('C', (char) var.get());

        /**
         * The new value may be retrieved by sending a clock-pulse.
         */
        var.clockIn().send(now4);
        tester.awaitEquals(var.dataOut(), 'C');
        tester.awaitEquals(var.clockOut(), now4);
    }
}
