package com.mackenziehigh.socius.io;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.Processor;
import java.io.PrintStream;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Prints objects to standard-output.
 *
 * @param <T>
 */
public final class Printer<T>
{
    private final Processor<T> actor;

    private final String format;

    private final Consumer<Object> method;

    private Printer (final Stage stage,
                     final PrintStream stream,
                     final String format,
                     final boolean line)
    {
        this.actor = Processor.newProcessor(stage, this::print);
        this.format = Objects.requireNonNull(format, "format");
        Objects.requireNonNull(stream, "stream");
        this.method = line ? stream::println : stream::print;
    }

    private T print (final T value)
    {
        final String text = String.format(format, value);
        method.accept(text);
        return value;
    }

    public Input<T> dataIn ()
    {
        return actor.dataIn();
    }

    public Output<T> dataOut ()
    {
        return actor.dataOut();
    }

    public static <T> Printer<T> newPrint (final Stage stage,
                                           final String format)
    {
        return new Printer<>(stage, System.out, format, false);
    }

    public static <T> Printer<T> newPrintln (final Stage stage,
                                             final String format)
    {

        return new Printer<>(stage, System.out, format, true);
    }

    public static <T> Printer<T> newPrintln (final Stage stage)
    {
        return new Printer<>(stage, System.out, "%s", true);
    }

    public static <T> Printer<T> newPrinterr (final Stage stage,
                                              final String format)
    {
        return new Printer<>(stage, System.err, format, false);
    }

    public static <T> Printer<T> newPrinterrln (final Stage stage,
                                                final String format)
    {

        return new Printer<>(stage, System.err, format, true);
    }

    public static <T> Printer<T> newPrinterrln (final Stage stage)
    {
        return new Printer<>(stage, System.err, "%s", true);
    }
}
