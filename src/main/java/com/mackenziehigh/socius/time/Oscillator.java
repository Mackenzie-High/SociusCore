package com.mackenziehigh.socius.time;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.Processor;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;

/**
 * A clock that sends ticks at a variable frequency.
 */
public final class Oscillator
{
    private final ScheduledExecutorService service;

    private final LongUnaryOperator waveform;

    private final AtomicBoolean started = new AtomicBoolean();

    private final AtomicBoolean stopped = new AtomicBoolean();

    private final Processor<Instant> procClockOut;

    private final AtomicLong seqnum = new AtomicLong();

    private Oscillator (final Builder builder)
    {
        this.waveform = builder.waveform;

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
        procClockOut.dataIn().send(Instant.now());

        if (stopped.get() == false)
        {
            final long delayNanos = waveform.applyAsLong(seqnum.incrementAndGet());
            service.schedule(this::onTick, delayNanos, TimeUnit.NANOSECONDS);
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

        private ScheduledExecutorService service;

        private LongUnaryOperator waveform = x -> TimeUnit.SECONDS.toNanos(1);

        private Builder ()
        {
            // Pass.
        }

        /**
         * Specify a sine-like function whose absolute-value
         * will determine the periodicity of the oscillations.
         *
         * @param waveform is a sine-like function that takes
         * a sequence-number as input and produces a nanosecond
         * value that is the period until the next tick.
         * @return this.
         */
        public Builder withWaveform (final LongUnaryOperator waveform)
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
            this.service = service;
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
