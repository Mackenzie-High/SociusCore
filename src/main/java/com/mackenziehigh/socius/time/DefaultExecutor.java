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
