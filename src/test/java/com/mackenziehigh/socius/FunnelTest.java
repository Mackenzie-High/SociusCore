package com.mackenziehigh.socius;

import org.junit.Test;

/**
 * Unit Test.
 */
public final class FunnelTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final Funnel<String> funnel = Funnel.newFunnel(tester.stage());

        tester.send(funnel.dataIn("A"), "Mercury");
        tester.send(funnel.dataIn("B"), "Venus");
        tester.expect(funnel.dataOut(), "Mercury");
        tester.expect(funnel.dataOut(), "Venus");
        tester.send(funnel.dataIn("A"), "Earth");
        tester.send(funnel.dataIn("B"), "Mars");
        tester.expect(funnel.dataOut(), "Earth");
        tester.expect(funnel.dataOut(), "Mars");
        tester.requireEmptyOutputs();
        tester.run();
    }
}
