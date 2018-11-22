package com.mackenziehigh.socius;

import org.junit.Test;

/**
 * Unit Test.
 */
public final class ProcessorTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final Processor<Integer> actor = Processor.newProcessor(tester.stage(), (Integer x) -> x * x);

        tester.send(actor.dataIn(), 2);
        tester.send(actor.dataIn(), 3);
        tester.send(actor.dataIn(), 4);
        tester.send(actor.dataIn(), 5);
        tester.send(actor.dataIn(), 6);
        tester.expect(actor.dataOut(), 4);
        tester.expect(actor.dataOut(), 9);
        tester.expect(actor.dataOut(), 16);
        tester.expect(actor.dataOut(), 25);
        tester.expect(actor.dataOut(), 36);
        tester.requireEmptyOutputs();
        tester.run();
    }
}
