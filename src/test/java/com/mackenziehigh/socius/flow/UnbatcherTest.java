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
package com.mackenziehigh.socius.flow;

import com.google.common.collect.Lists;
import com.mackenziehigh.socius.util.ActorTester;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class UnbatcherTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final Unbatcher<Character> unbatcher = Unbatcher.newUnbatcher(tester.stage(), 3);

        /**
         * Batch #1 - Normal.
         */
        tester.send(unbatcher.dataIn(), Lists.newArrayList('A', 'B', 'C'));
        tester.expect(unbatcher.dataOut(0), 'A');
        tester.expect(unbatcher.dataOut(1), 'B');
        tester.expect(unbatcher.dataOut(2), 'C');
        tester.requireEmptyOutputs();

        /**
         * Batch #2 - Normal.
         */
        tester.send(unbatcher.dataIn(), Lists.newArrayList('D', 'E', 'F'));
        tester.expect(unbatcher.dataOut(0), 'D');
        tester.expect(unbatcher.dataOut(1), 'E');
        tester.expect(unbatcher.dataOut(2), 'F');
        tester.requireEmptyOutputs();

        /**
         * Batch #3 - Too Small.
         */
        tester.send(unbatcher.dataIn(), Lists.newArrayList('G', 'H'));
        tester.expect(unbatcher.dataOut(0), 'G');
        tester.expect(unbatcher.dataOut(1), 'H');
        tester.requireEmptyOutputs();

        /**
         * Batch #4 - Normal.
         */
        tester.send(unbatcher.dataIn(), Lists.newArrayList('I', 'J', 'K'));
        tester.expect(unbatcher.dataOut(0), 'I');
        tester.expect(unbatcher.dataOut(1), 'J');
        tester.expect(unbatcher.dataOut(2), 'K');
        tester.requireEmptyOutputs();

        /**
         * Batch #6 - Too Large.
         */
        tester.send(unbatcher.dataIn(), Lists.newArrayList('L', 'M', 'N', 'O', 'P'));
        tester.expect(unbatcher.dataOut(0), 'L');
        tester.expect(unbatcher.dataOut(1), 'M');
        tester.expect(unbatcher.dataOut(2), 'N');
        tester.requireEmptyOutputs();

        /**
         * Batch #7 - Normal.
         */
        tester.send(unbatcher.dataIn(), Lists.newArrayList('Q', 'R', 'S'));
        tester.expect(unbatcher.dataOut(0), 'Q');
        tester.expect(unbatcher.dataOut(1), 'R');
        tester.expect(unbatcher.dataOut(2), 'S');
        tester.requireEmptyOutputs();

        tester.run();
    }
}
