package com.mackenziehigh.socius.time;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.Processor;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class Minuteman
{
    private final Timer timer;

    private final AtomicBoolean started = new AtomicBoolean();

    private final AtomicBoolean stopped = new AtomicBoolean();

    private final Processor<Instant> procClockOut;

    private volatile Instant time;

    private Minuteman (final Stage stage)
    {
        this.timer = new Timer(getClass().getSimpleName(), false);
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
    public Minuteman start ()
    {
        if (started.compareAndSet(false, true))
        {
            time = Instant.now();
            schedule();
        }

        return this;
    }

    private void schedule ()
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
                time = time.truncatedTo(ChronoUnit.MINUTES);
                procClockOut.dataIn().send(time);
                this.cancel();
                schedule();
            }
        };

        /**
         * Schedule the task to execute at the top of the next minute.
         */
        time = time.truncatedTo(ChronoUnit.MINUTES).plusSeconds(60);
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
