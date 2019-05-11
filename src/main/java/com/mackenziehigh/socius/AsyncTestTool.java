package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 */
public final class AsyncTestTool
        implements Closeable
{
    private final Stage stage = Cascade.newStage();

    private final ConcurrentMap<Output<?>, BlockingQueue<Object>> connections = new ConcurrentHashMap<>();

    public synchronized <T> void connect (final Output<T> output)
    {
        if (connections.containsKey(output) == false)
        {
            final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
            final Processor<T> proc = Processor.fromConsumerScript(stage, x -> queue.add(x));
            output.connect(proc.dataIn());
            connections.put(output, queue);
        }
    }

    public Object await (final Output<?> output)
    {
        if (connections.containsKey(output))
        {
            try
            {
                final BlockingQueue<Object> queue = connections.get(output);
                return queue.take();
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

    public <T> void expect (final Output<T> output,
                            final T expected)
    {
        final Object actual = await(output);

        if (Objects.equals(expected, actual) == false)
        {
            throw new IllegalStateException(String.format("Expect: %s, Actual: %s", expected, actual));
        }
    }

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

    @Override
    public void close ()
    {
        stage.close();
    }

}
