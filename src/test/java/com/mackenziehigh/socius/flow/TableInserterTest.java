package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.utils.ReactionTester;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class TableInserterTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ReactionTester tester = new ReactionTester();
        final TableInserter<Character, String> inserter = TableInserter.newTableInserter(tester.stage(), x -> x.charAt(0));
        final Output<String> outputA = inserter.selectIf('A');
        final Output<String> outputE = inserter.selectIf('E');
        final Output<String> others = inserter.dataOut();

        tester.send(inserter.dataIn(), "Autumn");
        tester.send(inserter.dataIn(), "Emma");
        tester.send(inserter.dataIn(), "Molly");
        tester.send(inserter.dataIn(), "Avril");
        tester.send(inserter.dataIn(), "Erin");
        tester.send(inserter.dataIn(), "Olivia");
        tester.send(inserter.dataIn(), "Ashley");
        tester.expect(outputA, "Autumn");
        tester.expect(outputA, "Avril");
        tester.expect(outputA, "Ashley");
        tester.expect(outputE, "Emma");
        tester.expect(outputE, "Erin");
        tester.expect(others, "Molly");
        tester.expect(others, "Olivia");
        tester.requireEmptyOutputs();
        tester.run();
    }
}
