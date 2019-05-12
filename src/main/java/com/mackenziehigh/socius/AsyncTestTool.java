package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * (Experimental API) Provides a mechanism for testing actors in unit-tests.
 *
 * <p>
 * This API is still semi-unstable and may change without notice.
 * Refactoring, etc, may be needed in the future, if you use this API.
 * </p>
 */
public final class AsyncTestTool
        implements Closeable
{
    private final Stage stage;

    private final ConcurrentMap<Output<?>, BlockingQueue<Object>> connections = new ConcurrentHashMap<>();

    public AsyncTestTool ()
    {
        final ThreadFactory factory = (Runnable task) ->
        {
            final Thread thread = new Thread(task);
            thread.setName(AsyncTestTool.class.getName());
            thread.setDaemon(true);
            return thread;
        };

        this.stage = Cascade.newStage(Executors.newCachedThreadPool(factory));
    }

    /**
     * Get the stage that powers this tester.
     *
     * @return the underlying stage.
     */
    public Stage stage ()
    {
        return stage;
    }

    /**
     * Connect an output to the tester.
     *
     * @param <T> is the type of messages provided by the output.
     * @param output will be connected hereto.
     */
    public synchronized <T> void connect (final Output<T> output)
    {
        Objects.requireNonNull(output, "output");

        if (connections.containsKey(output) == false)
        {
            final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
            final Processor<T> proc = Processor.fromConsumerScript(stage, x -> queue.add(x));
            output.connect(proc.dataIn());
            connections.put(output, queue);
        }
    }

    /**
     * Wait for the given output to produce a message.
     *
     * @param output will produce a message at some point soon.
     * @return the message that was produced.
     */
    public Object await (final Output<?> output)
    {
        Objects.requireNonNull(output, "output");

        if (connections.containsKey(output))
        {
            try
            {
                final BlockingQueue<Object> queue = connections.get(output);

                final Object result = queue.poll(1, TimeUnit.SECONDS);

                if (result == null)
                {
                    throw new IllegalStateException("Timeout Expired");
                }
                else
                {
                    return result;
                }
            }
            catch (InterruptedException ex)
            {
                throw new RuntimeException("Interrupted", ex);
            }
        }
        else
        {
            throw new IllegalStateException("Not Connected to Output");
        }
    }

    /**
     * Wait for the given output to produce the expected message.
     *
     * @param <T> is the type of the message that the output will produce.
     * @param output will produce a message at some point soon.
     * @param expected is the message that the output will produce.
     * @throws IllegalStateException if an unexpected message is produced.
     */
    public <T> void expect (final Output<T> output,
                            final T expected)
    {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(expected, "expected");

        final Object actual = await(output);

        if (Objects.equals(expected, actual) == false)
        {
            throw new IllegalStateException(String.format("Expect: %s, Actual: %s", expected, actual));
        }
    }

    /**
     * Cause the current thread to sleep for the given number of milliseconds.
     *
     * @param millis is how long to sleep.
     */
    public void sleep (final long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close ()
    {
        stage.close();
    }

    public static void main (String[] args)
    {
        final AsyncTestTool tester = new AsyncTestTool();

        final Processor<String> p = Processor.fromIdentityScript(tester.stage());

        tester.connect(p.dataOut());
        p.accept("A");
        p.accept("B");

        System.out.println("X = " + tester.await(p.dataOut()));
        System.out.println("X = " + tester.await(p.dataOut()));
    }

}
