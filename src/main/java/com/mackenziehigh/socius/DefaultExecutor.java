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
final class DefaultExecutor
{
    private static ScheduledExecutorService service;

    public static synchronized ScheduledExecutorService get ()
    {
        if (service == null)
        {
            final String propertyName = String.format("%s.threadCount", DefaultExecutor.class.getName());
            final String threadCountText = System.getProperty(propertyName);
            final int count = threadCountText.matches("[1-9][0-9]{0,5}") ? Integer.parseInt(threadCountText) : 1;
            final ScheduledExecutorService ses = Executors.newScheduledThreadPool(count);
            final Thread hook = new Thread(() -> ses.shutdown());
            Runtime.getRuntime().addShutdownHook(hook);
            service = ses;
        }

        return service;
    }
}
