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

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class CommandDispatcherTest
{
    private static final class Command
    {
        public final String name;

        public final Integer value;

        public Command (final String name,
                        final Integer value)
        {
            this.name = name;
            this.value = value;
        }
    }

    private final AsyncTestTool tool = new AsyncTestTool();

    private final BlockingQueue<Integer> printed = new LinkedBlockingQueue<>();

    private final CommandDispatcher<String, Command, Integer> dispatcher = CommandDispatcher.<String, Command, Integer>newCommandDispatcher(tool.stage())
            .withKeyFunction(x -> x.name)
            .declareConsumer("printSquare", x -> printed.add(x.value * x.value))
            .declareConsumer("printCube", x -> printed.add(x.value * x.value * x.value))
            .declareFunction("computeSquare", x -> x.value * x.value)
            .declareFunction("computeCube", x -> x.value * x.value * x.value)
            .build();


    {
        tool.connect(dispatcher.dataOut());
        tool.connect(dispatcher.dropsOut());
    }

    /**
     * Test: 20190614234612478822
     *
     * <p>
     * Case: Dispatching of Function Calls.
     * </p>
     */
    @Test
    public void test20190614234612478822 ()
    {
        tool.setAwaitTimeout(Duration.ofMinutes(1));

        /**
         * Invocation of Consumer.
         */
        dispatcher.accept(new Command("printSquare", 2));
        tool.awaitSteadyState();
        assertEquals(2 * 2, (Object) printed.poll());

        /**
         * Invocation of Consumer.
         */
        dispatcher.accept(new Command("printCube", 3));
        tool.awaitSteadyState();
        assertEquals(3 * 3 * 3, (Object) printed.poll());

        /**
         * Invocation of Function.
         */
        dispatcher.accept(new Command("computeSquare", 5));
        tool.awaitEquals(dispatcher.dataOut(), 5 * 5);

        /**
         * Invocation of Function.
         */
        dispatcher.accept(new Command("computeCube", 7));
        tool.awaitEquals(dispatcher.dataOut(), 7 * 7 * 7);

        /**
         * Invocation of Non Existent Function.
         */
        dispatcher.accept(new Command("isPrime", 11));
        assertTrue(tool.awaitMessage(dispatcher.dropsOut()).name.equals("isPrime"));
    }
}
