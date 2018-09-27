package com.mackenziehigh.socius.testing;

import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 *
 */
public final class ReactionTester
        implements AutoCloseable
{
    @FunctionalInterface
    public interface Step
    {
        public void run ()
                throws Throwable;
    }

    private final Stage stage = Cascade.newStage();

    private final List<Step> steps = Lists.newLinkedList();

    private final Map<Output<?>, BlockingDeque<Object>> expectedOutputs = Maps.newConcurrentMap();

    private final Map<Output<?>, BlockingDeque<Object>> actualOutputs = Maps.newConcurrentMap();

    private final Map<Output<?>, BlockingDeque<Object>> outputSinks = Maps.newConcurrentMap();

    public Stage stage ()
    {
        return stage;
    }

    public <I, O> ReactionTester execute (final Step task)
    {
        steps.add(task);
        return this;
    }

    public <I, O> ReactionTester send (final Actor<I, O> actor,
                                       final I message)
    {
        return send(actor.input(), message);
    }

    public <I> ReactionTester send (final Actor.Input<I> input,
                                    final I message)
    {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(message, "message");
        steps.add(() -> input.send(message));
        return this;
    }

    public ReactionTester require (final BooleanSupplier condition)
    {
        Objects.requireNonNull(condition, "condition");
        steps.add(() -> Verify.verify(condition.getAsBoolean(), "Violation of Requirement"));
        return this;
    }

    public <I, O> ReactionTester expect (final Actor<I, O> actor,
                                         final O message)
    {
        return expect(actor.output(), message);
    }

    public <O> ReactionTester expect (final Actor.Output<O> output,
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
            final Object actual = actualOutputs.get(output).pollFirst(1, TimeUnit.SECONDS);

            Verify.verify(Objects.equals(expected, actual), "expected (%s) != actual (%s)", expected, actual);
        };

        steps.add(step);

        return this;
    }

    public ReactionTester requireEmptyOutputs ()
    {
        steps.add(() -> expectedOutputs.values().forEach(x -> Verify.verify(x.isEmpty(), "Non Empty Output")));
        return this;
    }

    public void run ()
            throws Throwable
    {
        try
        {
            for (Step step : steps)
            {
                step.run();
            }
        }
        finally
        {
            stage.close();
        }
    }

    @Override
    public void close ()
    {
        stage.close();
    }
}
