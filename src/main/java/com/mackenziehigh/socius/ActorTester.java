package com.mackenziehigh.socius;

import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mackenziehigh.cascade.Cascade.AbstractStage;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.BooleanSupplier;

/**
 *
 */
public final class ActorTester
{
    @FunctionalInterface
    public interface Step
    {
        public void run ()
                throws Throwable;
    }

    private final SyncStage stage = new SyncStage();

    private final List<Step> steps = Lists.newLinkedList();

    private final Map<Output<?>, BlockingDeque<Object>> expectedOutputs = Maps.newConcurrentMap();

    private final Map<Output<?>, BlockingDeque<Object>> actualOutputs = Maps.newConcurrentMap();

    public Stage stage ()
    {
        return stage;
    }

    public <I, O> ActorTester execute (final Step task)
    {
        steps.add(task);
        return this;
    }

    public <I, O> ActorTester send (final Actor<I, O> actor,
                                       final I message)
    {
        return send(actor.input(), message);
    }

    public <I> ActorTester send (final Actor.Input<I> input,
                                    final I message)
    {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(message, "message");
        steps.add(() -> input.send(message));
        return this;
    }

    public ActorTester require (final BooleanSupplier condition)
    {
        Objects.requireNonNull(condition, "condition");
        steps.add(() -> Verify.verify(condition.getAsBoolean(), "Violation of Requirement"));
        return this;
    }

    public <I, O> ActorTester expect (final Actor<I, O> actor,
                                         final O message)
    {
        return expect(actor.output(), message);
    }

    public <O> ActorTester expect (final Actor.Output<O> output,
                                      final O message)
    {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(message, "message");

        if (expectedOutputs.containsKey(output) == false)
        {
            expectedOutputs.put(output, new LinkedBlockingDeque<>());
            actualOutputs.put(output, new LinkedBlockingDeque<>());
            final Actor<O, ?> sink = stage.newActor().withScript((O x) -> actualOutputs.get(output).add(x)).create();
            sink.input().connect(output);
        }

        expectedOutputs.get(output).add(message);

        final Step step = () ->
        {
            final BlockingDeque<Object> queue1 = expectedOutputs.get(output);
            final BlockingDeque<Object> queue2 = actualOutputs.get(output);

            final Object expected = expectedOutputs.get(output).pollFirst();
            final Object actual = actualOutputs.get(output).pollFirst();

            Verify.verify(Objects.equals(expected, actual), "expected (%s) != actual (%s)", expected, actual);
        };

        steps.add(step);

        return this;
    }

    public ActorTester requireEmptyOutputs ()
    {
        steps.add(() -> actualOutputs.values().forEach(x -> Verify.verify(x.isEmpty(), "Non Empty Output: %s", x)));
        return this;
    }

    public ActorTester dumpOutputs ()
    {
        steps.add(() -> actualOutputs.values().forEach(System.out::println));
        return this;
    }

    public ActorTester println (final String line)
    {
        steps.add(() -> System.out.println(line));
        return this;
    }

    public ActorTester printOutput (final Output<?> output)
    {
        steps.add(() -> System.out.println(actualOutputs.get(output)));
        return this;
    }

    public ActorTester printOutput (final Actor<?, ?> actor)
    {
        return printOutput(actor.output());
    }

    public void run ()
            throws Throwable
    {
        try
        {
            for (Step step : steps)
            {
                step.run();
                stage.run();
            }
        }
        finally
        {
            stage.close();
        }
    }

    private final class SyncStage
            extends AbstractStage
    {
        private final Queue<AbstractStage.ActorTask> tasks = Queues.newConcurrentLinkedQueue();

        @Override
        protected void onSubmit (final AbstractStage.ActorTask task)
        {
            tasks.add(task);
        }

        @Override

        protected void onStageClose ()
        {
            // Pass.
        }

        public void run ()
        {
            while (tasks.isEmpty() == false)
            {
                tasks.poll().run();
            }
        }
    };
}
