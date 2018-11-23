package com.mackenziehigh.socius.core;

import com.google.common.util.concurrent.RateLimiter;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Objects;

/**
 * Throttles messages to a pre-specified Hertz rate.
 *
 * @param <T> is the type of the incoming and outgoing messages.
 */
public final class Throttler<T>
{
    private final Actor<T, T> dataIn;

    private final Actor<T, T> dataOut;

    private final Actor<T, T> overflowOut;

    private final RateLimiter limiter;

    private Throttler (final Stage stage,
                       final double hertz)
    {
        Objects.requireNonNull(stage, "stage");
        this.limiter = RateLimiter.create(hertz);
        this.dataIn = stage.newActor().withScript(this::onMessage).create();
        this.dataOut = stage.newActor().withScript((T x) -> x).create();
        this.overflowOut = stage.newActor().withScript((T x) -> x).create();
    }

    private void onMessage (final T message)
    {
        if (limiter.tryAcquire())
        {
            dataOut.accept(message);
        }
        else
        {
            overflowOut.accept(message);
        }
    }

    /**
     *
     *
     * @return
     */
    public Input<T> dataIn ()
    {
        return dataIn.input();
    }

    public Output<T> dataOut ()
    {
        return dataOut.output();
    }

    public Output<T> overflowOut ()
    {
        return overflowOut.output();
    }

    public static <T> Throttler<T> newThrottler (final Stage stage,
                                                 final double hertz)
    {
        return new Throttler<>(stage, hertz);
    }
}
