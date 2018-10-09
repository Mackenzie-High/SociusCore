package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage;
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
    private final Processor<T> procDataIn;

    private final Processor<T> procDataOut;

    private final Processor<Boolean> procToggleIn;

    private final Processor<Boolean> procToggleOut;

    private final Object lock = new Object();

    private final AtomicBoolean flag = new AtomicBoolean();

    private Valve (final Stage stage,
                   final boolean open)
    {
        this.procDataIn = Processor.newProcessor(stage, this::onDataIn);
        this.procDataOut = Processor.newProcessor(stage);
        this.procToggleIn = Processor.newProcessor(stage, this::onToggleIn);
        this.procToggleOut = Processor.newProcessor(stage, this::onToggleOut);
        this.procDataIn.dataOut().connect(this.procDataOut.dataIn());
        this.flag.set(open);
    }

    private T onDataIn (final T message)
    {
        return flag.get() ? message : null;
    }

    private void onToggleIn (final Boolean message)
    {
        toggle(message);
    }

    private Boolean onToggleOut (final Boolean message)
    {
        return flag.get();
    }

    /**
     * Open or close this valve.
     *
     * @param state is true, if the valve should be open, false otherwise.
     * @return this.
     */
    public Valve<T> toggle (final boolean state)
    {
        /**
         * Open or close the valve.
         */
        flag.set(state);

        /**
         * Notify down-stream parties of the change.
         * We must ensure thread-safety, since this method may be called concurrently.
         * A simple trick to ensure this is to let onToggleOut() read the flag.
         */
        procToggleOut.dataIn().send(true); // true is merely a placeholder.

        return this;
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
        return !isOpen();
    }

    /**
     * The messages flowing through this input will be forward to data-out,
     * if and only if the valve is currently open.
     *
     * @return the input that can be turned on/off by this valve.
     */
    public Input<T> dataIn ()
    {
        return procDataIn.dataIn();
    }

    /**
     * When this valve is open, then all messages from data-in
     * will be forwarded to this output.
     *
     * @return the output that can be turned on/off by this valve.
     */
    public Output<T> dataOut ()
    {
        return procDataOut.dataOut();
    }

    /**
     * Use this input to open/close the valve.
     *
     * @return the valve control input.
     */
    public Input<Boolean> toggleIn ()
    {
        return procToggleIn.dataIn();
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
    public Output<Boolean> toggleOut ()
    {
        return procToggleOut.dataOut();
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
