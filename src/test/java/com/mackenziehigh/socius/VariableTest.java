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
        final ActorTester tester = new ActorTester();
        final Variable<Character> var = Variable.newVariable(tester.stage(), 'A');
        final Instant now1 = Instant.now();
        final Instant now2 = Instant.now();
        final Instant now3 = Instant.now();
        final Instant now4 = Instant.now();

        /**
         * Verify the initial state.
         */
        tester.requireEmptyOutputs();
        tester.require(() -> 'A' == var.get());

        /**
         * Sending a clock-pulse retrieves the value in the variable.
         */
        tester.send(var.clockIn(), now1);
        tester.expect(var.dataOut(), 'A');
        tester.expect(var.clockOut(), now1);
        tester.requireEmptyOutputs();

        /**
         * The value may be retrieved multiple times.
         */
        tester.send(var.clockIn(), now2);
        tester.expect(var.dataOut(), 'A');
        tester.expect(var.clockOut(), now2);
        tester.requireEmptyOutputs();

        /**
         * Change the value stored in the variable via a message.
         */
        tester.send(var.dataIn(), 'B');
        tester.require(() -> 'B' == var.get());
        tester.expect(var.dataOut(), 'B');
        tester.requireEmptyOutputs();

        /**
         * The new value may be retrieved by sending a clock-pulse.
         */
        tester.send(var.clockIn(), now3);
        tester.expect(var.dataOut(), 'B');
        tester.expect(var.clockOut(), now3);
        tester.requireEmptyOutputs();

        /**
         * Change the value via the set() method.
         */
        tester.execute(() -> var.set('C'));
        tester.require(() -> 'C' == var.get());
        tester.expect(var.dataOut(), 'C');
        tester.requireEmptyOutputs();

        /**
         * The new value may be retrieved by sending a clock-pulse.
         */
        tester.send(var.clockIn(), now4);
        tester.expect(var.dataOut(), 'C');
        tester.expect(var.clockOut(), now4);
        tester.requireEmptyOutputs();

        tester.run();
    }
}
