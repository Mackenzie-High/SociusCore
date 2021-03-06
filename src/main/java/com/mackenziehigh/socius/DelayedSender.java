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

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A wrapper around a <code>ScheduledExecutorService</code> that will
 * send a specified message to an actor after a specified delay.
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
    private DelayedSender (final ScheduledExecutorService service)
    {
        this.service = Objects.requireNonNull(service, "service");
    }

    /**
     * Determine whether this clock is using the default executor.
     *
     * @return true, if this clock is using the default executor.
     */
    public boolean isUsingDefaultExecutor ()
    {
        return service.equals(DefaultExecutor.instance().service());
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
     * <p>
     * This method can be invoked multiple times to schedule multiple transmissions.
     * </p>
     *
     * @param <T> is the type of the message.
     * @param destination is who the message will be sent to.
     * @param message is the message to send.
     * @param delay is how long to wait before sending the message.
     * @return this.
     */
    public <T> DelayedSender send (final Input<? super T> destination,
                                   final T message,
                                   final Duration delay)
    {
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(delay, "delay");

        if (Duration.ZERO.equals(delay))
        {
            destination.send(message);
            return this;
        }
        else
        {
            service.schedule(() -> destination.send(message), delay.toNanos(), TimeUnit.NANOSECONDS);
            return this;
        }
    }

    /**
     * Create a new <code>DelayedSender</code> instance.
     *
     * @param service will provide the clock.
     * @return the new object.
     */
    public static DelayedSender newDelayedSender (final ScheduledExecutorService service)
    {
        return new DelayedSender(service);
    }

    /**
     * Return the global <code>DelayedSender</code> singleton.
     *
     * @return the singleton.
     */
    public static synchronized DelayedSender newDelayedSender ()
    {
        if (global == null)
        {
            final ScheduledExecutorService ses = DefaultExecutor.instance().service();
            global = new DelayedSender(ses);
        }

        return global;
    }
}
