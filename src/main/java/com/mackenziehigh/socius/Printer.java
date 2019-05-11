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

import com.mackenziehigh.cascade.Cascade.ActorFactory;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.Processor;
import java.io.PrintStream;
import java.util.Objects;
import java.util.function.Consumer;
import com.mackenziehigh.socius.Pipeline;

/**
 * Prints the <code>String</code> representations
 * of messages to standard-output, standard-error.
 *
 * @param <T> is the type of the incoming and outgoing messages.
 */
public final class Printer<T>
        implements Pipeline<T, T>
{
    /**
     * Provides the connectors and performs the printing.
     */
    private final Processor<T> actor;

    /**
     * This is the user-specified log-message to print.
     * The message will be substituted into this string.
     */
    private final String format;

    /**
     * This function provides the actual printing logic.
     */
    private final Consumer<Object> method;

    /**
     * This is where the output will go to.
     */
    private final PrintStream stream;

    private Printer (final ActorFactory stage,
                     final PrintStream stream,
                     final String format,
                     final boolean line)
    {
        this.actor = Processor.fromFunctionScript(stage, this::print);
        this.format = Objects.requireNonNull(format, "format");
        this.stream = Objects.requireNonNull(stream, "stream");
        this.method = line ? stream::println : stream::print;
    }

    private T print (final T value)
    {
        final String text = String.format(format, value);
        method.accept(text);
        stream.flush();
        return value;
    }

    /**
     * Input Connection.
     *
     * @return the input that provides the messages to print.
     */
    @Override
    public Input<T> dataIn ()
    {
        return actor.dataIn();
    }

    /**
     * Output Connection.
     *
     * @return the output that merely forwards all messages.
     */
    @Override
    public Output<T> dataOut ()
    {
        return actor.dataOut();
    }

    /**
     * Create a new <code>Printer</code> that will print the messages
     * to standard-output without a trailing newline.
     *
     * <p>
     * The format string will be passed into the <code>String.format()</code> method.
     * In addition, the message will be passed-in to the method as an argument.
     * The resulting string is what will actually be printed.
     * </p>
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @param format describes how to print the message.
     * @return the new printer.
     */
    public static <T> Printer<T> newPrint (final ActorFactory stage,
                                           final String format)
    {
        return new Printer<>(stage, System.out, format, false);
    }

    /**
     * Create a new <code>Printer</code> that will print the messages
     * to standard-out with a trailing newline.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @return the new printer.
     */
    public static <T> Printer<T> newPrint (final ActorFactory stage)
    {
        return new Printer<>(stage, System.out, "%s", false);
    }

    /**
     * Create a new <code>Printer</code> that will print the messages
     * to standard-error with a trailing newline.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @return the new printer.
     */
    public static <T> Printer<T> newPrinterr (final ActorFactory stage)
    {
        return new Printer<>(stage, System.err, "%s", false);
    }

    /**
     * Create a new <code>Printer</code> that will print the messages
     * to standard-output with a trailing newline.
     *
     * <p>
     * The format string will be passed into the <code>String.format()</code> method.
     * In addition, the message will be passed-in to the method as an argument.
     * The resulting string is what will actually be printed.
     * </p>
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @param format describes how to print the message.
     * @return the new printer.
     */
    public static <T> Printer<T> newPrintln (final ActorFactory stage,
                                             final String format)
    {

        return new Printer<>(stage, System.out, format, true);
    }

    /**
     * Create a new <code>Printer</code> that will print the messages
     * to standard-output with a trailing newline.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @return the new printer.
     */
    public static <T> Printer<T> newPrintln (final ActorFactory stage)
    {
        return new Printer<>(stage, System.out, "%s", true);
    }

    /**
     * Create a new <code>Printer</code> that will print the messages
     * to standard-error without a trailing newline.
     *
     * <p>
     * The format string will be passed into the <code>String.format()</code> method.
     * In addition, the message will be passed-in to the method as an argument.
     * The resulting string is what will actually be printed.
     * </p>
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @param format describes how to print the message.
     * @return the new printer.
     */
    public static <T> Printer<T> newPrinterr (final ActorFactory stage,
                                              final String format)
    {
        return new Printer<>(stage, System.err, format, false);
    }

    /**
     * Create a new <code>Printer</code> that will print the messages
     * to standard-error with a trailing newline.
     *
     * <p>
     * The format string will be passed into the <code>String.format()</code> method.
     * In addition, the message will be passed-in to the method as an argument.
     * The resulting string is what will actually be printed.
     * </p>
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @param format describes how to print the message.
     * @return the new printer.
     */
    public static <T> Printer<T> newPrinterrln (final ActorFactory stage,
                                                final String format)
    {

        return new Printer<>(stage, System.err, format, true);
    }

    /**
     * Create a new <code>Printer</code> that will print the messages
     * to standard-error with a trailing newline.
     *
     * @param <T> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @return the new printer.
     */
    public static <T> Printer<T> newPrinterrln (final ActorFactory stage)
    {
        return new Printer<>(stage, System.err, "%s", true);
    }
}
