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
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class TableSwitchTest
{
    private final AsyncTestTool tester = new AsyncTestTool();

    /**
     * Case: Basic Throughput.
     */
    @Test
    public void test1 ()
    {
        final TableSwitch<Character, String> tableSwitch = TableSwitch.newTableSwitch(tester.stage(), x -> x.charAt(0));
        final Output<String> outputA = tableSwitch.selectIf('A');
        final Output<String> outputE = tableSwitch.selectIf('E');
        final Output<String> others = tableSwitch.dataOut();

        tester.connect(outputA);
        tester.connect(outputE);
        tester.connect(others);

        tableSwitch.dataIn().send("Autumn");
        tableSwitch.dataIn().send("Emma");
        tableSwitch.dataIn().send("Molly");
        tableSwitch.dataIn().send("Avril");
        tableSwitch.dataIn().send("Erin");
        tableSwitch.dataIn().send("Olivia");
        tableSwitch.dataIn().send("Ashley");
        tester.awaitEquals(outputA, "Autumn");
        tester.awaitEquals(outputA, "Avril");
        tester.awaitEquals(outputA, "Ashley");
        tester.awaitEquals(outputE, "Emma");
        tester.awaitEquals(outputE, "Erin");
        tester.awaitEquals(others, "Molly");
        tester.awaitEquals(others, "Olivia");
    }

    /**
     * Case: <code>selectIf()</code> always returns the same object given the same key.
     */
    @Test
    public void test2 ()
    {
        final TableSwitch<Character, String> tableSwitch = TableSwitch.newTableSwitch(tester.stage(), x -> x.charAt(0));

        assertSame(tableSwitch.selectIf('A'), tableSwitch.selectIf('A'));
    }
}
