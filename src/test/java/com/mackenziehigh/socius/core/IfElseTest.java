package com.mackenziehigh.socius.core;

import com.mackenziehigh.socius.core.IfElse;
import com.mackenziehigh.socius.core.ActorTester;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class IfElseTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final IfElse<String> actor = IfElse.newIfElse(tester.stage(), x -> x.contains("e"));

        tester.send(actor.dataIn(), "avril");
        tester.send(actor.dataIn(), "emma");
        tester.send(actor.dataIn(), "erin");
        tester.send(actor.dataIn(), "t'pol");
        tester.expect(actor.falseOut(), "avril");
        tester.expect(actor.trueOut(), "emma");
        tester.expect(actor.trueOut(), "erin");
        tester.expect(actor.falseOut(), "t'pol");
        tester.requireEmptyOutputs();
        tester.run();
    }
}
