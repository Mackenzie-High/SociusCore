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
 * @author mackenzie
 */
public final class TrampolineMachine<I, O>
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

    private TrampolineMachine (final ActorFactory stage,
                               final State<I, O> initial)
    {
        this.initial = Objects.requireNonNull(initial, "initial");
        this.current = initial;
        this.procIn = stage.newActor().withContextScript(this::onMessage).create();
        this.procOut = Processor.newConnector(stage);
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

    public static <I, O> TrampolineMachine<I, O> newTrampolineMachine (final ActorFactory stage,
                                                                       final State<I, O> initial)
    {
        return new TrampolineMachine<>(stage, initial);
    }

    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();
        final TrampolineMachine<Integer, String> sm = TrampolineMachine.newTrampolineMachine(stage, TrampolineMachine::state1);
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
            return TrampolineMachine::state1;
        }
        else
        {
            return TrampolineMachine::state2;
        }
    }

    private static State<Integer, String> state2 (final Context<Integer, String> ctx,
                                                  final Integer message)
    {
        if (message < 100)
        {
            System.err.println("B = " + message);
            return TrampolineMachine::state2;
        }
        else
        {
            return TrampolineMachine::state1;
        }
    }
}
