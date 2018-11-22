package com.mackenziehigh.socius;

import org.junit.Test;

/**
 * Unit Test.
 */
public final class SimpleInserterTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final SimpleInserter<Integer> inserter = SimpleInserter.<Integer>newInserter(tester.stage())
                .selectIf(x -> x % 2 == 0)
                .build();

        tester.send(inserter.dataIn(), 1);
        tester.send(inserter.dataIn(), 2);
        tester.send(inserter.dataIn(), 3);
        tester.send(inserter.dataIn(), 4);
        tester.send(inserter.dataIn(), 5);
        tester.send(inserter.dataIn(), 6);
        tester.send(inserter.dataIn(), 7);
        tester.send(inserter.dataIn(), 8);
        tester.expect(inserter.dataOut(), 1);
        tester.expect(inserter.selectionsOut(), 2);
        tester.expect(inserter.dataOut(), 3);
        tester.expect(inserter.selectionsOut(), 4);
        tester.expect(inserter.dataOut(), 5);
        tester.expect(inserter.selectionsOut(), 6);
        tester.expect(inserter.dataOut(), 7);
        tester.expect(inserter.selectionsOut(), 8);
        tester.requireEmptyOutputs();
        tester.run();
    }
}
