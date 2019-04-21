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
package com.mackenziehigh.socius.flow;

import com.mackenziehigh.socius.util.ActorTester;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class ShuntingYardTest
{
    private final ActorTester tester = new ActorTester();

    /**
     * Test: 20181210220416465169
     *
     * <p>
     * Case: Throughput.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20181210220416465169 ()
            throws Throwable
    {
        final ShuntingYard<Integer, String> object = ShuntingYard.<Integer, String>newBuilder(tester.stage())
                .withOption(x -> x % 3 == 0, x -> "fizz = " + x)
                .withOption(x -> x % 5 == 0, x -> "buzz = " + x)
                .build();

        tester.send(object.dataIn(), 1);
        tester.expect(object.dropsOut(), 1);

        tester.send(object.dataIn(), 2);
        tester.expect(object.dropsOut(), 2);

        tester.send(object.dataIn(), 3);
        tester.expect(object.dataOut(), "fizz = 3");

        tester.send(object.dataIn(), 4);
        tester.expect(object.dropsOut(), 4);

        tester.send(object.dataIn(), 5);
        tester.expect(object.dataOut(), "buzz = 5");

        tester.send(object.dataIn(), 6);
        tester.expect(object.dataOut(), "fizz = 6");

        tester.send(object.dataIn(), 7);
        tester.expect(object.dropsOut(), 7);

        tester.send(object.dataIn(), 8);
        tester.expect(object.dropsOut(), 8);

        tester.send(object.dataIn(), 9);
        tester.expect(object.dataOut(), "fizz = 9");

        tester.send(object.dataIn(), 10);
        tester.expect(object.dataOut(), "buzz = 10");

        tester.send(object.dataIn(), 11);
        tester.expect(object.dropsOut(), 11);

        tester.send(object.dataIn(), 12);
        tester.expect(object.dataOut(), "fizz = 12");

        tester.send(object.dataIn(), 13);
        tester.expect(object.dropsOut(), 13);

        tester.send(object.dataIn(), 14);
        tester.expect(object.dropsOut(), 14);

        tester.send(object.dataIn(), 15);
        tester.expect(object.dataOut(), "fizz = 15");

        tester.send(object.dataIn(), 16);
        tester.expect(object.dropsOut(), 16);

        tester.send(object.dataIn(), 17);
        tester.expect(object.dropsOut(), 17);

        tester.requireEmptyOutputs();
        tester.run();
    }
}
