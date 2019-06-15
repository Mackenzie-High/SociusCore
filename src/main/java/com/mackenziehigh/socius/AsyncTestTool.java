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

import com.mackenziehigh.cascade.Cascade.AbstractStage;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.io.Closeable;
import java.time.Duration;
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
 * This method throws unchecked <code>AwaitInterruptedException</code>s,
 * rather than propagating checked <code>InterruptedException</code>.
 * In general, interruption
 * </p>
 */
public final class AsyncTestTool
        implements Closeable
{

    /**
     * Indicates that an <code>await</code> method timed out.
     */
    public final class AwaitTimeoutException
            extends RuntimeException
    {
        // Pass.
    }

    /**
     * Indicates that an <code>await</code> method was interrupted.
     */
    public final class AwaitInterruptedException
            extends RuntimeException
    {
        // Pass.
    }

    /**
     * Indicates that this test tool is not connected to a necessary <code>Output</code>.
     */
    public final class NoConnectionException
            extends RuntimeException
    {
        // Pass.
    }

    /**
     * Indicates that an expected result was not produced during a test.
     */
    public final class ExpectationFailedException
            extends RuntimeException
    {
        private final Object expected;

        private final Object actual;

        public ExpectationFailedException (final Object expected,
                                           final Object actual)
        {
            this.expected = expected;
            this.actual = actual;
        }

        public Object expected ()
        {
            return expected;
        }

        public Object actual ()
        {
            return actual;
        }

        @Override
        public String toString ()
        {
            return String.format("Expected: %s, Actual: %s", expected(), actual());
        }
    }

    /**
     * The stage used herein will use daemon threads that are named after this class.
     */
    private final ThreadFactory factory = (Runnable task) ->
    {
        final Thread thread = new Thread(task);
        thread.setName(AsyncTestTool.class.getSimpleName());
        thread.setDaemon(true);
        return thread;
    };

    /**
     * This service provides the threads that power the stage.
     * The stage will use up to sixteen threads at a time.
     * The threads will be allowed to age off automatically;
     * therefore, we will not leak threads, if someone fails
     * to close this tester when they are done using it.
     */
    private final ExecutorService service = new ThreadPoolExecutor(0, 16, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), factory);

    /**
     * This is the number of actors that are currently scheduled to execute.
     * This is *not* the number of messages remaining to be processed.
     * When this count is zero, the stage is in a steady state
     * with no actors executing at that moment in time.
     */
    private final AtomicLong pending = new AtomicLong();

    /**
     * This stage can be used to power actors under test.
     */
    private final Stage stage = new AbstractStage()
    {
        @Override
        protected void onRunnable (final DefaultActor<?, ?> delegate)
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

            /**
             * The actor is being scheduled for execution,
             * so keep a count of it, so that we can detect
             * when the stage is in a state of equilibrium.
             */
            pending.incrementAndGet();

            /**
             * Schedule the actor to execute.
             */
            service.execute(task);
        }

        @Override
        protected void onClose ()
        {
            service.shutdown();
        }
    };

    /**
     * This map maps outputs that are being monitored to the queues that
     * will be asynchronously filled with the messages from those outputs.
     */
    private final ConcurrentMap<Output<?>, BlockingQueue<Object>> connections = new ConcurrentHashMap<>();

    /**
     * This is the approximate maximum amount of time that an <code>await</code> method will wait.
     */
    private volatile Duration timeout = Duration.ofSeconds(1);

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
     * Set the maximum amount of time that the <code>await</code> methods will wait.
     *
     * @param timeout is the maximum wait time.
     * @return this.
     */
    public AsyncTestTool setAwaitTimeout (final Duration timeout)
    {
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        return this;
    }

    /**
     * Connect an output to the tester.
     *
     * @param <T> is the type of messages provided by the output.
     * @param output will be connected hereto.
     */
    public <T> void connect (final Output<T> output)
    {
        Objects.requireNonNull(output, "output");

        final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();

        if (connections.putIfAbsent(output, queue) == null)
        {
            final Processor<T> proc = Processor.fromConsumerScript(stage, x -> queue.add(x));
            output.connect(proc.dataIn());
        }
    }

    /**
     * Wait for the given boolean condition to become true,
     * or for the await-timeout to expire.
     *
     * @param condition will become true at some point.
     */
    public void awaitTrue (final BooleanSupplier condition)
    {
        Objects.requireNonNull(condition, "condition");

        final Duration millis = Duration.ofMillis(1);

        /**
         * Wait for the timeout to expire or the condition to become true.
         */
        final long start = System.nanoTime();
        long elapsed = 0;
        while (elapsed < timeout.toNanos() && !condition.getAsBoolean())
        {
            sleep(millis);
            elapsed = System.nanoTime() - start;
        }

        /**
         * If the condition did not become true,
         * then throw an indicative exception.
         */
        if (condition.getAsBoolean() == false)
        {
            throw new AwaitTimeoutException();
        }
    }

    /**
     * Wait for the given boolean condition to become false,
     * or for the await-timeout to expire.
     *
     * @param condition will become false at some point.
     */
    public void awaitFalse (final BooleanSupplier condition)
    {
        Objects.requireNonNull(condition, "condition");
        awaitTrue(() -> !condition.getAsBoolean());
    }

    /**
     * Wait for the stage to reach a point in which no actors are
     * being executed thereon, or for the await-timeout to expire.
     */
    public void awaitSteadyState ()
    {
        awaitTrue(() -> pending.get() == 0);
    }

    /**
     * Wait for the given output to produce a message.
     *
     * @param <T> is the type of the expected message.
     * @param output will produce a message at some point soon.
     * @return the message that was produced.
     */
    @SuppressWarnings ("unchecked")
    public <T> T awaitMessage (final Output<T> output)
    {
        Objects.requireNonNull(output, "output");

        if (connections.containsKey(output))
        {
            try
            {
                /**
                 * Find the queue that receives messages from the given output.
                 */
                final BlockingQueue<Object> queue = connections.get(output);

                /**
                 * Wait for a message to become available, if any.
                 */
                final T result = (T) queue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);

                if (result == null)
                {
                    /**
                     * The timeout expired before a message became available;
                     * therefore, throw an indicative exception.
                     */
                    throw new AwaitTimeoutException();
                }
                else
                {
                    return result;
                }
            }
            catch (InterruptedException ex)
            {
                throw new AwaitInterruptedException();
            }
        }
        else
        {
            /**
             * We cannot wait for messages from outputs that are not connected;
             * therefore, throw an indicative exception.
             */
            throw new NoConnectionException();
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
    public <T> void awaitEquals (final Output<T> output,
                                 final T expected)
    {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(expected, "expected");

        final Object actual = awaitMessage(output);

        if (Objects.equals(expected, actual) == false)
        {
            throw new ExpectationFailedException(expected, actual);
        }
    }

    /**
     * Cause the current thread to sleep for the given number of milliseconds.
     *
     * @param period is how long to sleep.
     */
    public void sleep (final Duration period)
    {
        Objects.requireNonNull(period, "period");

        try
        {
            Thread.sleep(period.toMillis());
        }
        catch (InterruptedException ex)
        {
            throw new AwaitInterruptedException();
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
