package com.mackenziehigh.socius.flow;

import com.mackenziehigh.socius.testing.ActorTester;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class DuplicatorTest
{
    @Test
    public void test1 ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final Duplicator<Character> dup = Duplicator.<Character>newDuplicator(tester.stage())
                .withRepeatCount(4)
                .build();

        tester.send(dup.dataIn(), 'A');
        tester.send(dup.dataIn(), 'B');
        tester.send(dup.dataIn(), 'C');
        tester.expect(dup.dataOut(), 'A');
        tester.expect(dup.dataOut(), 'A');
        tester.expect(dup.dataOut(), 'A');
        tester.expect(dup.dataOut(), 'A');
        tester.expect(dup.dataOut(), 'B');
        tester.expect(dup.dataOut(), 'B');
        tester.expect(dup.dataOut(), 'B');
        tester.expect(dup.dataOut(), 'B');
        tester.expect(dup.dataOut(), 'C');
        tester.expect(dup.dataOut(), 'C');
        tester.expect(dup.dataOut(), 'C');
        tester.expect(dup.dataOut(), 'C');
        tester.requireEmptyOutputs();
        tester.run();
    }

    @Test
    public void test2 ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final Duplicator<Character> dup = Duplicator.<Character>newDuplicator(tester.stage())
                .withSequenceLength(3)
                .withRepeatCount(2)
                .build();

        tester.send(dup.dataIn(), 'A');
        tester.send(dup.dataIn(), 'B');
        tester.send(dup.dataIn(), 'C');
        tester.send(dup.dataIn(), 'X');
        tester.send(dup.dataIn(), 'Y');
        tester.send(dup.dataIn(), 'Z');
        tester.expect(dup.dataOut(), 'A');
        tester.expect(dup.dataOut(), 'B');
        tester.expect(dup.dataOut(), 'C');
        tester.expect(dup.dataOut(), 'A');
        tester.expect(dup.dataOut(), 'B');
        tester.expect(dup.dataOut(), 'C');
        tester.expect(dup.dataOut(), 'X');
        tester.expect(dup.dataOut(), 'Y');
        tester.expect(dup.dataOut(), 'Z');
        tester.expect(dup.dataOut(), 'X');
        tester.expect(dup.dataOut(), 'Y');
        tester.expect(dup.dataOut(), 'Z');
        tester.requireEmptyOutputs();
        tester.run();
    }
}
