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
 */
final class DefaultExecutor
{
    private static ScheduledExecutorService service;

    public static synchronized ScheduledExecutorService get ()
    {
        if (service == null)
        {
            final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
            final Thread hook = new Thread(() -> ses.shutdown());
            Runtime.getRuntime().addShutdownHook(hook);
            service = ses;
        }

        return service;
    }
}
