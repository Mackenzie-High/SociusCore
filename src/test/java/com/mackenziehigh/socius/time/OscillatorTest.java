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
public final class OscillatorTest
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

        final Oscillator clock = Oscillator
                .newOscillator()
                .withWaveform(x -> Duration.ofMillis(x * 100))
                .build();

        clock.clockOut().connect(sink.dataIn());

        assertEquals(0, clock.tickCount());
        assertTrue(clock.isUsingDefaultExecutor());

        final Instant start = Instant.now();
        clock.start();
        clock.start(); // Duplicate should be ignored.

        Thread.sleep(700);

        clock.stop();
        clock.stop(); // Duplicate should be ignored.

        assertTrue(ticks.size() >= 5);
        assertTrue(clock.tickCount() >= 4);
        final Instant tick0 = ticks.get(0); // delay 0
        final Instant tick1 = ticks.get(1); // delay 100
        final Instant tick2 = ticks.get(2); // delay 200
        final Instant tick3 = ticks.get(3); // delay 300
        final Instant tick4 = ticks.get(4); // delay 400

        assertTrue(Duration.between(start, tick0).toMillis() < 50);
        assertTrue(Duration.between(tick0, tick1).toMillis() <= 50);
        assertTrue(Duration.between(tick1, tick2).toMillis() >= 100);
        assertTrue(Duration.between(tick2, tick3).toMillis() >= 200);
        assertTrue(Duration.between(tick3, tick4).toMillis() >= 300);
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
        assertTrue(Oscillator
                .newOscillator()
                .build()
                .isUsingDefaultExecutor());

        assertFalse(Oscillator
                .newOscillator()
                .poweredBy(Executors.newSingleThreadScheduledExecutor())
                .build()
                .isUsingDefaultExecutor());
    }
}
