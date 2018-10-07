package com.mackenziehigh.socius.subprocess;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.gpb.subprocess_m.ProcessRequest;
import com.mackenziehigh.socius.gpb.subprocess_m.ProcessResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;

/**
 *
 */
public final class AsyncSubprocess
{
    private final Processor<ProcessRequest> dataIn;

    private final Processor<ProcessResponse> dataOut;

    private final String staticId = null;

    private volatile String dynamicId = null;

    private volatile Process process;

    private volatile Thread stdoutThread = null;

    private volatile Thread stderrThread = null;

    private AsyncSubprocess (final Builder builder)
    {
        this.dataIn = Processor.newProcessor(builder.stage, this::onMessage);
        this.dataOut = Processor.newProcessor(builder.stage);
    }

    private void onMessage (final ProcessRequest message)
            throws IOException
    {
        startProcess(message);
        sendToStandardIn(message);
    }

    private void startProcess (final ProcessRequest message)
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

    private void sendToStandardIn (final ProcessRequest message)
            throws IOException
    {
        for (String line : message.getStdinList())
        {
            process.getOutputStream().write(line.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().flush();
        }
    }

    private void readStandardOut (final InputStream stdout)
    {
        final InputStream bin = new BufferedInputStream(stdout);
        final Scanner scanner = new Scanner(bin);

        while (true)
        {
            final String line = scanner.nextLine();

            final ProcessResponse response = ProcessResponse
                    .newBuilder()
                    .setStaticId(staticId)
                    .setDynamicId(dynamicId)
                    .addStdout(line)
                    .build();

            dataOut.dataIn().send(response);
        }
    }

    private void readStandardErr (final InputStream stdout)
    {

    }

    public static Builder newAsyncSubprocess (final Stage stage)
    {
        return new Builder(stage);
    }

    public static final class Builder
    {
        private final Stage stage;

        public Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        public AsyncSubprocess create ()
        {
            return new AsyncSubprocess(this);
        }
    }
}
