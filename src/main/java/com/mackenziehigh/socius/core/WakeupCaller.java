package com.mackenziehigh.socius.core;

import com.mackenziehigh.socius.core.DelayedSender;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.core.Processor;
import com.mackenziehigh.socius.core.Printer;
import java.time.Duration;
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
     * Create an object that can be used to send wakeup calls.
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
        return new WakeupCaller(Cascade.newStage(), DelayedSender.global());
    }

    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();
        final Printer p = Printer.newPrintln(stage);
        final WakeupCaller wc = WakeupCaller.newWakeupCaller();

        final Trigger<String> et = wc.newTrigger(Duration.ofSeconds(5));
        et.request("A");
        et.request("B");
        et.request("C");
        et.request("D");
        et.request("E");

        et.wakeupOut().connect(p.dataIn());
    }
}
