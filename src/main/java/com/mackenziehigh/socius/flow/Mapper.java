package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;

/**
 * Applies a map-function to incoming messages and then forwards the results,
 * such that the output data-type is the same as the input data-type.
 *
 * <p>
 * If the map-function returns null, then no message will be forwarded.
 * Thus, in effect, returning null causes the <code>Processor</code> to act as a <code>Filter</code>.
 * </p>
 *
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public final class Mapper<I, O>
{
    private final Actor<I, O> actor;

    private Mapper (final Actor<I, O> actor)
    {
        this.actor = actor;
    }

    /**
     * Input Connection.
     *
     * @return the input that supplies the messages to be processed.
     */
    public Input<I> dataIn ()
    {
        return actor.input();
    }

    /**
     * Output Connection.
     *
     * @return the output that receives the results of processing the messages.
     */
    public Output<O> dataOut ()
    {
        return actor.output();
    }

    /**
     * Factory Method.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     * @param stage will be used t create private actors.
     * @param script defines the processing to perform.
     * @return the new processor.
     */
    public static <I, O> Mapper<I, O> newMapper (final Stage stage,
                                                 final Actor.FunctionScript<I, O> script)
    {
        return new Mapper<>(stage.newActor().withScript(script).create());
    }

    /**
     * Factory Method.
     *
     * @param <I> is the type of the incoming messages.
     * @param stage will be used t create private actors.
     * @param script defines the processing to perform.
     * @return the new processor.
     */
    public static <I> Mapper<I, I> newMapper (final Stage stage,
                                              final Actor.ConsumerScript<I> script)
    {
        return new Mapper<>(stage.newActor().withScript(script).create());
    }

    /**
     * Creates a new identity processor.
     *
     * <p>
     * An identity processor merely forwards incoming messages to
     * the output without performing any actual transformation, etc.
     * </p>
     *
     * @param <I> is the type of the incoming messages.
     * @param stage will be used t create private actors.
     * @return the new processor.
     */
    public static <I> Mapper<I, I> newMapper (final Stage stage)
    {
        return new Mapper<>(stage.newActor().withScript((I x) -> x).create());
    }

}
