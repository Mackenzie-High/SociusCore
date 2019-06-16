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
public final class LookupTowerTest
{
    private final Stage stage = Cascade.newStage(1);

    private final AsyncTestTool tester = new AsyncTestTool();

    private final Function<Integer, String> keyFunction = x -> String.valueOf(x).substring(0, 1);

    private final Pipeline<Integer, String> floorA = Pipeline.fromFunctionScript(stage, x -> "A" + x + "A");

    private final Pipeline<Integer, String> floorB = Pipeline.fromFunctionScript(stage, x -> "B" + x + "B");

    private final Pipeline<Integer, String> floorC = Pipeline.fromFunctionScript(stage, x -> "C" + x + "C");

    private final LookupTower<Integer, String> tower = LookupTower.<Integer, String>newLookupTower(stage)
            .withFloor(x -> "1".equals(keyFunction.apply(x)), floorA)
            .withFloor(x -> "2".equals(keyFunction.apply(x)), floorB)
            .withFloor(x -> "3".equals(keyFunction.apply(x)), floorC)
            .build();


    {
        tester.connect(tower.dataOut());
        tester.connect(tower.dropsOut());
    }

    /**
     * Test: 20190615153710763439
     *
     * <p>
     * Case: Basic Throughput.
     * </p>
     */
    @Test
    public void test20190615153710763439 ()
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
     * Test: 20190615154145279064
     *
     * <p>
     * Method: <code>floors</code>
     * </p>
     *
     * <p>
     * Case: Verify List Content.
     * </p>
     */
    @Test
    public void test20190615154145279064 ()
    {
        final var floors = tower.floors();

        assertEquals(3, floors.size());

        assertSame(floorA.dataIn(), floors.get(0).dataIn());
        assertSame(floorA.dataOut(), floors.get(0).dataOut());

        assertSame(floorB.dataIn(), floors.get(1).dataIn());
        assertSame(floorB.dataOut(), floors.get(1).dataOut());

        assertSame(floorC.dataIn(), floors.get(2).dataIn());
        assertSame(floorC.dataOut(), floors.get(2).dataOut());
    }
}
