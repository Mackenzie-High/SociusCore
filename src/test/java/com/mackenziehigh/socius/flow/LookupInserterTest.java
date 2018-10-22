package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.utils.ReactionTester;
import java.math.BigInteger;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class LookupInserterTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ReactionTester tester = new ReactionTester();
        final LookupInserter<Integer> inserter = LookupInserter.newLookupInserter(tester.stage());
        final Output<Integer> primes = inserter.selectIf(x -> BigInteger.valueOf(x).isProbablePrime(10));
        final Output<Integer> power2 = inserter.selectIf(x -> (x & (x - 1)) == 0);
        final Output<Integer> others = inserter.dataOut();

        tester.send(inserter.dataIn(), 2);
        tester.send(inserter.dataIn(), 3);
        tester.send(inserter.dataIn(), 4);
        tester.send(inserter.dataIn(), 5);
        tester.send(inserter.dataIn(), 6);
        tester.send(inserter.dataIn(), 7);
        tester.send(inserter.dataIn(), 8);
        tester.send(inserter.dataIn(), 9);
        tester.send(inserter.dataIn(), 10);
        tester.send(inserter.dataIn(), 11);
        tester.expect(primes, 2); // primes has higher precedence than power2 (declared first).
        tester.expect(primes, 3);
        tester.expect(power2, 4);
        tester.expect(primes, 5);
        tester.expect(others, 6);
        tester.expect(primes, 7);
        tester.expect(power2, 8);
        tester.expect(others, 9);
        tester.expect(others, 10);
        tester.expect(primes, 11);
        tester.requireEmptyOutputs();
        tester.run();
    }
}
