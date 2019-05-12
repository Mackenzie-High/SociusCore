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

import org.junit.Test;

/**
 * Unit Test.
 */
public final class DuplicatorTest
{
    @Test
    public void test1 ()
            throws Throwable
    {
        final var tester = new AsyncTestTool();
        final var dup = Duplicator.<Character>newDuplicator(tester.stage())
                .withRepeatCount(4)
                .build();

        tester.connect(dup.dataOut());

        dup.dataIn().send('A');
        dup.dataIn().send('B');
        dup.dataIn().send('C');

        tester.expect(dup.dataOut(), 'A');
        tester.expect(dup.dataOut(), 'A');
        tester.expect(dup.dataOut(), 'A');
        tester.expect(dup.dataOut(), 'A');

        tester.expect(dup.dataOut(), 'B');
        tester.expect(dup.dataOut(), 'B');
        tester.expect(dup.dataOut(), 'B');
        tester.expect(dup.dataOut(), 'B');

        tester.expect(dup.dataOut(), 'C');
        tester.expect(dup.dataOut(), 'C');
        tester.expect(dup.dataOut(), 'C');
        tester.expect(dup.dataOut(), 'C');
    }

    @Test
    public void test2 ()
            throws Throwable
    {
        final var tester = new AsyncTestTool();
        final var dup = Duplicator.<Character>newDuplicator(tester.stage())
                .withSequenceLength(3)
                .withRepeatCount(2)
                .build();

        tester.connect(dup.dataOut());

        dup.dataIn().send('A');
        dup.dataIn().send('B');
        dup.dataIn().send('C');
        dup.dataIn().send('X');
        dup.dataIn().send('Y');
        dup.dataIn().send('Z');

        tester.expect(dup.dataOut(), 'A');
        tester.expect(dup.dataOut(), 'B');
        tester.expect(dup.dataOut(), 'C');
        tester.expect(dup.dataOut(), 'A');
        tester.expect(dup.dataOut(), 'B');
        tester.expect(dup.dataOut(), 'C');
        tester.expect(dup.dataOut(), 'X');
        tester.expect(dup.dataOut(), 'Y');
        tester.expect(dup.dataOut(), 'Z');
        tester.expect(dup.dataOut(), 'X');
        tester.expect(dup.dataOut(), 'Y');
        tester.expect(dup.dataOut(), 'Z');
    }
}