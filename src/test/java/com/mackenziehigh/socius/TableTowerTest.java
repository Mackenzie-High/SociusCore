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

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import java.util.function.Function;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class TableTowerTest
{

    private final Stage stage = Cascade.newStage(1);

    private final AsyncTestTool tester = new AsyncTestTool();

    private final Function<Integer, Character> keyFunction = x -> String.valueOf(x).charAt(0);

    private final Pipeline<Integer, String> floorA = Pipeline.fromFunctionScript(stage, x -> "A" + x + "A");

    private final Pipeline<Integer, String> floorB = Pipeline.fromFunctionScript(stage, x -> "B" + x + "B");

    private final Pipeline<Integer, String> floorC = Pipeline.fromFunctionScript(stage, x -> "C" + x + "C");

    private final TableTower<Character, Integer, String> fixedTower = TableTower.<Character, Integer, String>newTableTower(stage)
            .withKeyFunction(keyFunction)
            .withFloor('1', floorA)
            .withFloor('2', floorB)
            .withFloor('3', floorC)
            .build();

    private final TableTower<Character, Integer, String> expandoTower = TableTower.<Character, Integer, String>newTableTower(stage)
            .withKeyFunction(keyFunction)
            .withFloor('1', floorA)
            .withFloor('2', floorB)
            .withFloor('3', floorC)
            .withAutoExpansion(this::floorFactory)
            .build();


    {
        tester.connect(fixedTower.dataOut());
        tester.connect(fixedTower.dropsOut());
        tester.connect(expandoTower.dataOut());
        tester.connect(expandoTower.dataOut());
    }

    private Pipeline<Integer, String> floorFactory (Integer message)
    {
        return Pipeline.fromFunctionScript(stage, x -> "AUTO" + x + "AUTO");
    }

    /**
     * Test: 20190514003742189062
     *
     * <p>
     * Case: Basic Throughput.
     * </p>
     */
    @Test
    public void test20190514003742189062 ()
    {
        fixedTower.dataIn().send(101);
        fixedTower.dataIn().send(201);
        fixedTower.dataIn().send(301);
        fixedTower.dataIn().send(401);
        fixedTower.dataIn().send(102);
        fixedTower.dataIn().send(202);
        fixedTower.dataIn().send(302);

        tester.expect(fixedTower.dataOut(), "A101A");
        tester.expect(fixedTower.dataOut(), "B201B");
        tester.expect(fixedTower.dataOut(), "C301C");

        tester.expect(fixedTower.dropsOut(), 401);

        tester.expect(fixedTower.dataOut(), "A102A");
        tester.expect(fixedTower.dataOut(), "B202B");
        tester.expect(fixedTower.dataOut(), "C302C");

    }

    /**
     * Test: 20190514004313647875
     *
     * <p>
     * Case: Auto Expansion.
     * </p>
     */
    @Test
    public void test20190514004313647875 ()
    {
        expandoTower.dataIn().send(101);
        expandoTower.dataIn().send(201);
        expandoTower.dataIn().send(301);
        expandoTower.dataIn().send(401);
        expandoTower.dataIn().send(102);
        expandoTower.dataIn().send(202);
        expandoTower.dataIn().send(302);

        tester.expect(expandoTower.dataOut(), "A101A");
        tester.expect(expandoTower.dataOut(), "B201B");
        tester.expect(expandoTower.dataOut(), "C301C");

        tester.expect(expandoTower.dataOut(), "AUTO401AUTO");

        tester.expect(expandoTower.dataOut(), "A102A");
        tester.expect(expandoTower.dataOut(), "B202B");
        tester.expect(expandoTower.dataOut(), "C302C");
    }
}
