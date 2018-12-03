package com.mackenziehigh.socius.flow;

import com.google.common.util.concurrent.RateLimiter;
import com.mackenziehigh.cascade.Cascade.Stage;
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
    /**
     * Provides the data-input connector.
     */
    private final Processor<T> procDataIn;

    /**
     * Provides the data-output connector.
     */
    private final Processor<T> procDataOut;

    /**
     * Provides the overflow-output connector.
     */
    private final Processor<T> procOverflowOut;

    /**
     * This objects decides whether a message should be dropped or forwarded.
     * This object is not thread-safe, so it can only be touched by the actor.
     */
    private final RateLimiter limiter;

    private Throttler (final Stage stage,
                       final double hertz)
    {
        Objects.requireNonNull(stage, "stage");
        this.limiter = RateLimiter.create(hertz);
        this.procDataIn = Processor.newConsumer(stage, this::onMessage);
        this.procDataOut = Processor.newConnector(stage);
        this.procOverflowOut = Processor.newConnector(stage);
    }

    private void onMessage (final T message)
    {
        if (limiter.tryAcquire())
        {
            // Forward the message.
            procDataOut.accept(message);
        }
        else
        {
            // Drop the message.
            procOverflowOut.accept(message);
        }
    }

    /**
     * Input Connection.
     *
     * @return the input that provides the messages to throttle.
     */
    public Input<T> dataIn ()
    {
        return procDataIn.dataIn();
    }

    /**
     * Output Connection.
     *
     * <p>
     * This output will receive the messages that were <b>not</b> dropped.
     * </>
     *
     * @return the output that will receive the throttled messages.
     */
    public Output<T> dataOut ()
    {
        return procDataOut.dataOut();
    }

    /**
     * Output Connection.
     *
     * <p>
     * This output will receive the messages that <b>were</b> dropped.
     * </>
     *
     * @return the output that will receive the dropped messages.
     */
    public Output<T> overflowOut ()
    {
        return procOverflowOut.dataOut();
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @param hertz will be the maximum rate at which messages will be forwarded.
     * @return the new throttler.
     */
    public static <T> Throttler<T> newThrottler (final Stage stage,
                                                 final double hertz)
    {
        return new Throttler<>(stage, hertz);
    }
}
