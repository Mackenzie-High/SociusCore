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
        final ActorTester tester = new ActorTester();
        final RoundRobin<Character> balancer = RoundRobin.newRoundRobin(tester.stage(), 3);

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
