package com.mackenziehigh.socius;

/*
 * Copyright 2019 Michael Mackenzie High
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mackenziehigh.cascade.Cascade.AbstractStage;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * (Experimental API) Provides a mechanism for synchronously testing actors in unit-tests.
 *
 * <p>
 * This API is still semi-unstable and may change without notice.
 * Refactoring, etc, may be needed in the future, if you use this API.
 * </p>
 */
public final class ActorTester
{
    /**
     * A step to perform during the test.
     */
    @FunctionalInterface
    public interface Step
    {
        public void run ()
                throws Throwable;
    }

    /**
     * Thrown whenever a step fails to execute successfully.
     */
    public final class StepException
            extends RuntimeException
    {
        private StepException (final String message)
        {
            super(message);
        }

        private StepException (final StackTraceElement location,
                               final Throwable cause)
        {
            super(String.format("(class = %s, method = %s, line = %d): %s", location.getClassName(), location.getMethodName(), location.getLineNumber(), cause.getMessage()), cause);
        }
    }

    private final AtomicBoolean closed = new AtomicBoolean();

    private final SyncStage stage = new SyncStage();

    private final List<Step> steps = Lists.newLinkedList();

    private final Map<Output<?>, BlockingDeque<Object>> actualOutputs = Maps.newConcurrentMap();

    public Stage stage ()
    {
        return stage;
    }

    /**
     * Connect an output to the tester.
     *
     * @param <O> is the type of messages provided by the output.
     * @param output will be connected hereto.
     * @return this.
     */
    public <O> ActorTester connect (final Output<O> output)
    {
        requireOpen();
        requireSyncActorFactory(output.actor());
        Objects.requireNonNull(output, "output");

        if (actualOutputs.containsKey(output) == false)
        {
            actualOutputs.put(output, new LinkedBlockingDeque<>());
            final Actor<O, ?> sink = stage.newActor().withConsumerScript((O x) -> actualOutputs.get(output).add(x)).create();
            sink.input().connect(output);
        }

        return this;
    }

    /**
     * Add a step to execute, when the test is run.
     *
     * @param task will be executed during the test.
     * @return this.
     */
    public ActorTester execute (final Step task)
    {
        requireOpen();
        Objects.requireNonNull(task, "task");
        steps.add(new WrapperStep(task));
        return this;
    }

    /**
     * Add a step to execute, when the test is run.
     *
     * @param <I> is the type of the message to send.
     * @param input will receive the message.
     * @param message will be sent to the input during the test.
     * @return this.
     */
    public <I> ActorTester send (final Input<I> input,
                                 final I message)
    {
        requireOpen();
        requireSyncActorFactory(input.actor());
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(message, "message");
        execute(() -> input.send(message));
        return this;
    }

    /**
     * Add a step to execute, when the test is run.
     *
     * @param condition must be true during the test.
     * @return this.
     */
    public ActorTester require (final BooleanSupplier condition)
    {
        require(condition, "Violation of Requirement");
        return this;
    }

    /**
     * Add a step to execute, when the test is run.
     *
     * @param condition must be true during the test.
     * @param error is the error-message to use, if the requirement fails.
     * @return this.
     */
    public ActorTester require (final BooleanSupplier condition,
                                final String error)
    {
        requireOpen();
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(error, "error");
        final Step step = () ->
        {
            if (condition.getAsBoolean() == false)
            {
                throw new StepException(error);
            }
        };
        execute(step);
        return this;
    }

    /**
     * Add a step to execute, when the test is run.
     *
     * @param <O> is the type of the message to receive.
     * @param output will provide the message.
     * @param message is the expected message.
     * @return this.
     */
    public <O> ActorTester expect (final Output<O> output,
                                   final O message)
    {
        requireOpen();
        requireSyncActorFactory(output.actor());
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(message, "message");
        Preconditions.checkArgument(output.actor().stage().equals(stage), "Wrong ActorFactory");

        connect(output);

        final Step step = () ->
        {
            final Object actual = actualOutputs.get(output).pollFirst();

            if (Objects.equals(message, actual) == false)
            {
                final String error = String.format("expected (%s) != actual (%s)", message, actual);
                throw new StepException(error);
            }
        };

        execute(step);

        return this;
    }

    /**
     * Add a step to execute, when the test is run.
     *
     * @param <O> is the type of the message to receive.
     * @param output will provide the message.
     * @param condition is the expected message.
     * @param error is the error-message to use, if the requirement fails.
     * @return this.
     */
    public <O> ActorTester expectLike (final Output<O> output,
                                       final Predicate<O> condition,
                                       final String error)
    {
        requireOpen();
        requireSyncActorFactory(output.actor());
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(condition, "condition");
        Preconditions.checkArgument(output.actor().stage().equals(stage), "Wrong ActorFactory");

        connect(output);

        final Step step = () ->
        {
            final O actual = (O) actualOutputs.get(output).pollFirst();

            if (condition.test(actual) == false)
            {
                throw new StepException(error);
            }
        };

        execute(step);

        return this;
    }

    /**
     * Add a step to execute, when the test is run.
     *
     * @return this.
     */
    public ActorTester requireEmptyOutputs ()
    {
        requireOpen();
        execute(() -> actualOutputs.values().forEach(x -> Verify.verify(x.isEmpty(), "Non Empty Output: %s", x)));
        return this;
    }

    /**
     * Execute all the steps in the test.
     *
     * @throws Throwable if one of the steps throws an exception.
     */
    public void run ()
            throws Throwable
    {
        requireOpen();

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
            closed.set(true);
            stage.close();
        }
    }

    private void requireOpen ()
    {
        if (closed.get())
        {
            throw new IllegalStateException(getClass().getSimpleName() + " is already closed!");
        }
    }

    private void requireSyncActorFactory (final Actor<?, ?> actor)
    {
        if (actor.stage().equals(stage) == false)
        {
            throw new IllegalArgumentException("The actor is on the wrong stage!");
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

    private final class WrapperStep
            implements Step
    {
        private final StackTraceElement location;

        private final Step delegate;

        public WrapperStep (final Step delegate)
        {
            // Find out where the step was created, which s outside of these classes.
            location = Arrays
                    .asList(Thread.currentThread().getStackTrace())
                    .stream()
                    .filter(x -> x.getLineNumber() >= 0)
                    .filter(x -> x.getClassName().startsWith("java.") == false)
                    .filter(x -> x.getClassName().startsWith(ActorTester.class.getName() + "$") == false)
                    .filter(x -> x.getClassName().equals(ActorTester.class.getName()) == false)
                    .findFirst()
                    .get();

            this.delegate = delegate;
        }

        @Override
        public void run ()
                throws Throwable
        {
            try
            {
                delegate.run();
            }
            catch (Throwable ex)
            {

                throw new StepException(location, ex);
            }
        }
    }
}
