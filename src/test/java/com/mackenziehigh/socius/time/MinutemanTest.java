package com.mackenziehigh.socius.time;

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
