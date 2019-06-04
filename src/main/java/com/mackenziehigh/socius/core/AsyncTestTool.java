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
package com.mackenziehigh.socius.core;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

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

    private volatile long timeoutMillis = TimeUnit.SECONDS.toMillis(1);

    private final AtomicLong pending = new AtomicLong();

    public AsyncTestTool ()
    {
        final ThreadFactory factory = (Runnable task) ->
        {
            final Thread thread = new Thread(task);
            thread.setName(AsyncTestTool.class.getName());
            thread.setDaemon(true);
            return thread;
        };

        final ExecutorService service = new ThreadPoolExecutor(0, 16, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), factory);

        this.stage = new Cascade.AbstractStage()
        {
            @Override
            protected void onSubmit (final DefaultActor<?, ?> delegate)
            {
                final Runnable task = () ->
                {
                    try
                    {
                        delegate.run();
                    }
                    finally
                    {
                        pending.decrementAndGet();
                    }
                };

                pending.incrementAndGet();
                service.execute(task);
            }

            @Override
            protected void onClose ()
            {
                // Pass.
            }
        };
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
     * Set the maximum amount of time that <code>await()</code> will wait.
     *
     * @param timeoutMillis is the maximum wait time.
     * @return this.
     */
    public AsyncTestTool setTimeout (final long timeoutMillis)
    {
        this.timeoutMillis = timeoutMillis;
        return this;
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

    public <T> void await (final BooleanSupplier condition)
    {
        for (int i = 0; i < timeoutMillis && !condition.getAsBoolean(); i++)
        {
            sleep(1);
        }

        if (condition.getAsBoolean() == false)
        {
            throw new IllegalStateException("The condition never became true.");
        }
    }

    public void awaitEquilibrium ()
    {
        await(() -> pending.get() > 0);
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

                final Object result = queue.poll(timeoutMillis, TimeUnit.MILLISECONDS);

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
}
