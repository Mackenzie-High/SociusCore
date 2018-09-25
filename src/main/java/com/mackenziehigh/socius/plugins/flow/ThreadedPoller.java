package com.mackenziehigh.socius.plugins.flow;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.plugins.io.Printer;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public final class ThreadedPoller<T>
        implements AutoCloseable
{

    @FunctionalInterface
    public interface PollerScript<T>
    {
        public T poll (final Duration timeout)
                throws Throwable;
    }

    private final Actor<T, T> dataOut;

    private final Duration timeout;

    private final PollerScript<T> script;

    private final Thread thread;

    private final AtomicBoolean stop = new AtomicBoolean();

    private ThreadedPoller (final Stage stage,
                            final Duration timeout,
                            final PollerScript<T> script)
    {
        Objects.requireNonNull(stage, "stage");
        this.dataOut = stage.newActor().withScript((T x) -> x).create();
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.script = Objects.requireNonNull(script, "script");
        this.thread = new Thread(this::run);
    }

    private void run ()
    {
        while (stop.get() == false)
        {
            try
            {
                final T result = script.poll(timeout);

                if (result != null)
                {
                    dataOut.accept(result);
                }
            }
            catch (Throwable ex)
            {
                // TODO: Replace with logger.
                ex.printStackTrace(System.err);
            }
        }
    }

    @Override
    public void close ()
    {
        stop.set(true);
    }

    public Output<T> dataOut ()
    {
        return dataOut.output();
    }

    public static <T> ThreadedPoller<T> newPoller (final Stage stage,
                                                   final Duration timeout,
                                                   final PollerScript<T> script)
    {
        final ThreadedPoller<T> poller = new ThreadedPoller<>(stage, timeout, script);
        poller.thread.start();
        return poller;
    }

    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();
        final ThreadedPoller<String> poller = ThreadedPoller.newPoller(stage, Duration.ZERO, x -> Instant.now().toString());
        final Printer p = new Printer(stage);
        poller.dataOut().connect(p.dataIn());

    }
}
