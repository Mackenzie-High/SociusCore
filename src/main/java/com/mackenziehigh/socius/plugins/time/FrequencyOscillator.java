package com.mackenziehigh.socius.plugins.time;

import com.google.common.base.Preconditions;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.plugins.flow.Processor;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntUnaryOperator;

/**
 *
 */
public final class FrequencyOscillator
{
    private final Processor<Instant> clockOut;

    private final ScheduledExecutorService service;

    private final int minimum;

    private final int maximum;

    private final IntUnaryOperator period;

    private final AtomicBoolean started = new AtomicBoolean();

    private FrequencyOscillator (final ScheduledExecutorService service,
                                 final IntUnaryOperator period,
                                 final int minimum,
                                 final int maximum)
    {
        Preconditions.checkArgument(minimum <= maximum, "minimum > maximum");
        this.service = Objects.requireNonNull(service, "service");
        this.period = Objects.requireNonNull(period, "period");
        this.minimum = minimum;
        this.maximum = maximum;
        final Stage stage = Cascade.newExecutorStage(service);
        this.clockOut = Processor.newProcessor(stage);
    }

    private void onTick (final int seqnum)
    {
        final int delay = period.applyAsInt(seqnum);

        service.schedule(() -> onTick(seqnum + 1), delay, TimeUnit.NANOSECONDS);
    }

    public FrequencyOscillator start ()
    {
        if (started.compareAndSet(false, true))
        {
            service.submit(() -> onTick(minimum));
        }

        return this;
    }

    public FrequencyOscillator stop ()
    {
        return this;
    }

    public Output<Instant> clockOut ()
    {
        return clockOut.dataOut();
    }

    public static FrequencyOscillator create (final ScheduledExecutorService service,
                                              final IntUnaryOperator period,
                                              final int minimum,
                                              final int maximum)
    {
        return new FrequencyOscillator(service, period, minimum, maximum);
    }

    public static void main (String[] args)
    {
        final ScheduledExecutorService s = Executors.newScheduledThreadPool(4);
        final Stage stage = Cascade.newExecutorStage(s);

    }
}
