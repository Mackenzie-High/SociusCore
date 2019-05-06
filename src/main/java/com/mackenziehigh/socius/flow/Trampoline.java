package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.ActorFactory;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Context;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Objects;

/**
 * A state-machine implementation based on trampolining.
 *
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public final class Trampoline<I, O>
        implements DataPipeline<I, O>
{
    /**
     * Defines the states and transitions.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     */
    public interface Script<I, O>
    {
        /**
         * Specifies the initial state of the state-machine.
         *
         * @return the initial state.
         */
        public State<I, O> initial ();

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
         * @param source is the current state.
         * @param cause is the unhandled exception.
         * @return the next state.
         */
        public State<I, O> error (State<I, O> source,
                                  Throwable cause);
    }

    /**
     * Signature of a state transition function.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     */
    @FunctionalInterface
    public interface State<I, O>
    {
        public State<I, O> transition (Context<I, O> context,
                                       I message);
    }

    /**
     * This script defines the states and transitions used by this state-machine.
     */
    private final Script<I, O> script;

    /**
     * This is the initial state that the state-machine is in.
     */
    private final State<I, O> initial;

    /**
     * This is the current state that the state-machine is in.
     */
    private State<I, O> current;

    /**
     * This actor provides the input connector.
     */
    private final Actor<I, O> procIn;

    /**
     * This actor provides the output connector.
     */
    private final Processor<O> procOut;

    private Trampoline (final ActorFactory stage,
                         final Script<I, O> script)
    {
        this.script = Objects.requireNonNull(script, "script");
        this.procIn = stage.newActor().withContextScript(this::onMessage).create();
        this.procOut = Processor.fromIdentityScript(stage);
        this.procIn.output().connect(procOut.dataIn());
        this.initial = script.initial();
        this.current = initial;
    }

    private void onMessage (final Context<I, O> context,
                            final I message)
            throws Throwable
    {
        try
        {
            current = current.transition(context, message);
        }
        catch (Throwable ex1)
        {
            try
            {
                current = script.error(current, ex1);
            }
            catch (Throwable ex2)
            {
                current = initial;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input<I> dataIn ()
    {
        return procIn.input();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Output<O> dataOut ()
    {
        return procOut.dataOut();
    }

    /**
     * Get the state that the state-machine is currently in.
     *
     * @return the current state.
     */
    public State<I, O> state ()
    {
        return current;
    }

    /**
     * Set the state that the state-machine is currently in.
     *
     * @param state is the state that the machine will be in.
     * @return the new state.
     */
    public State<I, O> state (final State<I, O> state)
    {
        current = Objects.requireNonNull(state, "state");
        return state;
    }

    /**
     * Factory method.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     * @param stage will be used to create private actors.
     * @param script defines the states and transitions of this machine.
     * @return the new state-machine.
     */
    public static <I, O> Trampoline<I, O> newTrampolineMachine (final ActorFactory stage,
                                                                 final Script<I, O> script)
    {
        return new Trampoline<>(stage, script);
    }

}
