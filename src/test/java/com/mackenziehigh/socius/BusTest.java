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
public final class BusTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final var tester = new AsyncTestTool();
        final var bus = Bus.newBus(tester.stage());

        tester.connect(bus.dataOut("X"));
        tester.connect(bus.dataOut("Y"));

        bus.dataIn("A").send("Avril");
        tester.expect(bus.dataOut("X"), "Avril");
        tester.expect(bus.dataOut("Y"), "Avril");

        bus.dataIn("E").send("Emma");
        tester.expect(bus.dataOut("X"), "Emma");
        tester.expect(bus.dataOut("Y"), "Emma");
    }
}
