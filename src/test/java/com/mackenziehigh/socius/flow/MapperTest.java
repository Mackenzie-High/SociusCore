package com.mackenziehigh.socius.flow;

import com.mackenziehigh.socius.testing.ActorTester;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class MapperTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final Mapper<Integer, String> actor = Mapper.newFunction(tester.stage(), (Integer x) -> String.format("%d ** 2 = %d", x, x * x));

        tester.send(actor.dataIn(), 2);
        tester.send(actor.dataIn(), 3);
        tester.send(actor.dataIn(), 4);
        tester.send(actor.dataIn(), 5);
        tester.send(actor.dataIn(), 6);
        tester.expect(actor.dataOut(), "2 ** 2 = 4");
        tester.expect(actor.dataOut(), "3 ** 2 = 9");
        tester.expect(actor.dataOut(), "4 ** 2 = 16");
        tester.expect(actor.dataOut(), "5 ** 2 = 25");
        tester.expect(actor.dataOut(), "6 ** 2 = 36");
        tester.requireEmptyOutputs();
        tester.run();
    }
}
