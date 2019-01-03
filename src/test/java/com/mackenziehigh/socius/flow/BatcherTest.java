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
import com.mackenziehigh.socius.testing.ActorTester;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class BatcherTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final Batcher<Character> batcher = Batcher.<Character>newBatcher(tester.stage()).withArity(3).build();

        /**
         * Batch #1.
         */
        tester.send(batcher.dataIn(0), 'A').requireEmptyOutputs();
        tester.send(batcher.dataIn(1), 'B').requireEmptyOutputs();
        tester.send(batcher.dataIn(2), 'C');
        tester.expect(batcher.dataOut(), Lists.newArrayList('A', 'B', 'C'));
        tester.requireEmptyOutputs();

        /**
         * Batch #2.
         */
        tester.send(batcher.dataIn(0), 'D').requireEmptyOutputs();
        tester.send(batcher.dataIn(1), 'E').requireEmptyOutputs();
        tester.send(batcher.dataIn(2), 'F');
        tester.expect(batcher.dataOut(), Lists.newArrayList('D', 'E', 'F'));
        tester.requireEmptyOutputs();

        /**
         * Batch #3.
         */
        tester.send(batcher.dataIn(0), 'G').requireEmptyOutputs();
        tester.send(batcher.dataIn(1), 'H').requireEmptyOutputs();
        tester.send(batcher.dataIn(2), 'I');
        tester.expect(batcher.dataOut(), Lists.newArrayList('G', 'H', 'I'));
        tester.requireEmptyOutputs();

        tester.run();
    }
}
