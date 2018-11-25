package com.mackenziehigh.socius.flow;

import com.google.common.collect.ImmutableList;
import com.mackenziehigh.socius.testing.ActorTester;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class BatchInserterTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();

        /**
         * Each batch requires one 'A', two 'B', and one 'C'.
         */
        final BatchInserter<Character> inserter = BatchInserter.<Character>newInserter(tester.stage())
                .require(x -> x == 'A')
                .require(x -> x == 'B')
                .require(x -> x == 'B')
                .require(x -> x == 'C')
                .build();

        tester.send(inserter.dataIn(), 'A').requireEmptyOutputs();
        tester.send(inserter.dataIn(), 'A').expect(inserter.dataOut(), 'A'); // Batch already has 'A'.
        tester.send(inserter.dataIn(), 'A').expect(inserter.dataOut(), 'A'); // Batch already has 'A'.
        tester.send(inserter.dataIn(), 'B').requireEmptyOutputs();
        tester.send(inserter.dataIn(), 'B').requireEmptyOutputs();
        tester.send(inserter.dataIn(), 'B').expect(inserter.dataOut(), 'B'); // Batch already has two 'B'.
        tester.send(inserter.dataIn(), 'C');
        tester.expect(inserter.batchOut(), ImmutableList.of('A', 'B', 'B', 'C'));
        // Start of second batch.
        tester.send(inserter.dataIn(), 'C').requireEmptyOutputs();
        tester.send(inserter.dataIn(), 'C').expect(inserter.dataOut(), 'C'); // Batch already has 'C'.
        tester.send(inserter.dataIn(), 'A').requireEmptyOutputs();
        tester.send(inserter.dataIn(), 'A').expect(inserter.dataOut(), 'A'); // Batch already has 'A'.
        tester.send(inserter.dataIn(), 'B').requireEmptyOutputs();
        tester.send(inserter.dataIn(), 'X').expect(inserter.dataOut(), 'X'); // No Match
        tester.send(inserter.dataIn(), 'B');
        tester.expect(inserter.batchOut(), ImmutableList.of('A', 'B', 'B', 'C')); // (C, A, B, B) == (A, B, B, C).
        tester.send(inserter.dataIn(), 'Y').expect(inserter.dataOut(), 'Y'); // No Match
        tester.send(inserter.dataIn(), 'Z').expect(inserter.dataOut(), 'Z'); // No Match
        tester.requireEmptyOutputs();

        tester.run();
    }
}
