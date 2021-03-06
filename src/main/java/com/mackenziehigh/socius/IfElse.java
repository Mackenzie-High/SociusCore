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
package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Route messages bases on a boolean condition.
 *
 * @param <T> is the type of messages that flow through the if-else router.
 */
public final class IfElse<T>
        implements Processor<T>
{
    /**
     * This is the user-defined condition that determines
     * whether messages are routed from the data-input to
     * either the true-output or the false-output.
     */
    private final Predicate<T> condition;

    /**
     * Provides the data-input connector.
     */
    private final Processor<T> checker;

    /**
     * Provides the true-output connector.
     */
    private final Processor<T> trueOut;

    /**
     * Provides the false-output connector.
     */
    private final Processor<T> falseOut;

    private IfElse (final Stage stage,
                    final Predicate<T> condition)
    {
        Objects.requireNonNull(stage, "stage");
        this.condition = Objects.requireNonNull(condition, "condition");
        this.checker = Processor.fromConsumerScript(stage, this::onMessage);
        this.trueOut = Processor.fromIdentityScript(stage);
        this.falseOut = Processor.fromIdentityScript(stage);
    }

    private void onMessage (final T message)
    {
        if (condition.test(message))
        {
            trueOut.dataIn().send(message);
        }
        else
        {
            falseOut.dataIn().send(message);
        }
    }

    /**
     * Get the input that supplies messages hereto.
     *
     * @return the only input.
     */
    @Override
    public Input<T> dataIn ()
    {
        return checker.dataIn();
    }

    /**
     * Equivalent: <code>trueOut()</code>.
     *
     * @return the equivalent output.
     */
    @Override
    public Output<T> dataOut ()
    {
        return trueOut();
    }

    /**
     * Get the output that receives the messages
     * for which the predicate returned true.
     *
     * @return the matching messages.
     */
    public Output<T> trueOut ()
    {
        return trueOut.dataOut();
    }

    /**
     * Get the output that receives the messages
     * for which the predicate returned false.
     *
     * @return the matching messages.
     */
    public Output<T> falseOut ()
    {
        return falseOut.dataOut();
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of messages that flow through the if-else router.
     * @param stage will be used to create private actors.
     * @param condition will be used to decide which route messages take.
     * @return this.
     */
    public static <T> IfElse<T> newIfElse (final Stage stage,
                                           final Predicate<T> condition)
    {
        return new IfElse<>(stage, condition);
    }
}
