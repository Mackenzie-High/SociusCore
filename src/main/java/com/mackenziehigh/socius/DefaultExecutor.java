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

import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Provides the default <code>ScheduledExecutorService</code>.
 *
 * <p>
 * Operations performed on this executor must be near instant.
 * Generally, this executor is only used to send message (no processing).
 * </p>
 *
 * <p>
 * Obtain this executor lazily in order to avoid allocating unnecessary resources.
 * </p>
 *
 * <p>
 * The number of threads can be set at startup via the <code>com.mackenziehigh.socius.DefaultExecutor.threadCount</code> system property.
 * </p>
 */
public final class DefaultExecutor
{
    /**
     * This is the name of the property that can be used
     * to specify the number of threads to allocate.
     */
    public static final String PROPERTY_NAME = String.format("%s.threadCount", DefaultExecutor.class.getName());

    private static volatile DefaultExecutor instance;

    private volatile ScheduledExecutorService service;

    private volatile OptionalInt threadCount = OptionalInt.empty();

    private final String threadCountText;

    /**
     * Sole Constructor.
     *
     * <p>
     * This constructor is only exposed for unit-testing purposes.
     * </p>
     *
     * @param threadCountText is the number of threads to allocate.
     */
    DefaultExecutor (final String threadCountText)
    {
        this.threadCountText = Objects.requireNonNull(threadCountText, "threadCountText");
    }

    /**
     * Get the singleton instance of this class, creating it if necessary.
     *
     * @return the only instance of this class.
     */
    public static DefaultExecutor instance ()
    {
        synchronized (DefaultExecutor.class)
        {
            final String threadCountProperty = System.getProperty(PROPERTY_NAME, "");
            instance = instance == null ? new DefaultExecutor(threadCountProperty) : instance;
        }

        return instance;
    }

    /**
     * Get the number of threads allocated for use by the executor, if any.
     *
     * @return the number of threads in the default-executor.
     */
    public OptionalInt threadCount ()
    {
        return threadCount;
    }

    final ScheduledExecutorService service ()
    {
        synchronized (this)
        {
            if (service == null)
            {
                final int count = threadCountText.matches("[1-9][0-9]{0,5}") ? Integer.parseInt(threadCountText) : 1;
                threadCount = OptionalInt.of(count);
                final ScheduledExecutorService ses = Executors.newScheduledThreadPool(count);
                final Thread hook = new Thread(() -> ses.shutdown());
                Runtime.getRuntime().addShutdownHook(hook);
                service = ses;
            }
        }

        return service;
    }
}
