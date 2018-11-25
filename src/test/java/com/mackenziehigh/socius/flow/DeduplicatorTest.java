package com.mackenziehigh.socius.flow;

import com.mackenziehigh.socius.testing.ActorTester;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class DeduplicatorTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final Deduplicator<String, String> dedup = Deduplicator.<String, String>newDeduplicator(tester.stage())
                .withKeyFunction(x -> x)
                .withCapacity(3)
                .build();
        tester.stage().addErrorHandler(System.out::println);

        tester.send(dedup.dataIn(), "A");
        tester.send(dedup.dataIn(), "B");
        tester.send(dedup.dataIn(), "C");
        tester.send(dedup.dataIn(), "D");
        tester.send(dedup.dataIn(), "E");
        tester.send(dedup.dataIn(), "E");
        tester.send(dedup.dataIn(), "F");
        tester.expect(dedup.dataOut(), "A");
        tester.expect(dedup.dataOut(), "B");
        tester.expect(dedup.dataOut(), "C");
        tester.expect(dedup.dataOut(), "D");
        tester.expect(dedup.dataOut(), "E");
        tester.expect(dedup.dataOut(), "F");
        tester.run();
    }
}
