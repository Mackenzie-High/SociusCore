package com.mackenziehigh.socius.flow;

import com.mackenziehigh.socius.testing.ActorTester;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class RoundRobinBalancerTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final RoundRobinBalancer<Character> balancer = RoundRobinBalancer.newBalancer(tester.stage(), 3);

        assertEquals(3, balancer.arity());

        tester.send(balancer.dataIn(), 'A');
        tester.send(balancer.dataIn(), 'B');
        tester.send(balancer.dataIn(), 'C');
        tester.send(balancer.dataIn(), 'D');
        tester.send(balancer.dataIn(), 'E');
        tester.send(balancer.dataIn(), 'F');
        tester.send(balancer.dataIn(), 'G');
        tester.send(balancer.dataIn(), 'H');
        tester.send(balancer.dataIn(), 'I');
        tester.expect(balancer.dataOut(0), 'A');
        tester.expect(balancer.dataOut(1), 'B');
        tester.expect(balancer.dataOut(2), 'C');
        tester.expect(balancer.dataOut(0), 'D');
        tester.expect(balancer.dataOut(1), 'E');
        tester.expect(balancer.dataOut(2), 'F');
        tester.expect(balancer.dataOut(0), 'G');
        tester.expect(balancer.dataOut(1), 'H');
        tester.expect(balancer.dataOut(2), 'I');
        tester.requireEmptyOutputs();

        tester.run();
    }
}
