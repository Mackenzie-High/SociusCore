package com.mackenziehigh.socius.plugins.flow;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.function.Predicate;

/**
 * Route messages bases on a boolean condition.
 *
 * @param <T> is the type of messages that flow through the if-else router.
 */
public final class IfElse<T>
{
    private final Predicate<T> condition;

    private final Processor<T> checker;

    private final Processor<T> trueOut;

    private final Processor<T> falseOut;

    private IfElse (final Stage stage,
                    final Predicate<T> condition)
    {
        this.condition = condition;
        this.checker = Processor.newProcessor(stage, this::onMessage);
        this.trueOut = Processor.newProcessor(stage);
        this.falseOut = Processor.newProcessor(stage);
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
    public Input<T> dataIn ()
    {
        return checker.dataIn();
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
