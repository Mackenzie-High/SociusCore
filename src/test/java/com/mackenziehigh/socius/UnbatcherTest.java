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

import java.util.List;
import static org.junit.Assert.*;
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
        final var tester = new AsyncTestTool();
        final Unbatcher<Character> unbatcher = Unbatcher.newUnbatcher(tester.stage(), 3);

        assertEquals(3, unbatcher.arity());

        /**
         * Batch #1 - Normal.
         */
        unbatcher.dataIn().send(List.of('A', 'B', 'C'));
        tester.expect(unbatcher.dataOut(0), 'A');
        tester.expect(unbatcher.dataOut(1), 'B');
        tester.expect(unbatcher.dataOut(2), 'C');

        /**
         * Batch #2 - Normal.
         */
        unbatcher.dataIn().send(List.of('D', 'E', 'F'));
        tester.expect(unbatcher.dataOut(0), 'D');
        tester.expect(unbatcher.dataOut(1), 'E');
        tester.expect(unbatcher.dataOut(2), 'F');

        /**
         * Batch #3 - Too Small.
         */
        unbatcher.dataIn().send(List.of('G', 'H'));
        tester.expect(unbatcher.dataOut(0), 'G');
        tester.expect(unbatcher.dataOut(1), 'H');

        /**
         * Batch #4 - Normal.
         */
        unbatcher.dataIn().send(List.of('I', 'J', 'K'));
        tester.expect(unbatcher.dataOut(0), 'I');
        tester.expect(unbatcher.dataOut(1), 'J');
        tester.expect(unbatcher.dataOut(2), 'K');

        /**
         * Batch #6 - Too Large.
         */
        unbatcher.dataIn().send(List.of('L', 'M', 'N', 'O', 'P'));
        tester.expect(unbatcher.dataOut(0), 'L');
        tester.expect(unbatcher.dataOut(1), 'M');
        tester.expect(unbatcher.dataOut(2), 'N');

        /**
         * Batch #7 - Normal.
         */
        unbatcher.dataIn().send(List.of('Q', 'R', 'S'));
        tester.expect(unbatcher.dataOut(0), 'Q');
        tester.expect(unbatcher.dataOut(1), 'R');
        tester.expect(unbatcher.dataOut(2), 'S');
    }
}
