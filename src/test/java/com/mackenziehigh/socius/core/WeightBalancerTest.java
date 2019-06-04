/*
 * Copyright 2019 Michael Mackenzie High
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mackenziehigh.socius.core;

import com.mackenziehigh.socius.core.Processor;
import com.mackenziehigh.socius.core.AsyncTestTool;
import com.mackenziehigh.socius.core.WeightBalancer;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class WeightBalancerTest
{
    private final AsyncTestTool tester = new AsyncTestTool();

    private final WeightBalancer<Integer> balancer = WeightBalancer.newWeightBalancer(tester.stage(), 3, x -> x);

    private final Processor<Integer> actor0 = Processor.fromIdentityScript(tester.stage());

    private final Processor<Integer> actor1 = Processor.fromIdentityScript(tester.stage());

    private final Processor<Integer> actor2 = Processor.fromIdentityScript(tester.stage());


    {
        balancer.dataOut(0).connect(actor0.dataIn());
        balancer.dataOut(1).connect(actor1.dataIn());
        balancer.dataOut(2).connect(actor2.dataIn());

        tester.connect(actor0.dataOut());
        tester.connect(actor1.dataOut());
        tester.connect(actor2.dataOut());
    }

    @Test
    public void test ()
            throws Throwable
    {
        // Postcondition:
        //     actor0 weight = 101
        //     actor1 weight = 0
        //     actor2 weight = 0
        balancer.dataIn().send(101);
        tester.expect(actor0.dataOut(), 101);
        assertEquals(101, balancer.sumOf(0));
        assertEquals(0, balancer.sumOf(1));
        assertEquals(0, balancer.sumOf(2));

        // Postcondition:
        //     actor0 weight = 101
        //     actor1 weight = 102
        //     actor2 weight = 0
        balancer.dataIn().send(102);
        tester.expect(actor1.dataOut(), 102);
        assertEquals(101, balancer.sumOf(0));
        assertEquals(102, balancer.sumOf(1));
        assertEquals(0, balancer.sumOf(2));

        // Postcondition:
        //     actor0 weight = 101
        //     actor1 weight = 102
        //     actor2 weight = 103
        balancer.dataIn().send(103);
        tester.expect(actor2.dataOut(), 103);
        assertEquals(101, balancer.sumOf(0));
        assertEquals(102, balancer.sumOf(1));
        assertEquals(103, balancer.sumOf(2));

        // Postcondition:
        //     actor0 weight = 205
        //     actor1 weight = 102
        //     actor2 weight = 103
        balancer.dataIn().send(104);
        tester.expect(actor0.dataOut(), 104);
        assertEquals(205, balancer.sumOf(0));
        assertEquals(102, balancer.sumOf(1));
        assertEquals(103, balancer.sumOf(2));

        // Postcondition:
        //     actor0 weight = 205
        //     actor1 weight = 207
        //     actor2 weight = 103
        balancer.dataIn().send(105);
        tester.expect(actor1.dataOut(), 105);
        assertEquals(205, balancer.sumOf(0));
        assertEquals(207, balancer.sumOf(1));
        assertEquals(103, balancer.sumOf(2));

        // Postcondition:
        //     actor0 weight = 205
        //     actor1 weight = 207
        //     actor2 weight = 209
        balancer.dataIn().send(106);
        tester.expect(actor2.dataOut(), 106);
        assertEquals(205, balancer.sumOf(0));
        assertEquals(207, balancer.sumOf(1));
        assertEquals(209, balancer.sumOf(2));

        // Postcondition:
        //     actor0 weight = 605
        //     actor1 weight = 207
        //     actor2 weight = 209
        balancer.dataIn().send(400);
        tester.expect(actor0.dataOut(), 400);
        assertEquals(605, balancer.sumOf(0));
        assertEquals(207, balancer.sumOf(1));
        assertEquals(209, balancer.sumOf(2));

        // Postcondition:
        //     actor0 weight = 605
        //     actor1 weight = 257
        //     actor2 weight = 209
        balancer.dataIn().send(50);
        tester.expect(actor1.dataOut(), 50);
        assertEquals(605, balancer.sumOf(0));
        assertEquals(257, balancer.sumOf(1));
        assertEquals(209, balancer.sumOf(2));

        // Postcondition:
        //     actor0 weight = 605
        //     actor1 weight = 257
        //     actor2 weight = 509
        balancer.dataIn().send(300);
        tester.expect(actor2.dataOut(), 300);
        assertEquals(605, balancer.sumOf(0));
        assertEquals(257, balancer.sumOf(1));
        assertEquals(509, balancer.sumOf(2));

        // Postcondition:
        //     actor0 weight = 605
        //     actor1 weight = 317
        //     actor2 weight = 509
        balancer.dataIn().send(60);
        tester.expect(actor1.dataOut(), 60);
        assertEquals(605, balancer.sumOf(0));
        assertEquals(317, balancer.sumOf(1));
        assertEquals(509, balancer.sumOf(2));

        // Postcondition:
        //     actor0 weight = 605
        //     actor1 weight = 400
        //     actor2 weight = 509
        balancer.dataIn().send(83);
        tester.expect(actor1.dataOut(), 83);
        assertEquals(605, balancer.sumOf(0));
        assertEquals(400, balancer.sumOf(1));
        assertEquals(509, balancer.sumOf(2));

        // Postcondition:
        //     actor0 weight = 605
        //     actor1 weight = 509
        //     actor2 weight = 509
        balancer.dataIn().send(109);
        tester.expect(actor1.dataOut(), 109);
        assertEquals(605, balancer.sumOf(0));
        assertEquals(509, balancer.sumOf(1));
        assertEquals(509, balancer.sumOf(2));

        // Postcondition:
        //     actor0 weight = 605
        //     actor1 weight = 600
        //     actor2 weight = 509
        balancer.dataIn().send(91);
        tester.expect(actor1.dataOut(), 91);
        assertEquals(605, balancer.sumOf(0));
        assertEquals(600, balancer.sumOf(1));
        assertEquals(509, balancer.sumOf(2));

        // Postcondition:
        //     actor0 weight = 605
        //     actor1 weight = 600
        //     actor2 weight = 540
        balancer.dataIn().send(31);
        tester.expect(actor2.dataOut(), 31);
        assertEquals(605, balancer.sumOf(0));
        assertEquals(600, balancer.sumOf(1));
        assertEquals(540, balancer.sumOf(2));

        // Postcondition:
        //     actor0 weight = 605
        //     actor1 weight = 600
        //     actor2 weight = 605
        balancer.dataIn().send(65);
        tester.expect(actor2.dataOut(), 65);
        assertEquals(605, balancer.sumOf(0));
        assertEquals(600, balancer.sumOf(1));
        assertEquals(605, balancer.sumOf(2));

        // Postcondition:
        //     actor0 weight = 605
        //     actor1 weight = 620
        //     actor2 weight = 605
        balancer.dataIn().send(20);
        tester.expect(actor1.dataOut(), 20);
        assertEquals(605, balancer.sumOf(0));
        assertEquals(620, balancer.sumOf(1));
        assertEquals(605, balancer.sumOf(2));
    }
}
