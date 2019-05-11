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
public final class FunnelTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final Funnel<String> funnel = Funnel.newFunnel(tester.stage());

        tester.send(funnel.dataIn("A"), "Mercury");
        tester.send(funnel.dataIn("B"), "Venus");
        tester.expect(funnel.dataOut(), "Mercury");
        tester.expect(funnel.dataOut(), "Venus");
        tester.send(funnel.dataIn("A"), "Earth");
        tester.send(funnel.dataIn("B"), "Mars");
        tester.expect(funnel.dataOut(), "Earth");
        tester.expect(funnel.dataOut(), "Mars");
        tester.requireEmptyOutputs();
        tester.run();
    }
}
