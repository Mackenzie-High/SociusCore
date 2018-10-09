package com.mackenziehigh.socius.flow;

import com.google.common.collect.Lists;
import com.mackenziehigh.socius.testing.ReactionTester;
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
        final ReactionTester tester = new ReactionTester();
        final Batcher<Character> batcher = Batcher.newBatcher(tester.stage(), 3);

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
