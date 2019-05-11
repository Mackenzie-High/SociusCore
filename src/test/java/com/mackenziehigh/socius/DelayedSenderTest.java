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

import com.mackenziehigh.socius.DelayedSender;
import com.google.common.collect.Lists;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.socius.CollectionSink;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class DelayedSenderTest
{
    /**
     * Case: Throughput.
     *
     * @throws InterruptedException
     */
    @Test
    public void test ()
            throws InterruptedException
    {
        final Cascade.Stage stage = Cascade.newStage();
        final List<String> ticks = Lists.newCopyOnWriteArrayList();
        final CollectionSink<String> sink = CollectionSink.newCollectionSink(stage, ticks);

        final DelayedSender clock = DelayedSender.newDelayedSender();

        clock.send(sink.dataIn(), "X", Duration.ofMillis(300));
        clock.send(sink.dataIn(), "Y", Duration.ofMillis(100));
        clock.send(sink.dataIn(), "Z", Duration.ofMillis(200));

        Thread.sleep(400);

        assertTrue(ticks.size() == 3);

        assertEquals("Y", ticks.get(0)); // First, due to shortest delay.
        assertEquals("Z", ticks.get(1));
        assertEquals("X", ticks.get(2)); // Last, due to longest delay.
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
