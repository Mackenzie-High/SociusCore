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
        implements Pipeline<T, T>
{
    /**
     * Provides the data-input connector.
     */
    private final Processor<T> procDataIn;

    /**
     * Provides the data-output connector.
     */
    private final Pipeline<Boolean, T> procDataOut;

    /**
     * Provides the clock-input and clock-output connectors.
     */
    private final Processor<Instant> procClock;

    /**
     * Provides the actual storage.
     */
    private final AtomicReference<T> variable;

    private Variable (final Stage stage,
                      final AtomicReference<T> variable)
    {
        this.procClock = Processor.fromFunctionScript(stage, this::onGet);
        this.procDataIn = Processor.fromConsumerScript(stage, this::onSet);
        this.procDataOut = Pipeline.fromFunctionScript(stage, this::onSend);
        this.variable = variable;
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
    @Override
    public Input<T> dataIn ()
    {
        return procDataIn.dataIn();
    }

    /**
     * Use this output to receive values from this variable.
     *
     * @return the output connector.
     */
    @Override
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
        return new Variable<>(stage, new AtomicReference<>(initial));
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of the value stored in the variable.
     * @param stage will be used to create private actors.
     * @param variable stores the value of the variable and may be shared.
     * @return the new variable.
     */
    public static <T> Variable<T> newSharedVariable (final Stage stage,
                                                     final AtomicReference<T> variable)
    {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(variable, "variable");
        return new Variable<>(stage, variable);
    }
}
