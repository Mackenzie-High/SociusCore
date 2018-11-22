package com.mackenziehigh.socius;

import com.mackenziehigh.socius.actors.Filter;
import com.mackenziehigh.socius.testing.ReactionTester;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class FilterTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ReactionTester tester = new ReactionTester();
        final Filter<String> actor = Filter.newFilter(tester.stage(), x -> !x.contains("e"));

        tester.send(actor.dataIn(), "avril");
        tester.send(actor.dataIn(), "emma");
        tester.send(actor.dataIn(), "erin");
        tester.send(actor.dataIn(), "t'pol");
        tester.send(actor.dataIn(), "elle");
        tester.send(actor.dataIn(), "olivia");
        tester.expect(actor.dataOut(), "avril");
        tester.expect(actor.dataOut(), "t'pol");
        tester.expect(actor.dataOut(), "olivia");
        tester.requireEmptyOutputs();
        tester.run();
    }
}
