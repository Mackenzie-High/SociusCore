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

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
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
        final var tester = new AsyncTestTool();
        final LookupSwitch<Integer> inserter = LookupSwitch.newLookupSwitch(tester.stage());
        final Output<Integer> primes = inserter.selectIf(x -> BigInteger.valueOf(x).isProbablePrime(10));
        final Output<Integer> power2 = inserter.selectIf(x -> (x & (x - 1)) == 0);
        final Output<Integer> others = inserter.dataOut();

        tester.connect(primes);
        tester.connect(power2);
        tester.connect(others);

        inserter.dataIn().send(2);
        inserter.dataIn().send(3);
        inserter.dataIn().send(4);
        inserter.dataIn().send(5);
        inserter.dataIn().send(6);
        inserter.dataIn().send(7);
        inserter.dataIn().send(8);
        inserter.dataIn().send(9);
        inserter.dataIn().send(10);
        inserter.dataIn().send(11);

        tester.awaitEquals(primes, 2); // primes has higher precedence than power2 (declared first).
        tester.awaitEquals(primes, 3);
        tester.awaitEquals(power2, 4);
        tester.awaitEquals(primes, 5);
        tester.awaitEquals(others, 6);
        tester.awaitEquals(primes, 7);
        tester.awaitEquals(power2, 8);
        tester.awaitEquals(others, 9);
        tester.awaitEquals(others, 10);
        tester.awaitEquals(primes, 11);
    }
}
