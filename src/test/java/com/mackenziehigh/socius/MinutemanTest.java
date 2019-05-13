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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class MinutemanTest
{
    /**
     * Case: Basics.
     *
     * <p>
     * Since a tick only occurs once per minute,
     * this test does not validate that ticks are sent.
     * This is a hole in the test.
     * </p>
     *
     * @throws InterruptedException
     */
    @Test
    public void test ()
            throws InterruptedException
    {
        final Stage stage = Cascade.newStage();

        final Minuteman clock = Minuteman.newMinuteman(stage);

        assertFalse(clock.scheduledTick().isPresent());

        clock.start();
        clock.start(); // Duplicate should be ignored.

        assertEquals(Instant.now().truncatedTo(ChronoUnit.MINUTES).plusSeconds(60), clock.scheduledTick().get());

        clock.stop();
        clock.stop(); // Duplicate should be ignored.
    }
}
