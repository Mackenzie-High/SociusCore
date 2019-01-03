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

import com.mackenziehigh.socius.testing.ActorTester;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class FanoutTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final Fanout<String> fanout = Fanout.newFanout(tester.stage());

        tester.send(fanout.dataIn(), "Mercury");
        tester.expect(fanout.dataOut("A"), "Mercury");
        tester.expect(fanout.dataOut("B"), "Mercury");
        tester.send(fanout.dataIn(), "Venus");
        tester.expect(fanout.dataOut("A"), "Venus");
        tester.expect(fanout.dataOut("B"), "Venus");
        tester.requireEmptyOutputs();
        tester.run();
    }
}
