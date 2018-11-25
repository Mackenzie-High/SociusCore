package com.mackenziehigh.socius.flow;

import com.mackenziehigh.socius.testing.ActorTester;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class BusTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final Bus<String> bus = Bus.newBus(tester.stage());

        tester.send(bus.dataIn("A"), "Avril");
        tester.expect(bus.dataOut("X"), "Avril");
        tester.expect(bus.dataOut("Y"), "Avril");
        tester.send(bus.dataIn("E"), "Emma");
        tester.expect(bus.dataOut("X"), "Emma");
        tester.expect(bus.dataOut("Y"), "Emma");
        tester.requireEmptyOutputs();
        tester.run();
    }
}
