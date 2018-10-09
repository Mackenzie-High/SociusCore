package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mutably caches a value in-memory and forwards the value on clock ticks.
 *
 * @param <T> is the type of the cached value.
 */
public final class Variable<T>
{
    private final Processor<T> procDataIn;

    private final Mapper<Boolean, T> procDataOut;

    private final Processor<Instant> procClock;

    private final AtomicReference<T> variable = new AtomicReference<>();

    private Variable (final Stage stage,
                      final T initial)
    {
        this.procClock = Processor.newProcessor(stage, this::onGet);
        this.procDataIn = Processor.newProcessor(stage, this::onSet);
        this.procDataOut = Mapper.newMapper(stage, this::onSend);
        this.variable.set(initial);
    }

    private void onSet (final T message)
    {
        variable.set(message);
        sendValueThreadSafely();
    }

    private Instant onGet (final Instant message)
    {
        sendValueThreadSafely();
        return message;
    }

    private T onSend (final Boolean message)
    {
        return variable.get();
    }

    /**
     * Send a constant to onSend(), which will then read the variable.
     * This ensures that the dataOut() always gets the most up-to-date value.
     */
    private void sendValueThreadSafely ()
    {
        procDataOut.dataIn().send(true);
    }

    /**
     * Set the value currently stored in this variable.
     *
     * @param value is the new value.
     * @return this.
     */
    public Variable<T> set (final T value)
    {
        variable.set(value);
        sendValueThreadSafely();
        return this;
    }

    /**
     * Get the current value stored in the variable.
     *
     * @return the current value.
     */
    public T get ()
    {
        return variable.get();
    }

    /**
     * Use this input to set the value stored in this variable.
     *
     * @return the input connector.
     */
    public Input<T> dataIn ()
    {
        return procDataIn.dataIn();
    }

    /**
     * Use this output to receive values from this variable.
     *
     * @return the output connector.
     */
    public Output<T> dataOut ()
    {
        return procDataOut.dataOut();
    }

    /**
     * Use this input to cause the stored value to be sent to the data-out.
     *
     * @return the input connector.
     */
    public Input<Instant> clockIn ()
    {
        return procClock.dataIn();
    }

    /**
     * This output merely forwards the clock-in messages.
     *
     * @return the output connector.
     */
    public Output<Instant> clockOut ()
    {
        return procClock.dataOut();
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
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(initial, "initial");
        return new Variable<>(stage, initial);
    }
}
