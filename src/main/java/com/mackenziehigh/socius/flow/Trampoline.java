package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.ActorFactory;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Context;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.io.Printer;
import java.util.Objects;

/**
 *
 */
public final class Trampoline<I, O>
        implements DataPipeline<I, O>
{
    public interface State<I, O>
    {
        public State<I, O> onMessage (Context<I, O> context,
                                      I message);
    }

    private final State<I, O> initial;

    private State<I, O> current;

    private final Actor<I, O> procIn;

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

    @Override
    public Input<I> dataIn ()
    {
        return procIn.input();
    }

    @Override
    public Output<O> dataOut ()
    {
        return procOut.dataOut();
    }

    public State<I, O> state ()
    {
        return current;
    }

    public State<I, O> state (final State<I, O> state)
    {
        current = state;
        return state;
    }

    public static <I, O> Trampoline<I, O> newTrampolineMachine (final ActorFactory stage,
                                                                final State<I, O> initial)
    {
        return new Trampoline<>(stage, initial);
    }

    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();
        final Trampoline<Integer, String> sm = Trampoline.newTrampolineMachine(stage, Trampoline::state1);
        final Printer<String> p = Printer.newPrintln(stage, "X = %s");
        sm.dataOut().connect(p.dataIn());

        for (int i = 0; i < 20; i++)
        {
            sm.accept(i);
        }
    }

    private static State<Integer, String> state1 (final Context<Integer, String> ctx,
                                                  final Integer message)
    {
        ctx.sendFrom("V" + message);

        if (message < 10)
        {
            System.err.println("A = " + message);
            return Trampoline::state1;
        }
        else
        {
            return Trampoline::state2;
        }
    }

    private static State<Integer, String> state2 (final Context<Integer, String> ctx,
                                                  final Integer message)
    {
        if (message < 100)
        {
            System.err.println("B = " + message);
            return Trampoline::state2;
        }
        else
        {
            return Trampoline::state1;
        }
    }
}
