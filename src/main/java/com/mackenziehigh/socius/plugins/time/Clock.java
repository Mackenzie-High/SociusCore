package com.mackenziehigh.socius.plugins.time;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.inception.Kernel.KernelApi;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Basic Clock.
 */
public final class Clock
{
    private final Actor<Instant, Instant> actor;

    private final Output<Instant> clockOut;

    private volatile long periodNanos = TimeUnit.SECONDS.toNanos(1);

    public Clock (final Stage stage)
    {
        this.actor = stage.newActor().withScript((Instant x) -> x).create();
        this.clockOut = actor.output();
    }

    public Clock (final KernelApi kapi)
    {
        this(kapi.stage());
    }

    public Output<Instant> clockOut ()
    {
        return clockOut;
    }

    public Clock period (final Duration value)
    {
        return period(value.toNanos(), TimeUnit.NANOSECONDS);
    }

    public Clock period (final long value)
    {
        return period(value, TimeUnit.MILLISECONDS);
    }

    public Clock period (final long value,
                         final TimeUnit valueUnit)
    {
        periodNanos = valueUnit.toNanos(value);
        return this;
    }

    public Clock start ()
    {
        final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(this::run, 0, periodNanos, TimeUnit.NANOSECONDS);
        return this;
    }

    private void run ()
    {
        actor.input().send(Instant.now());
    }
}
