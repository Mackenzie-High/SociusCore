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
public final class PipelineTest
{
    @Test
    public void testFromFunctionScript ()
            throws Throwable
    {
        final var tester = new AsyncTestTool();
        final Pipeline<Integer, String> actor = Pipeline.fromFunctionScript(tester.stage(), (Integer x) -> String.format("%d ** 2 = %d", x, x * x));

        tester.connect(actor.dataOut());

        actor.dataIn().send(2);
        actor.dataIn().send(3);
        actor.dataIn().send(4);
        actor.dataIn().send(5);
        actor.dataIn().send(6);

        tester.expect(actor.dataOut(), "2 ** 2 = 4");
        tester.expect(actor.dataOut(), "3 ** 2 = 9");
        tester.expect(actor.dataOut(), "4 ** 2 = 16");
        tester.expect(actor.dataOut(), "5 ** 2 = 25");
        tester.expect(actor.dataOut(), "6 ** 2 = 36");
    }
}
