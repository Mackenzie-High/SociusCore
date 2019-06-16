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

import java.time.Duration;
import java.util.concurrent.Executors;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class DelayedSenderTest
{
    private final AsyncTestTool tester = new AsyncTestTool();

    /**
     * Case: Throughput when Delay is Zero.
     *
     * @throws InterruptedException
     */
    @Test
    public void test ()
            throws InterruptedException
    {
        final Processor<String> sink = Processor.fromIdentityScript(tester.stage());
        tester.connect(sink.dataOut());

        final DelayedSender clock = DelayedSender.newDelayedSender();

        clock.send(sink.dataIn(), "X", Duration.ofMillis(300));
        clock.send(sink.dataIn(), "Y", Duration.ofMillis(100));
        clock.send(sink.dataIn(), "Z", Duration.ofMillis(200));

        tester.awaitEquals(sink.dataOut(), "Y"); // First, due to shortest delay.
        tester.awaitEquals(sink.dataOut(), "Z");
        tester.awaitEquals(sink.dataOut(), "X"); // Last, due to longest delay.
        tester.assertEmptyOutputs();
    }

    /**
     * Test: 20190615222213559604
     *
     * <p>
     * Method: <code>send</code>
     * </p>
     *
     * <p>
     * Case: Throughput when Delay is Non-Zero.
     * </p>
     */
    @Test
    public void test20190615222213559604 ()
    {
        final Processor<String> sink = Processor.fromIdentityScript(tester.stage());
        tester.connect(sink.dataOut());

        final DelayedSender clock = DelayedSender.newDelayedSender();

        clock.send(sink.dataIn(), "X", Duration.ZERO);
        clock.send(sink.dataIn(), "Y", Duration.ZERO);
        clock.send(sink.dataIn(), "Z", Duration.ZERO);

        tester.awaitEquals(sink.dataOut(), "X");
        tester.awaitEquals(sink.dataOut(), "Y");
        tester.awaitEquals(sink.dataOut(), "Z");
        tester.assertEmptyOutputs();
    }

    /**
     * Test: 20181125020004923394
     *
     * <p>
     * Case: Different Executors.
     * </p>
     */
    @Test
    public void test20181125020004923394 ()
    {
        assertTrue(DelayedSender
                .newDelayedSender()
                .isUsingDefaultExecutor());

        assertFalse(DelayedSender
                .newDelayedSender(Executors.newSingleThreadScheduledExecutor())
                .isUsingDefaultExecutor());
    }
}
