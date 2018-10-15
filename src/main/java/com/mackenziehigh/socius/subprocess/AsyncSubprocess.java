package com.mackenziehigh.socius.subprocess;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.utils.memory.ByteSequence;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Scanner;

/**
 *
 */
public final class AsyncSubprocess
{
    private final Processor<ByteSequence> procStdin;

    private final Processor<ByteSequence> procStdout;

    private final Processor<ByteSequence> procStderr;

    private final String staticId = null;

    private volatile String dynamicId = null;

    private volatile Process process;

    private volatile Thread stdoutThread = null;

    private volatile Thread stderrThread = null;

    private AsyncSubprocess (final Builder builder)
    {
        this.procStdin = Processor.newProcessor(builder.stage, this::onMessage);
        this.procStdout = Processor.newProcessor(builder.stage);
        this.procStderr = Processor.newProcessor(builder.stage);
    }

    private void onMessage (final ByteSequence message)
            throws IOException
    {
        startProcess(message);
        sendToStandardIn(message);
    }

    private void startProcess (final ByteSequence message)
            throws IOException
    {
        if (process != null && process.isAlive())
        {
            return;
        }

        final ProcessBuilder builder = new ProcessBuilder();
        {

        }
        process = builder.start();

        stdoutThread = new Thread(() -> readStandardOut(process.getInputStream()));
        stderrThread = new Thread(() -> readStandardErr(process.getErrorStream()));

        stdoutThread.start();
        stderrThread.start();
    }

    private void sendToStandardIn (final ByteSequence message)
            throws IOException
    {
        process.getOutputStream().write(message.toByteArray());
        process.getOutputStream().flush();
    }

    private void readStandardOut (final InputStream stdout)
    {
        final InputStream bin = new BufferedInputStream(stdout);
        final Scanner scanner = new Scanner(bin);

        while (true)
        {
            final String line = scanner.nextLine();

            //procStdout.dataIn().send(line);
        }
    }

    private void readStandardErr (final InputStream stdout)
    {

    }

    public Input<ByteSequence> stdin ()
    {
        return procStdin.dataIn();
    }

    public Output<ByteSequence> stdout ()
    {
        return procStdout.dataOut();
    }

    public Output<ByteSequence> stderr ()
    {
        return procStderr.dataOut();
    }

    public static Builder newAsyncSubprocess (final Stage stage)
    {
        return new Builder(stage);
    }

    public static final class Builder
    {
        private final Stage stage;

        private boolean prestart = false;

        private boolean restart = false;

        public Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        public Builder prestart ()
        {
            return this;
        }

        public AsyncSubprocess build ()
        {
            return new AsyncSubprocess(this);
        }
    }
}
