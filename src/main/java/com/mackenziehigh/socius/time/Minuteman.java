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
package com.mackenziehigh.socius.time;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.DataSource;
import com.mackenziehigh.socius.flow.Processor;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A clock that sends a tick once per minute,
 * as close to the top of the minute as possible.
 */
public final class Minuteman
        implements DataSource<Instant>
{
    /**
     * Effectively, this is the clock (time source) itself.
     */
    private final Timer timer;

    /**
     * This flag will become true, when start() is called.
     */
    private final AtomicBoolean started = new AtomicBoolean();

    /**
     * This flag will become true, when stop() is called.
     */
    private final AtomicBoolean stopped = new AtomicBoolean();

    /**
     * This actor provides the output connector.
     */
    private final Processor<Instant> procClockOut;

    /**
     * This is when the clock is scheduled to tick next.
     */
    private volatile Instant time;

    private Minuteman (final Stage stage)
    {
        Objects.requireNonNull(stage, "stage");
        this.timer = new Timer(getClass().getSimpleName(), true);
        this.procClockOut = Processor.newConnector(stage);
    }

    /**
     * Get the next time that the clock is scheduled to tick.
     *
     * <p>
     * In effect, this method returns the current minute rounded up.
     * </p>
     *
     * <p>
     * Empty is returned, if the clock is not running.
     * </p>
     *
     * @return the upcoming tick.
     */
    public Optional<Instant> scheduledTick ()
    {
        return Optional.ofNullable(time);
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
    public Minuteman start ()
    {
        if (started.compareAndSet(false, true))
        {
            schedule(Instant.now());
        }

        return this;
    }

    private void schedule (final Instant now)
    {
        /**
         * This particular task object will only execute once.
         * When it executes, it will cancel itself, and then create
         * a new task object that will execute a minute later.
         */
        final TimerTask task = new TimerTask()
        {
            @Override
            public void run ()
            {
                final Instant now = time.truncatedTo(ChronoUnit.MINUTES);
                procClockOut.dataIn().send(now);
                cancel();
                schedule(now);
            }
        };

        /**
         * Schedule the task to execute at the top of the next minute.
         */
        time = now.truncatedTo(ChronoUnit.MINUTES).plusSeconds(60);
        timer.schedule(task, Date.from(time));
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
    public Minuteman stop ()
    {
        stopped.set(true);
        return this;
    }

    /**
     * Factory Method.
     *
     * @param stage will be used to create private actors.
     * @return a builder that can be used to create a clock.
     */
    public static Minuteman newMinuteman (final Stage stage)
    {
        return new Minuteman(stage);
    }
}
