package com.mackenziehigh.socius.subprocess;

import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.io.Printer;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One-shot shell-command executor.
 */
public final class ShellCommand
{
    private final Processor<Boolean> procTerm;

    private final Processor<Boolean> procKill;

    private final Processor<Integer> procExit;

    private final Processor<String> procStdin;

    private final Processor<String> procStdout;

    private final Processor<String> procStderr;

    private final BlockingQueue<String> stdinQueue = new LinkedBlockingQueue<>();

    private final Thread waiter = new Thread(this::waitForExit);

    private final Thread stdinWriter = new Thread(this::writeStdin);

    private final Thread stdoutReader = new Thread(this::readStdout);

    private final Thread stderrReader = new Thread(this::readStderr);

    private final AtomicBoolean started = new AtomicBoolean();

    private final AtomicBoolean stop = new AtomicBoolean();

    private final ProcessBuilder processBuilder;

    private volatile Process process;

    private final CountDownLatch exitLatch = new CountDownLatch(4);

    private final Future<Integer> future;

    private ShellCommand (final Builder builder)
    {
        this.procTerm = Processor.newProcessor(builder.stage, this::term);
        this.procKill = Processor.newProcessor(builder.stage, this::kill);
        this.procExit = Processor.newProcessor(builder.stage);
        this.procStdin = Processor.newProcessor(builder.stage, this::enqueueStdin);
        this.procStdout = Processor.newProcessor(builder.stage);
        this.procStderr = Processor.newProcessor(builder.stage);
        this.future = Futures.immediateFuture(0); // TODO
        this.processBuilder = new ProcessBuilder(builder.commands);

        this.waiter.setName("waiter");
        this.stdinWriter.setName("stdinWriter");
        this.stdoutReader.setName("stdoutReader");
        this.stderrReader.setName("stderrReader");

        this.waiter.setDaemon(true);
        this.stdinWriter.setDaemon(true);
        this.stdoutReader.setDaemon(true);
        this.stderrReader.setDaemon(true);
    }
    
    private void term (final Boolean condition)
    {
        if (process != null && condition)
        {
            process.destroy(); // TODO: Re-read docs
        }
    }

    private void kill (final Boolean condition)
    {
        if (process != null && condition)
        {
            process.destroyForcibly();
        }
    }

    private void enqueueStdin (final String line)
    {
        stdinQueue.add(line);
    }

    private void waitForExit ()
    {
        try
        {
            final int exitValue = process.waitFor();
            stop.set(true);
//            stdinWriter.interrupt();
//            stdoutReader.interrupt();
//            stderrReader.interrupt();

            exitLatch.countDown();
            exitLatch.await();

            procExit.dataIn().send(exitValue);

            // TODO: Race Condition
            Verify.verify(process.isAlive() == false);
            Verify.verify(stdinWriter.isAlive() == false);
            Verify.verify(stdoutReader.isAlive() == false);
            Verify.verify(stderrReader.isAlive() == false);
        }
        catch (InterruptedException ex)
        {
            // Pass.
        }
        catch (Throwable ex)
        {
            ex.printStackTrace(System.err);
        }

    }

    private void writeStdin ()
    {
        do
        {
            try
            {
                final String line = stdinQueue.poll(1, TimeUnit.SECONDS);

                if (line != null)
                {
                    final byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
                    process.getOutputStream().write(bytes);
                    process.getOutputStream().flush();
                }
            }
            catch (InterruptedException | IOException ex)
            {
                // Pass
            }
        }
        while (stop.get() == false);

        exitLatch.countDown();
    }

    private void readStdout ()
    {
        do
        {
            try (BufferedInputStream bin = new BufferedInputStream(process.getInputStream());
                 Scanner scanner = new Scanner(bin))
            {
                while (scanner.hasNextLine())
                {
                    final String line = scanner.nextLine();
                    procStdout.dataIn().send(line);
                }
            }
            catch (Throwable ex)
            {
                ex.printStackTrace(System.err);
            }
        }
        while (stop.get() == false);

        exitLatch.countDown();
    }

    private void readStderr ()
    {
        exitLatch.countDown();
    }

    /**
     * Start the shell-command and return immediately.
     *
     * <p>
     * Only the first invocation of this method performs an action.
     * Subsequent invocations are no-ops.
     * </p>
     *
     * @return a future that will provide access to the exit-code of the command.
     * @throws IOException
     */
    public Future<Integer> execute ()
            throws IOException
    {
        if (started.compareAndSet(false, true))
        {
            process = processBuilder.start();
            waiter.start();
            stdinWriter.start();
            stdoutReader.start();
            stderrReader.start();
        }

        return future;
    }

    /**
     * Send true to this input in order to terminate the shell-command.
     *
     * @return the termination control input.
     */
    public Input<Boolean> termIn ()
    {
        return procTerm.dataIn();
    }

    /**
     * Send true to this input in order to kill the shell-command.
     *
     * @return the kill control input.
     */
    public Input<Boolean> killIn ()
    {
        return procKill.dataIn();
    }

    /**
     * The exit-code of the shell-command will be sent via this output.
     *
     * @return the output.
     */
    public Output<Integer> exitOut ()
    {
        return procExit.dataOut();
    }

    /**
     * Use this input to send text to the standard-input of the shell-command.
     *
     * @return the input that supplies standard-input to the command.
     */
    public Input<String> stdin ()
    {
        return procStdin.dataIn();
    }

    /**
     * Use this method to receive lines from the standard-output of the shell-command.
     *
     * @return the output that is connected to the standard-output of the command.
     */
    public Output<String> stdout ()
    {
        return procStdout.dataOut();
    }

    /**
     * Use this method to receive lines from the standard-error of the shell-command.
     *
     * @return the output that is connected to the standard-error of the command.
     */
    public Output<String> stderr ()
    {
        return procStderr.dataOut();
    }

    /**
     * Factory Method.
     *
     * @param stage will be used to create private actors.
     * @return a builder that can create a new shell-command.
     */
    public static Builder newShellCommand (final Stage stage)
    {
        return new Builder(stage);
    }

    /**
     * Builder.
     */
    public static final class Builder
    {
        private final Stage stage;

        private final List<String> commands = Lists.newLinkedList();

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        /**
         * Use this method to specify the command to execute.
         *
         * @param command is the shell-command to execute.
         * @return this.
         */
        public Builder execute (final String... command)
        {
            commands.addAll(Arrays.asList(command));
            return this;
        }

        /**
         * Build.
         *
         * @return the newly built object.
         */
        public ShellCommand build ()
        {
            return new ShellCommand(this);
        }
    }

    public static void main (String[] args)
            throws IOException
    {
        final Stage stage = Cascade.newStage();
        final Printer e = Printer.newPrintln(stage, "E = %s");
        final Printer p = Printer.newPrintln(stage, "X = %s");

        final ShellCommand shell = ShellCommand.newShellCommand(stage).execute("head").build();
        shell.procStdout.dataOut().connect(p.dataIn());
        shell.procExit.dataOut().connect(e.dataIn());
        shell.procStdin.dataIn().send("Erin\n\n");

        shell.execute();

        System.in.read();
    }
}
