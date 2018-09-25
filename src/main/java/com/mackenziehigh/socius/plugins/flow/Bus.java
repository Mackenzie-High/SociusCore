package com.mackenziehigh.socius.plugins.flow;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.plugins.io.Printer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public final class Bus<M>
{
    private final Stage stage;

    private final Actor<M, M> hub;

    private final Map<Object, Actor<M, M>> inputs = new ConcurrentHashMap<>();

    private final Map<Object, Actor<M, M>> outputs = new ConcurrentHashMap<>();

    private Bus (final Stage stage)
    {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.hub = stage.newActor().withScript(this::forwardFromHub).withLinkedInflowQueue().create();
    }

    public synchronized Input<M> input (final Object key)
    {
        if (inputs.containsKey(key) == false)
        {
            final Actor<M, M> actor = stage.newActor().withScript(this::forwardToHub).create();
            inputs.put(key, actor);
        }

        final Actor<M, M> actor = inputs.get(key);

        return actor.input();
    }

    public synchronized Output<M> output (final Object key)
    {
        if (outputs.containsKey(key) == false)
        {
            final Actor<M, M> actor = stage.newActor().withScript((M x) -> x).create();
            outputs.put(key, actor);
        }

        final Actor<M, M> actor = outputs.get(key);

        return actor.output();
    }

    private void forwardToHub (final M message)
    {
        hub.accept(message);
    }

    private void forwardFromHub (final M message)
    {
        outputs.values().stream().forEach(x -> x.accept(message));
    }

    public static <M> Bus<M> newBus (final Stage stage)
    {
        return new Bus<>(stage);
    }

    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();
        final Bus<Integer> bus = Bus.newBus(stage);

        final Printer p = new Printer(stage);
        p.format("X = %s");
        bus.output("p").connect(p.dataIn());
        final Printer q = new Printer(stage);
        q.format("Y = %s");
        bus.output("q").connect(q.dataIn());

        bus.input("x").send(100);
    }

}
