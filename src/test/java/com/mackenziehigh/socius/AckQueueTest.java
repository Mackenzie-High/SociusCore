package com.mackenziehigh.socius;

import com.mackenziehigh.socius.actors.AckQueue;
import com.mackenziehigh.socius.testing.ReactionTester;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class AckQueueTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ReactionTester tester = new ReactionTester();
        final AckQueue<Character, Integer> queue = AckQueue.<Character, Integer>newAckQueue(tester.stage())
                .withBacklogCapacity(5)
                .withInFlightPermits(3)
                .build();

        tester.send(queue.dataIn(), 'A'); // 1  (send direct)
        tester.send(queue.dataIn(), 'B'); // 2  (send direct)
        tester.send(queue.dataIn(), 'C'); // 3  (send direct)
        tester.send(queue.dataIn(), 'D'); // 4  (add to backlog)
        tester.send(queue.dataIn(), 'E'); // 5  (add to backlog)
        tester.send(queue.dataIn(), 'F'); // 6  (add to backlog)
        tester.send(queue.dataIn(), 'G'); // 7  (add to backlog)
        tester.send(queue.dataIn(), 'H'); // 8  (add to backlog)
        tester.send(queue.dataIn(), 'I'); // 9  (overflow)
        tester.send(queue.dataIn(), 'J'); // 10 (overflow)
        tester.printOutput(queue.dataOut());
        tester.expect(queue.dataOut(), 'A');
        tester.expect(queue.dataOut(), 'B');
        tester.expect(queue.dataOut(), 'C');
        tester.expect(queue.overflowOut(), 'I');
        tester.expect(queue.overflowOut(), 'J');
        tester.requireEmptyOutputs();
        tester.send(queue.acksIn(), 4);
        tester.send(queue.acksIn(), 5);
        tester.send(queue.acksIn(), 6);
        tester.expect(queue.dataOut(), 'D');
        tester.expect(queue.dataOut(), 'E');
        tester.expect(queue.dataOut(), 'F');
        tester.requireEmptyOutputs();
        tester.send(queue.acksIn(), 7);
        tester.send(queue.acksIn(), 8);
        tester.requireEmptyOutputs();
        tester.expect(queue.dataOut(), 'G');
        tester.expect(queue.dataOut(), 'H');
        tester.run();

    }
}
