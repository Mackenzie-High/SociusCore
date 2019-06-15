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
package com.mackenziehigh.socius;

import com.mackenziehigh.socius.RoundRobin;
import com.mackenziehigh.socius.AsyncTestTool;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class RoundRobinTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final var tester = new AsyncTestTool();
        final RoundRobin<Character> balancer = RoundRobin.newRoundRobin(tester.stage(), 3);

        assertEquals(3, balancer.arity());

        tester.connect(balancer.dataOut(0));
        tester.connect(balancer.dataOut(1));
        tester.connect(balancer.dataOut(2));

        balancer.dataIn().send('A');
        balancer.dataIn().send('B');
        balancer.dataIn().send('C');
        balancer.dataIn().send('D');
        balancer.dataIn().send('E');
        balancer.dataIn().send('F');
        balancer.dataIn().send('G');
        balancer.dataIn().send('H');
        balancer.dataIn().send('I');

        tester.awaitEquals(balancer.dataOut(0), 'A');
        tester.awaitEquals(balancer.dataOut(1), 'B');
        tester.awaitEquals(balancer.dataOut(2), 'C');
        tester.awaitEquals(balancer.dataOut(0), 'D');
        tester.awaitEquals(balancer.dataOut(1), 'E');
        tester.awaitEquals(balancer.dataOut(2), 'F');
        tester.awaitEquals(balancer.dataOut(0), 'G');
        tester.awaitEquals(balancer.dataOut(1), 'H');
        tester.awaitEquals(balancer.dataOut(2), 'I');
    }
}
