package com.mackenziehigh.socius.time;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.Processor;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A utility for requesting a wakeup call after a user-specified delay.
 */
public final class WakeupCaller
{
    /**
     * Used to request a wakeup call.
     *
     * @param <T> is the type of message sent during the wakeup call.
     */
    public final class Trigger<T>
    {
        private final Processor<T> procWakeup;

        private final Duration delay;

        private final AtomicBoolean scheduled = new AtomicBoolean();

        private Trigger (final Duration delay)
        {
            this.delay = delay;
            this.procWakeup = Processor.newProcessor(stage, this::onWakeup);
        }

        /**
         * This output will transmit the wakeup call.
         *
         * @return the wakeup-call output.
         */
        public Output<T> wakeupOut ()
        {
            return procWakeup.dataOut();
        }

        /**
         * Request a wakeup call.
         *
         * <p>
         * This method is a no-op, if a wakeup was already requested,
         * but the call has not yet been sent.
         * </p>
         *
         * @param message will be sent during the wakeup call.
         * @return this.
         */
        public Trigger<T> request (final T message)
        {
            if (scheduled.compareAndSet(false, true))
            {
                sender.send(procWakeup.dataIn(), message, delay);
            }
            return this;
        }

        private T onWakeup (final T message)
        {
            scheduled.set(false);
            return message;
        }
    }

    private final Stage stage;

    private final DelayedSender sender;

    private WakeupCaller (final Stage stage,
                          final DelayedSender sender)
    {
        this.stage = stage;
        this.sender = sender;
    }

    /**
     * Determine whether this clock is using the default executor.
     *
     * @return true, if this clock is using the default executor.
     */
    public boolean isUsingDefaultExecutor ()
    {
        return sender.isUsingDefaultExecutor();
    }

    /**
     * Create an object that can be used to schedule wakeup calls.
     *
     * @param <T> is the type of message that will be sent during the wakeup call.
     * @param delay is how long to wait after a wakeup call request before performing the call.
     * @return the new wakeup call trigger.
     */
    public <T> Trigger<T> newTrigger (final Duration delay)
    {
        return new Trigger<>(delay);
    }

    /**
     * Factory Method.
     *
     * @return a new instance of this class.
     */
    public static WakeupCaller newWakeupCaller ()
    {
        return newWakeupCaller(DefaultExecutor.get());
    }

    /**
     * Factory Method.
     *
     * @param service will provide the clock.
     * @return a new instance of this class.
     */
    public static WakeupCaller newWakeupCaller (final ScheduledExecutorService service)
    {
        Objects.requireNonNull(service, "service");
        final Stage stage = Cascade.newExecutorStage(service);
        final DelayedSender sender = DelayedSender.newDelayedSender(service);
        return new WakeupCaller(stage, sender);
    }
}
