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

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class DefaultExecutorTest
{
    /**
     * Test: 20190615215253287110
     *
     * <p>
     * Method: <code>instance</code>
     * </p>
     *
     * <p>
     * Case: Singleton Always Returned.
     * </p>
     */
    @Test
    public void test20190615215253287110 ()
    {
        assertSame(DefaultExecutor.instance(), DefaultExecutor.instance());
    }

    /**
     * Test: 20190615215253287155
     *
     * <p>
     * Method: <code>service</code>
     * </p>
     *
     * <p>
     * Case: Default Thread Count.
     * </p>
     */
    @Test
    public void test20190615215253287155 ()
    {
        final DefaultExecutor executor = new DefaultExecutor("X");
        assertTrue(executor.threadCount().isEmpty());
        executor.service();
        assertTrue(executor.threadCount().isPresent());
        assertEquals(1, executor.threadCount().getAsInt());
    }

    /**
     * Test: 20190615220809143952
     *
     * <p>
     * Method: <code>service</code>
     * </p>
     *
     * <p>
     * Case: Non-Default Thread Count.
     * </p>
     */
    @Test
    public void test20190615220809143952 ()
    {
        final DefaultExecutor executor = new DefaultExecutor("13");
        assertTrue(executor.threadCount().isEmpty());
        executor.service();
        assertTrue(executor.threadCount().isPresent());
        assertEquals(13, executor.threadCount().getAsInt());
    }

    /**
     * Test: 20190615221742318467
     *
     * <p>
     * Case: Verify Property Name.
     * </p>
     */
    @Test
    public void test20190615221742318467 ()
    {
        assertEquals("com.mackenziehigh.socius.DefaultExecutor.threadCount", DefaultExecutor.PROPERTY_NAME);
    }
}
