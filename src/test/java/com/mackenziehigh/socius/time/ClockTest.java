package com.mackenziehigh.socius.time;

import com.google.common.collect.Lists;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.io.CollectionSink;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class ClockTest
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
        final Stage stage = Cascade.newStage();
        final List<Instant> ticks = Lists.newCopyOnWriteArrayList();
        final CollectionSink<Instant> sink = CollectionSink.newCollectionSink(stage, ticks);

        final Clock clock = Clock
                .newClock()
                .withDelay(Duration.ofMillis(100))
                .withPeriod(Duration.ofMillis(200))
                .build();

        clock.clockOut().connect(sink.dataIn());

        assertEquals(100, clock.delay().toMillis());
        assertEquals(200, clock.period().toMillis());
        assertEquals(0, clock.tickCount());
        assertTrue(clock.isUsingDefaultExecutor());

        final Instant start = Instant.now();
        clock.start();
        clock.start(); // Duplicate should be ignored.

        Thread.sleep(600);

        clock.stop();
        clock.stop(); // Duplicate should be ignored.

        assertTrue(ticks.size() >= 2);
        assertTrue(clock.tickCount() >= 2);
        final Instant tick1 = ticks.get(0);
        final Instant tick2 = ticks.get(1);

        assertTrue(Duration.between(start, tick1).toMillis() >= clock.delay().toMillis());

        /**
         * The amount of time between ticks may vary (fixed rate vs fixed delay).
         * However, I would not expect a difference of more that 10% (bad assumption?).
         */
        assertTrue(Duration.between(tick1, tick2).toMillis() >= clock.period().toMillis() - (0.1 * clock.period().toMillis()));
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
        assertTrue(Clock
                .newClock()
                .build()
                .isUsingDefaultExecutor());

        assertFalse(Clock
                .newClock()
                .poweredBy(Executors.newSingleThreadScheduledExecutor())
                .build()
                .isUsingDefaultExecutor());
    }
}
