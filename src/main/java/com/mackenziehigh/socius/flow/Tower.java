package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Routes an incoming message to a corresponding pipeline (floor) based on a key therein,
 * creating the pipeline (floor) when the key is received for the first time.
 *
 * <p>
 * <b>Warning:</b> This class can cause memory leaks, if floors are continually added.
 * Floors will be added, as needed, when messages arrive with news keys.
 * However, floors will never be removed, because they may still be in-use.
 * Thus, this class is intended for use when the number of floors is known in advance.
 * </p>
 *
 * @param <K> is the type of the keys that identify the floors.
 * @param <I> is the type of the incoming messages.
 * @param <O> is the type of the outgoing messages.
 */
public final class Tower<K, I, O>
        implements DataPipeline<I, O>
{
    /**
     * This map maps keys to corresponding pipelines (floors).
     */
    private final Map<K, DataPipeline<I, O>> floors = new ConcurrentHashMap<>();

    /**
     * This processors receives the incoming messages
     * and routes them to the appropriate floor,
     * creating the floor if it does not exist.
     */
    private final Processor<I> procIn;

    /**
     * This processor funnels the outgoing messages
     * from the floors into a single output.
     */
    private final Processor<O> procOut;

    /**
     * Given an incoming message, this function extracts the key,
     * which identifies the floor corresponding thereto.
     */
    private final Function<I, K> keyFunction;

    /**
     * This function is used to construct new floors
     * given the keys that will identify them herein.
     */
    private final Function<K, DataPipeline<I, O>> floorBuilder;

    private Tower (final Stage stage,
                   final Function<I, K> keyFunction,
                   final Function<K, DataPipeline<I, O>> floorBuilder)
    {
        this.floorBuilder = Objects.requireNonNull(floorBuilder, "floorBuilder");
        this.keyFunction = Objects.requireNonNull(keyFunction, "keyFunction");
        this.procIn = Processor.newConsumer(stage, this::onMessage);
        this.procOut = Processor.newConnector(stage);
    }

    /**
     * Factory Method.
     *
     * @param <K> is the type of the keys that identify the floors.
     * @param <I> is the type of the incoming messages.
     * @param <O> is the type of the outgoing messages.
     * @param stage will be used to create private actors.
     * @param keyFunction will be used to extract the keys from incoming messages.
     * @param floorBuilder will be used to create new floors, as needed.
     * @return the newly constructed object.
     */
    public static <K, I, O> Tower<K, I, O> newTower (final Stage stage,
                                                     final Function<I, K> keyFunction,
                                                     final Function<K, DataPipeline<I, O>> floorBuilder)
    {
        return new Tower<>(stage, keyFunction, floorBuilder);
    }

    private void onMessage (final I message)
    {
        /**
         * Obtain the key that identifies the corresponding floor.
         */
        final K key = keyFunction.apply(message);

        /**
         * If the floor does not exist, then create it.
         */
        if (floors.containsKey(key) == false)
        {
            final DataPipeline<I, O> floor = floorBuilder.apply(key);
            floor.dataOut().connect(procOut.dataIn());
            floors.put(key, floor);
        }

        /**
         * Send the message to the corresponding floor.
         */
        final DataPipeline<I, O> dest = floors.get(key);
        dest.accept(message);
    }

    /**
     * Input Connection.
     *
     * @return the input connector that supplies messages to the tower.
     */
    @Override
    public Input<I> dataIn ()
    {
        return procIn.dataIn();
    }

    /**
     * Output Connection.
     *
     * @return the output connector that receives messages from the tower.
     */
    @Override
    public Output<O> dataOut ()
    {
        return procOut.dataOut();
    }

}
