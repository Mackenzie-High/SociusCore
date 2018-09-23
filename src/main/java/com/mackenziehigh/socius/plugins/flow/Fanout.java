package com.mackenziehigh.socius.plugins.flow;

import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.plugins.Clock;
import com.mackenziehigh.socius.plugins.Printer;
import java.time.Instant;
import java.util.Map;

/**
 *
 */
public final class Fanout<T>
{
    private final Actor<T, T> recvActor;

    private final Map<String, Actor<T, T>> outputs = Maps.newConcurrentMap();

    private final Object lock = new Object();

    private Fanout (final Stage stage)
    {
        this.recvActor = stage.newActor().withScript(this::send).create();
    }

    private void send (final T message)
    {
        outputs.values().forEach(x -> x.accept(message));
    }

    public Input<T> input ()
    {
        return recvActor.input();
    }

    public Output<T> output (final String name)
    {
        synchronized (lock)
        {
            if (outputs.containsKey(name) == false)
            {
                final Actor<T, T> identityActor = recvActor.stage().newActor().withScript((T x) -> x).create();
                outputs.put(name, identityActor);
            }
        }

        return outputs.get(name).output();
    }

    public static <T> Fanout<T> newFanout (final Stage stage)
    {
        return new Fanout<>(stage);
    }

    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();
        final Clock clock = new Clock(stage).period(1000).start();
        final Fanout<Instant> fanout = Fanout.newFanout(stage);
        final Printer printer1 = new Printer(stage);
        printer1.format("X = %s");
        final Printer printer2 = new Printer(stage);
        printer2.format("Y = %s");

        clock.clockOut().connect(fanout.input());
        fanout.output("UTC").connect(printer1.dataIn());
        fanout.output("EST").connect(printer2.dataIn());
    }
}
