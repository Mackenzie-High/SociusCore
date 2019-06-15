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
package com.mackenziehigh.socius.core;

import com.mackenziehigh.cascade.Cascade.Stage;
import java.util.Objects;

/**
 * A state-machine implementation based on trampolining.
 *
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public abstract class AbstractTrampoline<I, O>
        extends AbstractPipeline<I, O>
{
    /**
     * Signature of a state transition function.
     *
     * @param <I> is the type of the incoming messages.
     */
    @FunctionalInterface
    public interface State<I>
    {
        public State<I> onMessage (I message)
                throws Throwable;
    }

    /**
     * Specifies the initial state of the state-machine.
     *
     * @param message is the first message received.
     * @return the initial state.
     * @throws java.lang.Throwable if something goes wrong.
     */
    protected abstract State<I> onInitial (I message)
            throws Throwable;

    /**
     * This state responds to messages by doing nothing.
     */
    private final State<I> nop = message -> this.nop;

    /**
     * This is the initial state that the state-machine is in.
     */
    private final State<I> initial = this::onInitial;

    /**
     * This is the current state that the state-machine is in.
     */
    private volatile State<I> current = initial;

    protected AbstractTrampoline (final Stage stage)
    {
        super(stage);
    }

    @Override
    protected final void onMessage (final I message)
            throws Throwable
    {
        try
        {
            current = current.onMessage(message);
        }
        catch (Throwable ex1)
        {
            try
            {
                current = onError(current, message, ex1);
            }
            catch (Throwable ex2)
            {
                current = nop;
            }
        }
    }

    /**
     * Specifies the state to goto, given the current state,
     * when an unhandled exception occurs in the current state.
     *
     * <p>
     * This method is intended to be overridden when desired.
     * </p>
     *
     * <p>
     * By default, this method merely returns the initial state.
     * </p>
     *
     * <p>
     * Care should be taken to ensure that this method never
     * throws an exception itself; otherwise, the state-machine
     * will suppress the exception and return to the initial state.
     * </p>
     *
     * @param state is the current state.
     * @param message was being processed when the exception occurred.
     * @param cause is the unhandled exception.
     * @return the next state.
     * @throws java.lang.Throwable if something goes wrong.
     */
    protected State<I> onError (final State<I> state,
                                final I message,
                                final Throwable cause)
            throws Throwable
    {
        return initial;
    }

    /**
     * Get the default no-op state.
     *
     * @return the no-op state.
     */
    public final State<I> nop ()
    {
        return nop;
    }

    /**
     * Determine whether this state-machine is in the no-op state.
     *
     * @return true, if this state-machine is in the no-op state.
     */
    public final boolean isNop ()
    {
        return nop.equals(current);
    }

    /**
     * Get the state that the state-machine is currently in.
     *
     * @return the current state.
     */
    public final State<I> state ()
    {
        return current;
    }

    /**
     * Set the state that the state-machine is currently in.
     *
     * @param state is the state that the machine will be in.
     * @return the new state.
     */
    public final State<I> state (final State<I> state)
    {
        current = Objects.requireNonNull(state, "state");
        return state;
    }
}
