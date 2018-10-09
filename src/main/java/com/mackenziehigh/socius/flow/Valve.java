package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Conditionally forwards messages based on a boolean flag (open|closed).
 *
 * @param <T> is the type of messages that flow through the valve.
 */
public final class Valve<T>
{
    private final Actor<T, T> pipe;

    private final Actor<Boolean, Boolean> toggle;

    private final AtomicBoolean flag = new AtomicBoolean();

    private Valve (final Stage stage,
                   final boolean open)
    {
        this.pipe = stage.newActor().withScript(this::onPipeMessage).create();
        this.toggle = stage.newActor().withScript(this::onToggleMessage).create();
        this.flag.set(open);
    }

    private T onPipeMessage (final T message)
    {
        return flag.get() ? message : null;
    }

    private void onToggleMessage (final Boolean message)
    {
        flag.set(message);
    }

    /**
     * Determine whether the valve is currently open.
     *
     * @return true, if the valve is open.
     */
    public boolean isOpen ()
    {
        return flag.get();
    }

    /**
     * Determine whether the valve is currently closed.
     *
     * @return true, if the valve is closed.
     */
    public boolean isClosed ()
    {
        return flag.get();
    }

    /**
     * The messages flowing through this input will be forward to data-out,
     * if and only if the valve is currently open.
     *
     * @return the input that can be turned on/off by this valve.
     */
    public Input<T> dataIn ()
    {
        return pipe.input();
    }

    /**
     * When this valve is open, then all messages from data-in
     * will be forwarded to this output.
     *
     * @return the output that can be turned on/off by this valve.
     */
    public Output<T> dataOut ()
    {
        return pipe.output();
    }

    /**
     * Use this input to open/close the valve.
     *
     * @return the valve control input.
     */
    public Input<Boolean> toggleIn ()
    {
        return toggle.input();
    }

    /**
     * This output merely forwards the toggle-in input.
     *
     * <p>
     * This output is intended to facilitate daisy chaining.
     * </p>
     *
     * @return the valve control output.
     */
    public Input<Boolean> toggleOut ()
    {
        return toggle.input();
    }

    /**
     * Factory Method (Initially Open).
     *
     * @param <T> is the type of messages that will flow through the valve.
     * @param stage will be used to create private actors.
     * @return the new valve.
     */
    public static <T> Valve<T> newOpenValve (final Stage stage)
    {
        return new Valve<>(stage, true);
    }

    /**
     * Factory Method (Initially Closed).
     *
     * @param <T> is the type of messages that will flow through the valve.
     * @param stage will be used to create private actors.
     * @return the new valve.
     */
    public static <T> Valve<T> newClosedValve (final Stage stage)
    {
        return new Valve<>(stage, false);
    }
}
