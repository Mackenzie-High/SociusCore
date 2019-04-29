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
     * Signature of a state transition function.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     */
    @FunctionalInterface
    public interface State<I, O>
    {
        public State<I, O> onMessage (Context<I, O> context,
                                      I message);
    }

    /**
     * This is the initial state that the state-machine will be in.
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
                        final State<I, O> initial)
    {
        this.initial = Objects.requireNonNull(initial, "initial");
        this.current = initial;
        this.procIn = stage.newActor().withContextScript(this::onMessage).create();
        this.procOut = Processor.fromIdentityScript(stage);
        this.procIn.output().connect(procOut.dataIn());
    }

    private void onMessage (final Context<I, O> context,
                            final I message)
            throws Throwable
    {
        try
        {
            current = current.onMessage(context, message);
        }
        catch (Throwable ex)
        {
            current = initial;
            throw ex;
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
     * @param initial is the state that the machine will be in initially.
     * @return the new state-machine.
     */
    public static <I, O> Trampoline<I, O> newTrampolineMachine (final ActorFactory stage,
                                                                final State<I, O> initial)
    {
        return new Trampoline<>(stage, initial);
    }

}
