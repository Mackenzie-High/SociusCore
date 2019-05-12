package com.mackenziehigh.socius;

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
        public State<I> onMessage (I message);
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
     * This is the current state that the state-machine is in.
     */
    private volatile State<I> current;

    protected AbstractTrampoline (Stage stage)
    {
        super(stage);
    }

    @Override
    protected final void onMessage (I message)
            throws Throwable
    {
        if (initial == null)
        {
            initial = initial();
            current = initial;
        }

        try
        {
            current = current.onMessage(message);
        }
        catch (Throwable ex1)
        {
            try
            {
                current = onError(current, ex1);
            }
            catch (Throwable ex2)
            {
                current = initial;
            }
        }
    }

    /**
     * Specifies the state to goto, given the current state,
     * when an unhandled exception occurs in the current state.
     *
     * <p>
     * Case should be taken to ensure that this method never
     * throws an exception itself; otherwise, the state-machine
     * will suppress the exception and return to the initial state.
     * </p>
     *
     * @param state is the current state.
     * @param cause is the unhandled exception.
     * @return the next state.
     */
    protected State<I> onError (final State<I> state,
                                final Throwable cause)
    {
        return initial;
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