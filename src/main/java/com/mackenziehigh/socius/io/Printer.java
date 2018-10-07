package com.mackenziehigh.socius.io;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Objects;

/**
 * Prints objects to standard-output.
 *
 * @param <T>
 */
public final class Printer<T>
{
    private final Actor<T, T> actor;

    private final Input<T> dataIn;

    private final Output<T> dataOut;

    private volatile String format = "%s\n";

    public Printer (final Stage stage)
    {
        this.actor = stage.newActor().withScript(this::print).create();
        this.dataIn = actor.input();
        this.dataOut = actor.output();
    }

    public Input<T> dataIn ()
    {
        return dataIn;
    }

    public Output<T> dataOut ()
    {
        return dataOut;
    }

    public Printer<T> format (final String format)
    {
        this.format = Objects.requireNonNull(format, "format");
        return this;
    }

    private T print (final T value)
    {
        final String text = String.format(format, value);
        System.out.println(text);
        return value;
    }
}
