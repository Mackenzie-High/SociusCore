package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade.ActorFactory;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.ConsumerScript;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.ContextScript;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.FunctionScript;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Map;
import java.util.Objects;

/**
 * Applies a function to incoming messages and then forwards the results,
 * such that the output data-type may be different than the input data-type.
 *
 * <p>
 * If the function returns null or void, then no message will be forwarded.
 * Thus, in effect, returning null or void causes the <code>Mapper</code> to act as a filter.
 * </p>
 *
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public interface Pipeline<I, O>
        extends Sink<I>,
                Source<O>
{
    /**
     * Input Connection.
     *
     * @return the data-input that provides the messages to the pipeline.
     */
    @Override
    public Input<I> dataIn ();

    /**
     * Output Connection.
     *
     * @return the data-output that receives messages from the pipeline.
     */
    @Override
    public Output<O> dataOut ();

    /**
     * Factory Method.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     * @param actor will be wrapped in a pipeline facade.
     * @return the new pipeline.
     */
    public static <I, O> Pipeline<I, O> fromActor (final Actor<I, O> actor)
    {
        Objects.requireNonNull(actor, "actor");
        return fromIO(actor.input(), actor.output());
    }

    /**
     * Factory Method.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     * @param stage will be used to create private actors.
     * @param script defines the processing to perform.
     * @return the new pipeline.
     */
    public static <I, O> Pipeline<I, O> fromContextScript (final ActorFactory stage,
                                                           final ContextScript<I, O> script)
    {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(script, "script");
        return fromActor(stage.newActor().withContextScript(script).create());
    }

    /**
     * Factory Method.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     * @param stage will be used to create private actors.
     * @param script defines the processing to perform.
     * @return the new pipeline.
     */
    public static <I, O> Pipeline<I, O> fromFunctionScript (final ActorFactory stage,
                                                            final FunctionScript<I, O> script)
    {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(script, "script");
        return fromActor(stage.newActor().withFunctionScript(script).create());
    }

    /**
     * Factory Method.
     *
     * @param <I> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @param script defines the processing to perform.
     * @return the new pipeline.
     */
    public static <I> Pipeline<I, I> fromConsumerScript (final ActorFactory stage,
                                                         final ConsumerScript<I> script)
    {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(script, "script");
        return fromActor(stage.newActor().withConsumerScript(script).create());
    }

    /**
     * Creates a new identity processor.
     *
     * <p>
     * An identity processor merely forwards incoming messages to
     * the output without performing any actual transformation, etc.
     * </p>
     *
     * @param <I> is the type of the incoming and outgoing messages.
     * @param stage will be used to create private actors.
     * @return the new pipeline.
     */
    public static <I> Pipeline<I, I> fromIdentityScript (final ActorFactory stage)
    {
        Objects.requireNonNull(stage, "stage");
        return fromFunctionScript(stage, (I x) -> x);
    }

    /**
     * Factory Method.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     * @param input provides the incoming messages.
     * @param output receives the outgoing messages.
     * @return the new pipeline.
     */
    public static <I, O> Pipeline<I, O> fromIO (final Input<I> input,
                                                final Output<O> output)
    {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");

        return new Pipeline<I, O>()
        {
            @Override
            public Input<I> dataIn ()
            {
                return input;
            }

            @Override
            public Output<O> dataOut ()
            {
                return output;
            }
        };
    }

    /**
     * Factory Method.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     * @param stage will be used t create private actors.
     * @param mappings defines the the input and output pairings.
     * @param defaultValue is the default output, if the input is not in the mappings map.
     * @return the new processor.
     */
    public static <I, O> Pipeline<I, O> fromMap (final ActorFactory stage,
                                                 final Map<I, O> mappings,
                                                 final O defaultValue)
    {
        /**
         * The map may change during the call.
         * Therefore, get the output and store in a local,
         * rather than calling containsKey() and then get().
         * Moreover, this is slightly faster conceptually.
         */
        final FunctionScript<I, O> script = msg ->
        {
            final O output = mappings.get(msg);
            return output == null ? defaultValue : output;
        };

        return fromFunctionScript(stage, script);
    }
}
