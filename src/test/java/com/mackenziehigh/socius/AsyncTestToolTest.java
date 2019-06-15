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

import com.mackenziehigh.socius.Processor;
import com.mackenziehigh.socius.AsyncTestTool;
import com.mackenziehigh.cascade.Cascade.Stage;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class AsyncTestToolTest
{
    private final AsyncTestTool tool = new AsyncTestTool();

    private final ExecutorService service = Executors.newFixedThreadPool(5);

    @Before
    public void setup ()
    {
        tool.setAwaitTimeout(Duration.ofSeconds(1));
    }

    @After
    public void destroy ()
    {
        tool.close();
        service.shutdown();
    }

    /**
     * Test: 20190614203444597385
     *
     * <p>
     * Method: <code>awaitTrue</code>
     * </p>
     *
     * <p>
     * Case: No Timeout Occurs.
     * </p>
     */
    @Test
    public void test20190614203444597385 ()
    {
        final AtomicInteger counter = new AtomicInteger(5);

        final BooleanSupplier condition = () ->
        {
            return counter.decrementAndGet() <= 0;
        };

        assertEquals(5, counter.get());
        tool.awaitTrue(condition);
        assertTrue(counter.get() <= 0);
    }

    /**
     * Test: 20190614211950560863
     *
     * <p>
     * Method: <code>awaitTrue</code>
     * </p>
     *
     * <p>
     * Case: Timeout.
     * </p>
     */
    @Test (expected = AsyncTestTool.AwaitTimeoutException.class)
    public void test20190614211950560863 ()
    {
        tool.setAwaitTimeout(Duration.ZERO);
        tool.awaitTrue(() -> false);
    }

    /**
     * Test: 20190614203444597436
     *
     * <p>
     * Method: <code>awaitFalse</code>
     * </p>
     *
     * <p>
     * Case: No Timeout Occurs.
     * </p>
     */
    @Test
    public void test20190614203444597436 ()
    {
        final AtomicInteger counter = new AtomicInteger(5);

        final BooleanSupplier condition = () ->
        {
            return counter.decrementAndGet() > 0;
        };

        assertEquals(5, counter.get());
        tool.awaitFalse(condition);
        assertTrue(counter.get() <= 0);
    }

    /**
     * Test: 20190614213514045194
     *
     * <p>
     * Method: <code>awaitFalse</code>
     * </p>
     *
     * <p>
     * Case: Timeout.
     * </p>
     */
    @Test (expected = AsyncTestTool.AwaitTimeoutException.class)
    public void test20190614213514045194 ()
    {
        tool.setAwaitTimeout(Duration.ZERO);
        tool.awaitFalse(() -> true);
    }

    /**
     * Test: 20190614203444597455
     *
     * <p>
     * Method: <code>awaitEquals</code>
     * </p>
     *
     * <p>
     * Case: No Timeout Occurs, Expected Value Received.
     * </p>
     */
    @Test
    public void test20190614203444597455 ()
    {
        final Stage stage = tool.stage();
        final Processor<String> actor = Processor.fromIdentityScript(stage);

        tool.connect(actor.dataOut());
        actor.accept("X");
        tool.awaitEquals(actor.dataOut(), "X");
    }

    /**
     * Test: 20190614220911104541
     *
     * <p>
     * Method: <code>awaitEquals</code>
     * </p>
     *
     * <p>
     * Case: No Timeout Occurs, Unexpected Value Received.
     * </p>
     */
    @Test
    public void test20190614220911104541 ()
    {
        final Stage stage = tool.stage();
        final Processor<String> actor = Processor.fromIdentityScript(stage);

        tool.connect(actor.dataOut());
        actor.accept("Y");

        try
        {
            tool.awaitEquals(actor.dataOut(), "X");
            fail();
        }
        catch (AsyncTestTool.ExpectationFailedException ex)
        {
            assertEquals("X", ex.expected());
            assertEquals("Y", ex.actual());
            assertEquals("Expected: X, Actual: Y", ex.toString());
        }
    }

    /**
     * Test: 20190614220911104592
     *
     * <p>
     * Method: <code>awaitEquals</code>
     * </p>
     *
     * <p>
     * Case: Timeout.
     * </p>
     */
    @Test (expected = AsyncTestTool.AwaitTimeoutException.class)
    public void test20190614220911104592 ()
    {
        final Stage stage = tool.stage();
        final Processor<String> actor = Processor.fromIdentityScript(stage);

        tool.setAwaitTimeout(Duration.ZERO);
        tool.connect(actor.dataOut());
        tool.awaitEquals(actor.dataOut(), "X");
    }

    /**
     * Test: 20190614203444597472
     *
     * <p>
     * Method: <code>awaitMessage</code>
     * </p>
     *
     * <p>
     * Case: No Timeout Occurs.
     * </p>
     */
    @Test
    public void test20190614203444597472 ()
    {
        final Processor<String> actor = Processor.fromIdentityScript(tool.stage());

        tool.connect(actor.dataOut());
        actor.accept("X");
        assertEquals("X", tool.awaitMessage(actor.dataOut()));
    }

    /**
     * Test: 20190614214033914475
     *
     * <p>
     * Method: <code>awaitMessage</code>
     * </p>
     *
     * <p>
     * Case: Timeout.
     * </p>
     */
    @Test (expected = AsyncTestTool.AwaitTimeoutException.class)
    public void test20190614214033914475 ()
    {
        final Processor<String> actor = Processor.fromIdentityScript(tool.stage());

        tool.connect(actor.dataOut());
        tool.setAwaitTimeout(Duration.ZERO);
        tool.awaitMessage(actor.dataOut());
    }

    /**
     * Test: 20190614214033914519
     *
     * <p>
     * Method: <code>awaitMessage</code>
     * </p>
     *
     * <p>
     * Case: No Connection.
     * </p>
     */
    @Test (expected = AsyncTestTool.NoConnectionException.class)
    public void test20190614214033914519 ()
    {
        final Processor<String> actor = Processor.fromIdentityScript(tool.stage());

        tool.awaitMessage(actor.dataOut());
    }

    /**
     * Test: 20190614230910871278
     *
     * <p>
     * Method: <code>awaitMessage</code>
     * </p>
     *
     * <p>
     * Case: Interrupted.
     * </p>
     */
    @Test
    public void test20190614230910871278 ()
    {
        try
        {
            final Processor<String> actor = Processor.fromIdentityScript(tool.stage());
            tool.connect(actor.dataOut());
            Thread.currentThread().interrupt();
            assertTrue(Thread.currentThread().isInterrupted());
            tool.awaitMessage(actor.dataOut());
            fail();
        }
        catch (AsyncTestTool.AwaitInterruptedException ex)
        {
            assertFalse(Thread.currentThread().isInterrupted());
        }
    }

    /**
     * Test: 20190614203444597489
     *
     * <p>
     * Method: <code>awaitSteadyState</code>
     * </p>
     *
     * <p>
     * Case: No Timeout Occurs.
     * </p>
     */
    @Test
    public void test20190614203444597489 ()
    {
        tool.awaitSteadyState();
    }

    /**
     * Test: 20190614223530318146
     *
     * <p>
     * Method: <code>awaitSteadyState</code>
     * </p>
     *
     * <p>
     * Case: Timeout.
     * </p>
     */
    @Test
    public void test20190614223530318146 ()
    {
        /**
         * Create an actor that will block indefinitely,
         * which means the stage is not in a steady state,
         * because the actor is being actively executed.
         */
        final CountDownLatch latch = new CountDownLatch(1);
        final Processor<String> actor = Processor.fromConsumerScript(tool.stage(), x -> latch.await());

        /**
         * Cause the actor to begin blocking.
         */
        actor.dataIn().send("X");

        try
        {
            tool.setAwaitTimeout(Duration.ZERO);
            tool.awaitSteadyState();
            fail();
        }
        catch (AsyncTestTool.AwaitTimeoutException ex)
        {
            // Pass, because this is the desired outcome.
        }
        finally
        {
            latch.countDown();
        }
    }

    /**
     * Test: 20190614233701399317
     *
     * <p>
     * Method: <code>sleep</code>
     * </p>
     *
     * <p>
     * Case: Interrupted.
     * </p>
     */
    @Test
    public void test20190614233701399317 ()
    {
        try
        {
            Thread.currentThread().interrupt();
            assertTrue(Thread.currentThread().isInterrupted());
            tool.sleep(Duration.ZERO);
            fail();
        }
        catch (AsyncTestTool.AwaitInterruptedException ex)
        {
            assertFalse(Thread.currentThread().isInterrupted());
        }
    }

}
