package com.mackenziehigh.socius;

import org.junit.Test;

/**
 * Unit Test.
 */
public final class FanoutTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final Fanout<String> fanout = Fanout.newFanout(tester.stage());

        tester.send(fanout.dataIn(), "Mercury");
        tester.expect(fanout.dataOut("A"), "Mercury");
        tester.expect(fanout.dataOut("B"), "Mercury");
        tester.send(fanout.dataIn(), "Venus");
        tester.expect(fanout.dataOut("A"), "Venus");
        tester.expect(fanout.dataOut("B"), "Venus");
        tester.requireEmptyOutputs();
        tester.run();
    }
}
