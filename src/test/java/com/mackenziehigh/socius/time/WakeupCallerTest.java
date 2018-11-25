package com.mackenziehigh.socius.time;

import com.google.common.collect.Lists;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.socius.io.CollectionSink;
import com.mackenziehigh.socius.time.WakeupCaller.Trigger;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class WakeupCallerTest
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

        final WakeupCaller clock = WakeupCaller.newWakeupCaller();
        final Trigger<String> trigger1 = clock.newTrigger(Duration.ofMillis(250));
        final Trigger<String> trigger2 = clock.newTrigger(Duration.ofMillis(500));

        trigger1.wakeupOut().connect(sink.dataIn());
        trigger2.wakeupOut().connect(sink.dataIn());

        /**
         * Request a wakeup call in (250 milliseconds).
         * Only the first one should be sent, since the last two
         * are (likely) scheduled before the call was sent.
         */
        trigger1.request("A");
        trigger1.request("B");
        trigger1.request("C");

        /**
         * Request a wakeup call in (500 milliseconds).
         * Only the first one should be sent, since the last two
         * are (likely) scheduled before the call was sent.
         */
        trigger2.request("D");
        trigger2.request("E");
        trigger2.request("F");

        Thread.sleep(700);

        assertTrue(ticks.size() == 2);

        assertEquals("A", ticks.get(0)); // First, due to shortest delay.
        assertEquals("D", ticks.get(1)); // Last, due to longest delay.
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
        assertTrue(WakeupCaller
                .newWakeupCaller()
                .isUsingDefaultExecutor());

        assertFalse(WakeupCaller
                .newWakeupCaller(Executors.newSingleThreadScheduledExecutor())
                .isUsingDefaultExecutor());
    }
}
