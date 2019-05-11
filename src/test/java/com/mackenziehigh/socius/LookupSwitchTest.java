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

import com.mackenziehigh.socius.LookupSwitch;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.util.ActorTester;
import java.math.BigInteger;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class LookupSwitchTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final LookupSwitch<Integer> inserter = LookupSwitch.newLookupInserter(tester.stage());
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
