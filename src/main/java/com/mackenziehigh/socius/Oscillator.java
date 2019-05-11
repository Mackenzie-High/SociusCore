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

import com.mackenziehigh.socius.DefaultExecutor;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.ActorFactory;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.Processor;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongFunction;
import com.mackenziehigh.socius.Source;

/**
 * A clock that sends ticks at a variable frequency.
 */
public final class Oscillator
        implements Source<Instant>
{
    /**
     * Effectively, this is the clock (time source) itself.
     */
    private final ScheduledExecutorService service;

    /**
     * This function will provide the delay between successive ticks.
     */
    private final LongFunction<Duration> waveform;

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

    private Oscillator (final Builder builder)
    {
        this.waveform = builder.waveform;
        this.service = builder.service != null ? builder.service : DefaultExecutor.get();
        final ActorFactory stage = Cascade.newStage(service);
        this.procClockOut = Processor.fromIdentityScript(stage);
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
    public Oscillator start ()
    {
        if (started.compareAndSet(false, true))
        {
            service.submit(this::onTick);
        }

        return this;
    }

    private void onTick ()
    {
        final long seqnum = tickCount.getAndIncrement();
        procClockOut.dataIn().send(Instant.now());

        if (stopped.get() == false)
        {
            final Duration delay = waveform.apply(seqnum);
            Objects.requireNonNull(delay, "delay");
            service.schedule(this::onTick, delay.toNanos(), TimeUnit.NANOSECONDS);
        }
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
    public Oscillator stop ()
    {
        stopped.set(true);
        return this;
    }

    /**
     * Factory Method.
     *
     * @return a builder that can be used to create a clock.
     */
    public static Builder newOscillator ()
    {
        return new Builder();
    }

    /**
     * Builder.
     */
    public static final class Builder
    {
        private final Duration oneSecond = Duration.ofSeconds(1);

        private ScheduledExecutorService service;

        private LongFunction<Duration> waveform = x -> oneSecond;

        private Builder ()
        {
            // Pass.
        }

        /**
         * Specify the periodicity of the oscillations.
         *
         * @param waveform is a sine-like function that takes
         * a sequence-number as input and produces a value
         * that is the delay until the next tick.
         * @return this.
         */
        public Builder withWaveform (final LongFunction<Duration> waveform)
        {
            this.waveform = Objects.requireNonNull(waveform, "waveform");
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
        public Oscillator build ()
        {
            return new Oscillator(this);
        }
    }
}
