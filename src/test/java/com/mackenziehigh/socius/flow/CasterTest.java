package com.mackenziehigh.socius.flow;

import com.mackenziehigh.socius.testing.ActorTester;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class CasterTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final Caster<Number, Integer> caster = Caster.newCaster(tester.stage(), Integer.class);

        /**
         * Casting an integer should succeed.
         */
        tester.send(caster.dataIn(), 3);
        tester.expect(caster.dataOut(), 3);

        /**
         * Casting a double should fail.
         */
        tester.send(caster.dataIn(), 3.0);
        tester.expect(caster.errorOut(), 3.0);

        tester.requireEmptyOutputs();
        tester.run();
    }
}
