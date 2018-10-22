package com.mackenziehigh.socius.time;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.Processor;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A clock that sends ticks at a user-specified periodicity.
 */
public final class Clock
{
    private final ScheduledExecutorService service;

    private final Duration delay;

    private final Duration period;

    private final AtomicBoolean started = new AtomicBoolean();

    private final AtomicBoolean stopped = new AtomicBoolean();

    private final Processor<Instant> procClockOut;

    private volatile Future<?> future;

    private Clock (final Builder builder)
    {
        this.delay = builder.delay;
        this.period = builder.period;

        if (builder.service != null)
        {
            this.service = builder.service;
        }
        else
        {
            this.service = Executors.newSingleThreadScheduledExecutor(); // TODO
        }

        final Stage stage = Cascade.newExecutorStage(service);
        this.procClockOut = Processor.newProcessor(stage);
    }

    /**
     * This output will receive ticks from the clock.
     *
     * @return the clock output.
     */
    public Output<Instant> clockOut ()
    {
        return procClockOut.dataOut();
    }

    /**
     * Call this method to cause the clock to start ticking.
     *
     * <p>
     * This method is a no-op, if the clock was already started.
     * </p>
     *
     * @return this.
     */
    public Clock start ()
    {
        if (started.compareAndSet(false, true))
        {
            future = service.scheduleAtFixedRate(this::onTick,
                                                 delay.toNanos(),
                                                 period.toNanos(),
                                                 TimeUnit.NANOSECONDS);
        }

        return this;
    }

    private void onTick ()
    {
        procClockOut.dataIn().send(Instant.now());
    }

    /**
     * Call this method to cause the clock to stop ticking.
     *
     * <p>
     * This method is a no-op, if the clock was already started.
     * </p>
     *
     * @return this.
     */
    public Clock stop ()
    {
        if (future != null)
        {
            if (stopped.compareAndSet(false, true))
            {
                final boolean interrupt = false;
                future.cancel(interrupt);
            }
        }

        return this;
    }

    /**
     * Factory Method.
     *
     * @return a builder that can be used to create a clock.
     */
    public static Builder newClock ()
    {
        return new Builder();
    }

    /**
     * Builder.
     */
    public static final class Builder
    {

        private ScheduledExecutorService service;

        private Duration delay = Duration.ZERO;

        private Duration period = Duration.ofSeconds(1);

        private Builder ()
        {
            // Pass.
        }

        /**
         * Specify the amount of time that must pass before the first tick.
         *
         * @param delay is the delay before the first tick.
         * @return this.
         */
        public Builder withDelay (final Duration delay)
        {
            this.delay = delay;
            return this;
        }

        /**
         * Specify the amount of time between ticks.
         *
         * @param period is the amount of time between ticks.
         * @return this.
         */
        public Builder withPeriod (final Duration period)
        {
            this.period = period;
            return this;
        }

        /**
         * Specify the executor that powers the clock.
         *
         * @param service will power the clock.
         * @return this.
         */
        public Builder poweredBy (final ScheduledExecutorService service)
        {
            this.service = service;
            return this;
        }

        /**
         * Build the clock.
         *
         * @return the new clock.
         */
        public Clock build ()
        {
            return new Clock(this);
        }
    }
}
