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
import static org.junit.Assert.*;
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

    private final TableTower<Character, Integer, String> tower = TableTower.<Character, Integer, String>newTableTower(stage)
            .withKeyFunction(keyFunction)
            .withFloor('1', floorA)
            .withFloor('2', floorB)
            .withFloor('3', floorC)
            .build();


    {
        tester.connect(tower.dataOut());
        tester.connect(tower.dropsOut());
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
        tower.dataIn().send(101); // Goes To Floor (A)
        tower.dataIn().send(201); // Goes To Floor (B)
        tower.dataIn().send(301); // Goes To Floor (C)
        tower.dataIn().send(401); // Dropped, because of no corresponding floor.
        tower.dataIn().send(102); // Goes To Floor (A)
        tower.dataIn().send(202); // Goes To Floor (B)
        tower.dataIn().send(302); // Goes To Floor (C)

        tester.awaitEquals(tower.dataOut(), "A101A");
        tester.awaitEquals(tower.dataOut(), "B201B");
        tester.awaitEquals(tower.dataOut(), "C301C");

        tester.awaitEquals(tower.dropsOut(), 401);

        tester.awaitEquals(tower.dataOut(), "A102A");
        tester.awaitEquals(tower.dataOut(), "B202B");
        tester.awaitEquals(tower.dataOut(), "C302C");
    }

    /**
     * Test: 20190615145510274432
     *
     * <p>
     * Method: <code>put</code>
     * </p>
     *
     * <p>
     * Case: Floor Added.
     * </p>
     */
    @Test
    public void test20190615145510274432 ()
    {
        final var floorD = Pipeline.fromFunctionScript(stage, (Integer x) -> "D" + x + "D");

        assertFalse(tower.floors().containsKey('4'));

        tower.put('4', floorD); // Method Under Test.

        assertTrue(tower.floors().containsKey('4'));
        assertEquals(4, tower.floors().size());

        tower.dataIn().send(101); // Goes To Floor (A)
        tower.dataIn().send(201); // Goes To Floor (B)
        tower.dataIn().send(301); // Goes To Floor (C)
        tower.dataIn().send(401); // Goes To Floor (D)

        tester.awaitEquals(tower.dataOut(), "A101A");
        tester.awaitEquals(tower.dataOut(), "B201B");
        tester.awaitEquals(tower.dataOut(), "C301C");
        tester.awaitEquals(tower.dataOut(), "D401D");
    }

    /**
     * Test: 20190615145510274485
     *
     * <p>
     * Method: <code>put</code>
     * </p>
     *
     * <p>
     * Case: Floor Already Exists.
     * </p>
     */
    @Test (expected = IllegalStateException.class)
    public void test20190615145510274485 ()
    {
        final var newFloor = Pipeline.fromFunctionScript(stage, (Integer x) -> "Z" + x + "Z");

        tower.put('2', newFloor); // Method Under Test.
    }

    /**
     * Test: 20190615145510274504
     *
     * <p>
     * Method: <code>remove</code>
     * </p>
     *
     * <p>
     * Case: Floor Removed.
     * </p>
     */
    @Test
    public void test20190615145510274504 ()
    {
        assertTrue(tower.floors().containsKey('2'));

        tower.remove('2'); // Method Under Test.

        assertFalse(tower.floors().containsKey('2'));
        assertEquals(2, tower.floors().size());

        tower.dataIn().send(101); // Goes To Floor (A)
        tower.dataIn().send(201); // Dropped, because of no corresponding floor.
        tower.dataIn().send(301); // Goes To Floor (C)

        tester.awaitEquals(tower.dataOut(), "A101A");
        tester.awaitEquals(tower.dataOut(), "C301C");

        tester.awaitEquals(tower.dropsOut(), 201);
    }

    /**
     * Test: 20190615145836689970
     *
     * <p>
     * Method: <code>remove</code>
     * </p>
     *
     * <p>
     * Case: No Such Floor.
     * </p>
     */
    @Test (expected = IllegalStateException.class)
    public void test20190615145836689970 ()
    {
        tower.remove('7');
    }
}
