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

import org.junit.Test;

/**
 * Unit Test.
 */
public final class ProcessorTest
{
    @Test
    public void testFromFunctionScript ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final Processor<Integer> actor = Processor.fromFunctionScript(tester.stage(), (Integer x) -> x * x);

        tester.send(actor.dataIn(), 2);
        tester.send(actor.dataIn(), 3);
        tester.send(actor.dataIn(), 4);
        tester.send(actor.dataIn(), 5);
        tester.send(actor.dataIn(), 6);
        tester.expect(actor.dataOut(), 4);
        tester.expect(actor.dataOut(), 9);
        tester.expect(actor.dataOut(), 16);
        tester.expect(actor.dataOut(), 25);
        tester.expect(actor.dataOut(), 36);
        tester.requireEmptyOutputs();
        tester.run();
    }

    @Test
    public void testFilter ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final Processor<String> actor = Processor.fromFilter(tester.stage(), x -> !x.contains("e"));

        tester.send(actor.dataIn(), "avril");
        tester.send(actor.dataIn(), "emma");
        tester.send(actor.dataIn(), "erin");
        tester.send(actor.dataIn(), "t'pol");
        tester.send(actor.dataIn(), "elle");
        tester.send(actor.dataIn(), "olivia");
        tester.expect(actor.dataOut(), "avril");
        tester.expect(actor.dataOut(), "t'pol");
        tester.expect(actor.dataOut(), "olivia");
        tester.requireEmptyOutputs();
        tester.run();
    }
}
