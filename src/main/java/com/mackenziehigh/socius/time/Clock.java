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
package com.mackenziehigh.socius.time;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.ActorFactory;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.DataSource;
import com.mackenziehigh.socius.flow.Processor;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A clock that sends ticks at a user-specified periodicity.
 *
 * <p>
 * A clock sends ticks at a fixed-rate, rather than a fixed-delay.
 * Thus, the time between ticks may occasionally be less than the periodicity.
 * This is particularly true, if the underlying executor is shared.
 * For a more in-depth discussion of fixed-rate versus fixed-delay,
 * see the documentation for the <code>ScheduledExecutorService</code> itself.
 * </p>
 */
public final class Clock
        implements DataSource<Instant>
{
    /**
     * Effectively, this is the clock (time source) itself.
     */
    private final ScheduledExecutorService service;

    /**
     * This is the amount of time to wait before the first tick.
     */
    private final Duration delay;

    /**
     * This is the amount of time to wait between ticks.
     */
    private final Duration period;

    /**
     * This flag will become true, when start() is called.
     */
    private final AtomicBoolean started = new AtomicBoolean();

    /**
     * This flag will become true, when stop() is called.
     */
    private final AtomicBoolean stopped = new AtomicBoolean();

    /**
     * This is the number of clock ticks that have been transmitted.
     */
    private final AtomicLong tickCount = new AtomicLong();

    /**
     * This actor provides the output connector.
     */
    private final Processor<Instant> procClockOut;

    /**
     * This is the next scheduled tick, if any.
     */
    private volatile Future<?> future;

    private Clock (final Builder builder)
    {
        this.delay = builder.delay;
        this.period = builder.period;
        this.service = builder.service != null ? builder.service : DefaultExecutor.get();
        final ActorFactory stage = Cascade.newStage(service);
        this.procClockOut = Processor.newConnector(stage);
    }

    /**
     * Get the delay between the start of the clock and the first tick.
     *
     * @return the initial delay.
     */
    public Duration delay ()
    {
        return delay;
    }

    /**
     * get the amount of time between successive ticks.
     *
     * @return the periodicity of the clock.
     */
    public Duration period ()
    {
        return period;
    }

    /**
     * Get the number of ticks that have been transmitted.
     *
     * @return the number of ticks, thus far.
     */
    public long tickCount ()
    {
        return tickCount.get();
    }

    /**
     * Determine whether this clock is using the default executor.
     *
     * @return true, if this clock is using the default executor.
     */
    public boolean isUsingDefaultExecutor ()
    {
        return service.equals(DefaultExecutor.get());
    }

    /**
     * This output will transmit ticks from the clock.
     *
     * @return the clock output.
     */
    @Override
    public Output<Instant> dataOut ()
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
        tickCount.incrementAndGet();
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
        if (stopped.compareAndSet(false, true))
        {
            if (future != null)
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
            this.delay = Objects.requireNonNull(delay, "delay");
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
            this.period = Objects.requireNonNull(period, "period");
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
            this.service = Objects.requireNonNull(service, "service");
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
