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

import com.mackenziehigh.cascade.Cascade;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * A stack-machine implementation based on trampolining.
 *
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public abstract class AbstractPushdownAutomaton<I, O>
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
        public void onMessage (I message);
    }

    /**
     * Signature of a side effect function.
     */
    @FunctionalInterface
    public interface SideEffect
    {
        public void onExecute ();
    }

    /**
     * Specifies the initial state of the state-machine.
     *
     * @return the initial state.
     */
    protected abstract State<I> initial ();

    /**
     * This is the initial state that the state-machine is in.
     */
    private volatile State<I> initial;

    /**
     * These are the states and side-effects to execute next.
     */
    private final Deque<Object> pushdownStack = new ArrayDeque<>();

    protected AbstractPushdownAutomaton (Cascade.Stage stage)
    {
        super(stage);
    }

    /**
     * Push the given state onto the execution <code>Deque</code> (LIFO).
     *
     * @param state will be pushed onto the <code>Deque</code>.
     */
    protected void push (final State<I> state)
    {
        Objects.requireNonNull(state, "state");
        pushdownStack.push(state);
    }

    /**
     * Push the given side-effect onto the execution <code>Deque</code> (LIFO).
     *
     * @param effect will be pushed onto the <code>Deque</code>.
     */
    protected void push (final SideEffect effect)
    {
        Objects.requireNonNull(effect, "effect");
        pushdownStack.push(effect);
    }

    /**
     * Add the given state to the execution <code>Deque</code> (FIFO).
     *
     * @param state will be appended onto the <code>Deque</code>.
     */
    protected void then (final State<I> state)
    {
        Objects.requireNonNull(state, "state");
        pushdownStack.add(state);
    }

    /**
     * Add the given side-effect to the execution <code>Deque</code> (FIFO).
     *
     * @param effect will be appended onto the <code>Deque</code>.
     */
    protected void then (final SideEffect effect)
    {
        Objects.requireNonNull(effect, "effect");
        pushdownStack.add(effect);
    }

    /**
     * Remove all states and side-effects from the execution <code>Deque</code>.
     */
    protected void clear ()
    {
        pushdownStack.clear();
    }

    @Override
    protected final void onMessage (I message)
            throws Throwable
    {
        if (initial == null)
        {
            initial = initial();
        }

        if (pushdownStack.isEmpty())
        {
            pushdownStack.push(initial);
        }

        try
        {
            boolean executedState = false;

            while (pushdownStack.isEmpty() == false)
            {
                final Object task = pushdownStack.pop();

                if (executedState && task instanceof State)
                {
                    final State state = (State) task;
                    push(state);
                    return;
                }
                else if (task instanceof State)
                {
                    final State state = (State) task;
                    state.onMessage(message);
                    executedState = true;
                }
                else if (task instanceof SideEffect)
                {
                    final SideEffect effect = (SideEffect) task;
                    effect.onExecute();
                }
            }
        }
        catch (Throwable ex1)
        {
            try
            {
                onError(ex1);
            }
            catch (Throwable ex2)
            {
                clear();
                push(initial);
            }
        }
    }

    /**
     * Specifies the state to goto when an unhandled exception occurs.
     *
     * <p>
     * Case should be taken to ensure that this method never
     * throws an exception itself; otherwise, the state-machine
     * will suppress the exception and return to the initial state.
     * </p>
     *
     * @param cause is the unhandled exception.
     */
    protected void onError (final Throwable cause)
    {
        clear();
        push(initial);
    }
}