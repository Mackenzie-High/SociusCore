package com.mackenziehigh.socius.flow;

import com.mackenziehigh.socius.testing.ReactionTester;
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
        final ReactionTester tester = new ReactionTester();
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
