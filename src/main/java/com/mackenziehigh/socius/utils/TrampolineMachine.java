package com.mackenziehigh.socius.utils;

/**
 * A mix-in style interface for creating trampoline based state-machines.
 *
 * <p>
 * Usually, a state is just a private method in the implementing class.
 * The state performs whatever work that it wishes to perform.
 * Then, the state returns the next state as its return-value.
 * The return can be concisely performed using method-references.
 * The trampoline will then invoke the returned method-reference,
 * which conceptually causes the state-machine to enter the next state.
 * </p>
 *
 * <p>
 * The state-machine terminates execution when a state returns null.
 * </p>
 */
public interface TrampolineMachine
        extends Runnable
{
    /**
     * This is the signature of the private method-references
     * that implement the states of the state-machine.
     */
    @FunctionalInterface
    public interface State
    {
        public State execute ()
                throws Throwable;
    }

    /**
     * This method will be invoked by the <code>run()</code> method
     * in order to ascertain the first state of the state-machine.
     *
     * @return the initial state of the state-machine.
     */
    public State initial ();

    /**
     * This method will be invoked by the <code>run()</code> method
     * in order to ascertain handle uncaught exceptions, if any.
     *
     * <p>
     * By default, this method prints the stack-trace to standard-error
     * and then returns null, which causes the state-machine to terminate.
     * </p>
     *
     * @param cause was thrown by a state in the state-machine.
     * @return the state to enter next in lieu of the exception.
     */
    public default State onUncaughtException (final Throwable cause)
    {
        cause.printStackTrace(System.err);
        return null;
    }

    /**
     * This method executes the states in the state-machine using a trampoline.
     *
     * <p>
     * You should <b>not</b> override this method.
     * </p>
     */
    @Override
    public default void run ()
    {
        State state = initial();

        while (state != null)
        {
            try
            {
                state = state.execute();
            }
            catch (Throwable ex)
            {
                try
                {
                    state = onUncaughtException(ex);
                }
                catch (Throwable ex2)
                {
                    state = null;
                }
            }
        }
    }
}
