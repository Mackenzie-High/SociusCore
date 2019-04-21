package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Set;

/**
 *
 * @author mackenzie
 * @param <I>
 * @param <O>
 * @param <T>
 */
public interface DataYard<I, O, T extends DataYard.Siding<I, O>>
        extends DataPipeline<I, O>
{
    /**
     * An option that may receive messages.
     *
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     */
    public interface Siding<I, O>
            extends DataPipeline<I, O>
    {
        /**
         * This input will receive messages that match this option.
         *
         * @return the data-input of this option-handler.
         */
        @Override
        public Input<I> dataIn ();

        /**
         * The option-handler will send responses via this output.
         *
         * @return the data-output of this option-handler.
         */
        @Override
        public Output<O> dataOut ();
    }

    public Set<T> options ();
}
