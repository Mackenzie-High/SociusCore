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

import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.ContextScript;
import java.util.Map;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class PipelineTest
{
    private final AsyncTestTool tester = new AsyncTestTool();

    /**
     * Test: 20190615155625059448
     *
     * <p>
     * Method: <code>fromActor</code>
     * </p>
     */
    @Test
    public void test20190615155625059448 ()
    {
        final Actor<Integer, Long> actor = tester
                .stage()
                .newActor()
                .withFunctionScript((Integer x) -> x * x * 1L)
                .create();

        final Pipeline<Integer, Long> pipe = Pipeline.fromActor(actor);

        tester.connect(pipe.dataOut());

        pipe.accept(2);
        pipe.accept(3);
        pipe.accept(4);
        pipe.accept(5);
        pipe.accept(6);

        tester.awaitEquals(pipe.dataOut(), 4L);
        tester.awaitEquals(pipe.dataOut(), 9L);
        tester.awaitEquals(pipe.dataOut(), 16L);
        tester.awaitEquals(pipe.dataOut(), 25L);
        tester.awaitEquals(pipe.dataOut(), 36L);
    }

    /**
     * Test: 20190615155625059499
     *
     * <p>
     * Method: <code>fromContextScript</code>
     * </p>
     */
    @Test
    public void test20190615155625059499 ()
    {
        final ContextScript<Integer, Long> script = (ctx, msg) ->
        {
            ctx.sendFrom(1L * msg * msg); // square
            ctx.sendFrom(1L * msg * msg * msg); // cube
        };

        final Pipeline<Integer, Long> actor = Pipeline.fromContextScript(tester.stage(), script);

        tester.connect(actor.dataOut());

        actor.accept(2);
        actor.accept(3);
        actor.accept(4);
        actor.accept(5);
        actor.accept(6);

        tester.awaitEquals(actor.dataOut(), 4L); // square = 2 ** 2
        tester.awaitEquals(actor.dataOut(), 8L); // cube = 2 ** 3
        tester.awaitEquals(actor.dataOut(), 9L); // square = 3 ** 2
        tester.awaitEquals(actor.dataOut(), 27L); // cube = 3 ** 3
        tester.awaitEquals(actor.dataOut(), 16L); // square = 4 ** 2
        tester.awaitEquals(actor.dataOut(), 64L); // cube = 4 ** 3
        tester.awaitEquals(actor.dataOut(), 25L); // square = 5 ** 2
        tester.awaitEquals(actor.dataOut(), 125L); // cube = 5 ** 3
        tester.awaitEquals(actor.dataOut(), 36L); // square = 6 ** 2
        tester.awaitEquals(actor.dataOut(), 216L); // cube = 6 ** 3
    }

    /**
     * Test: 20190615155625059523
     *
     * <p>
     * Method: <code>fromFunctionScript</code>
     * </p>
     */
    @Test
    public void test20190615155625059523 ()
    {
        final Pipeline<Integer, Long> actor = Pipeline.fromFunctionScript(tester.stage(), (Integer x) -> 1L * x * x);

        tester.connect(actor.dataOut());

        actor.accept(2);
        actor.accept(3);
        actor.accept(4);
        actor.accept(5);
        actor.accept(6);

        tester.awaitEquals(actor.dataOut(), 4L);
        tester.awaitEquals(actor.dataOut(), 9L);
        tester.awaitEquals(actor.dataOut(), 16L);
        tester.awaitEquals(actor.dataOut(), 25L);
        tester.awaitEquals(actor.dataOut(), 36L);
    }

    /**
     * Test: 20190615155625059595
     *
     * <p>
     * Method: <code>fromIO</code>
     * </p>
     */
    @Test
    public void test20190615155625059595 ()
    {
        final Pipeline<Integer, Long> delegate = Pipeline.fromFunctionScript(tester.stage(), (Integer x) -> 1L * x * x);

        final Pipeline<Integer, Long> actor = Pipeline.fromIO(delegate.dataIn(), delegate.dataOut());

        tester.connect(actor.dataOut());

        actor.accept(2);
        actor.accept(3);
        actor.accept(4);
        actor.accept(5);
        actor.accept(6);

        tester.awaitEquals(actor.dataOut(), 4L);
        tester.awaitEquals(actor.dataOut(), 9L);
        tester.awaitEquals(actor.dataOut(), 16L);
        tester.awaitEquals(actor.dataOut(), 25L);
        tester.awaitEquals(actor.dataOut(), 36L);
    }

    /**
     * Test: 20190615164138674936
     *
     * <p>
     * Method: <code>fromMap</code>
     * </p>
     */
    @Test
    public void test20190615164138674936 ()
    {
        final Map<Integer, String> map = Map.of(1, "Mercury", 2, "Venus", 3, "Earth", 4, "Mars", 9, "Pluto");

        final Pipeline<Integer, String> actor = Pipeline.fromMap(tester.stage(), map, "Jovian");

        tester.connect(actor.dataOut());

        actor.accept(1);
        actor.accept(2);
        actor.accept(3);
        actor.accept(4);
        actor.accept(5);
        actor.accept(6);
        actor.accept(7);
        actor.accept(8);
        actor.accept(9);

        tester.awaitEquals(actor.dataOut(), "Mercury");
        tester.awaitEquals(actor.dataOut(), "Venus");
        tester.awaitEquals(actor.dataOut(), "Earth");
        tester.awaitEquals(actor.dataOut(), "Mars");
        tester.awaitEquals(actor.dataOut(), "Jovian");
        tester.awaitEquals(actor.dataOut(), "Jovian");
        tester.awaitEquals(actor.dataOut(), "Jovian");
        tester.awaitEquals(actor.dataOut(), "Jovian");
        tester.awaitEquals(actor.dataOut(), "Pluto");
    }

}
