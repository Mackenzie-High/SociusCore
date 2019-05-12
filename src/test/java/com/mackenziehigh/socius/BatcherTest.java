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
        final var tester = new AsyncTestTool();
        final var batcher = Batcher.<Character>newBatcher(tester.stage()).withArity(3).build();

        tester.connect(batcher.dataOut());

        /**
         * Batch #1.
         */
        batcher.dataIn(0).send('A');
        batcher.dataIn(1).send('B');
        batcher.dataIn(2).send('C');
        tester.expect(batcher.dataOut(), List.of('A', 'B', 'C'));

        /**
         * Batch #2.
         */
        batcher.dataIn(0).send('D');
        batcher.dataIn(1).send('E');
        batcher.dataIn(2).send('F');
        tester.expect(batcher.dataOut(), List.of('D', 'E', 'F'));

        /**
         * Batch #3.
         */
        batcher.dataIn(0).send('G');
        batcher.dataIn(1).send('H');
        batcher.dataIn(2).send('I');
        tester.expect(batcher.dataOut(), List.of('G', 'H', 'I'));
    }
}
