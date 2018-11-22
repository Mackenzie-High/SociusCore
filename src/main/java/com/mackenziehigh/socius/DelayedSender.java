package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A wrapper around a <code>ScheduledExecutorService</code> that will send
 * a specified message to an <code>Actor</code> after a specified delay.
 */
public final class DelayedSender
{
    private static DelayedSender global = null;

    private final ScheduledExecutorService service;

    /**
     * Constructor.
     *
     * @param service will be used to perform the scheduled sends.
     */
    public DelayedSender (final ScheduledExecutorService service)
    {
        this.service = Objects.requireNonNull(service, "service");
    }

    /**
     * Schedule sending a message.
     *
     * <p>
     * If the destination is unable to receive the message due
     * to capacity restrictions, when the send is actually performed,
     * then the message will be silently dropped.
     * </p>
     *
     * @param <T> is the type of the message.
     * @param destination is who the message will be sent to.
     * @param message is the message to send.
     * @param delay is how long to wait before sending the message.
     * @return this.
     */
    public <T> DelayedSender send (final Actor.Input<? super T> destination,
                                   final T message,
                                   final Duration delay)
    {
        service.schedule(() -> destination.send(message), delay.toNanos(), TimeUnit.NANOSECONDS);
        return this;
    }

    /**
     * Return the global <code>DelayedSender</code> singleton.
     *
     * <p>
     * The first call to this method will create the instance
     * and the related <code>ScheduledExecutorService</code>.
     * </p>
     *
     * @return the singleton.
     */
    public static synchronized DelayedSender global ()
    {
        if (global == null)
        {
            final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
            final Thread hook = new Thread(() -> ses.shutdown());
            Runtime.getRuntime().addShutdownHook(hook);
            global = new DelayedSender(ses);
        }

        return global;
    }
}
