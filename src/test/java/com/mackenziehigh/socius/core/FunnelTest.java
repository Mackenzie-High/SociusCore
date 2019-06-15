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

import com.mackenziehigh.socius.core.Funnel;
import com.mackenziehigh.socius.core.AsyncTestTool;
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
        final var tester = new AsyncTestTool();
        final var funnel = Funnel.newFunnel(tester.stage());

        tester.connect(funnel.dataOut());

        funnel.dataIn("A").send("Mercury");
        funnel.dataIn("B").send("Venus");
        tester.awaitEquals(funnel.dataOut(), "Mercury");
        tester.awaitEquals(funnel.dataOut(), "Venus");

        funnel.dataIn("A").send("Earth");
        funnel.dataIn("B").send("Mars");
        tester.awaitEquals(funnel.dataOut(), "Earth");
        tester.awaitEquals(funnel.dataOut(), "Mars");
    }
}
