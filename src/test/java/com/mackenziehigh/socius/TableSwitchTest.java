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
import org.junit.Test;

/**
 * Unit Test.
 */
public final class TableSwitchTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final TableSwitch<Character, String> inserter = TableSwitch.newTableInserter(tester.stage(), x -> x.charAt(0));
        final Output<String> outputA = inserter.selectIf('A');
        final Output<String> outputE = inserter.selectIf('E');
        final Output<String> others = inserter.dataOut();

        tester.send(inserter.dataIn(), "Autumn");
        tester.send(inserter.dataIn(), "Emma");
        tester.send(inserter.dataIn(), "Molly");
        tester.send(inserter.dataIn(), "Avril");
        tester.send(inserter.dataIn(), "Erin");
        tester.send(inserter.dataIn(), "Olivia");
        tester.send(inserter.dataIn(), "Ashley");
        tester.expect(outputA, "Autumn");
        tester.expect(outputA, "Avril");
        tester.expect(outputA, "Ashley");
        tester.expect(outputE, "Emma");
        tester.expect(outputE, "Erin");
        tester.expect(others, "Molly");
        tester.expect(others, "Olivia");
        tester.requireEmptyOutputs();
        tester.run();
    }
}
