package com.mackenziehigh.socius.plugins.flow;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mutably caches a value in-memory and forwards the value on clock ticks.
 *
 * @param <T> is the type of the cached value.
 */
public final class Variable<T>
{
    private final Processor<T> dataIn;

    private final Processor<T> dataOut;

    private final Processor<Boolean> clearIn;

    private final Processor<Boolean> clearOut;

    private final Processor<Instant> clockIn;

    private final Processor<Instant> clockOut;

    private final AtomicReference<T> variable = new AtomicReference<>();

    private Variable (final Stage stage,
                      final T initial)
    {
        this.clockIn = Processor.newProcessor(stage, this::onGet);
        this.clockOut = Processor.newProcessor(stage);
        this.clearIn = Processor.newProcessor(stage, this::onClear);
        this.clearOut = Processor.newProcessor(stage);
        this.dataIn = Processor.newProcessor(stage, this::onSet);
        this.dataOut = Processor.newProcessor(stage);
        this.variable.set(initial);
    }

    private void onClear (final Boolean message)
    {
        if (message)
        {
            variable.set(null);
        }
    }

    private void onSet (final T message)
    {
        variable.set(message);
        dataOut.dataIn().send(message);
    }

    private void onGet (final Instant message)
    {
        final T value = variable.get();
        dataOut.dataIn().send(value == null ? value : null);
    }

    /**
     * Use this input to set the value stored in this variable.
     *
     * @return the input connector.
     */
    public Input<T> dataIn ()
    {
        return dataIn.dataIn();
    }

    /**
     * Use this output to receive values from this variable.
     *
     * @return the output connector.
     */
    public Output<T> dataOut ()
    {
        return dataOut.dataOut();
    }

    /**
     * Use this input to cause the stored value to be sent to the data-out.
     *
     * @return the input connector.
     */
    public Input<Instant> clockIn ()
    {
        return clockIn.dataIn();
    }

    /**
     * This output merely forwards the clock-in messages.
     *
     * @return the output connector.
     */
    public Output<Instant> clockOut ()
    {
        return clockOut.dataOut();
    }

    /**
     * Use this input to cause this variable to be cleared.
     *
     * @return the input connector.
     */
    public Input<Boolean> clearIn ()
    {
        return clearIn.dataIn();
    }

    /**
     * This output merely forwards the clear-in messages.
     *
     * @return the output connector.
     */
    public Output<Boolean> clearOut ()
    {
        return clearOut.dataOut();
    }

    /**
     * Factory Method (Initially Empty Variable).
     *
     * @param <T> is the type of the value stored in the variable.
     * @param stage will be used to create private actors.
     * @return the new variable.
     */
    public static <T> Variable<T> newVariable (final Stage stage)
    {
        return new Variable<>(stage, null);
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of the value stored in the variable.
     * @param stage will be used to create private actors.
     * @param initial will be the initial value stored in the variable.
     * @return the new variable.
     */
    public static <T> Variable<T> newVariable (final Stage stage,
                                               final T initial)
    {
        return new Variable<>(stage, initial);
    }
}
