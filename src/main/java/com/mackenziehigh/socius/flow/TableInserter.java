package com.mackenziehigh.socius.flow;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Conditionally routes messages based on a table lookup.
 */
public final class TableInserter<T>
{
    private final Processor<T> procDataIn;

    private final Processor<T> procDataOut;

    private final Map<Object, Input<T>> routes;

    private final Function<T, Object> extractor;

    private TableInserter (final Builder<T> builder)
    {
        this.procDataIn = Processor.newProcessor(builder.stage, this::onMessage);
        this.procDataOut = Processor.newProcessor(builder.stage);
        this.routes = ImmutableMap.copyOf(builder.routes);
        this.extractor = builder.extractor;
    }

    private void onMessage (final T message)
    {
        final Object key = extractor.apply(message);

        final Input<T> route = routes.get(key);

        if (route == null)
        {
            procDataOut.dataIn().send(message);
        }
        else
        {
            route.send(message);
        }
    }

    public Input<T> dataIn ()
    {
        return procDataIn.dataIn();
    }

    public Output<T> dataOut ()
    {
        return procDataOut.dataOut();
    }

    public static <T> Builder<T> newTableInserter (final Stage stage)
    {
        return new Builder(stage);
    }

    public static final class Builder<T>
    {
        private final Stage stage;

        private Function<T, Object> extractor = null;

        private final Map<Object, Input<T>> routes = Maps.newHashMap();

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        public Builder<T> withKeyExtractor (final Function<T, Object> functor)
        {
            this.extractor = Objects.requireNonNull(functor, "functor");
            return this;
        }

        public Builder<T> withRoute (final Object key,
                                     final Input<T> output)
        {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(output, "output");
            routes.put(key, output);
            return this;
        }

        public TableInserter build ()
        {
            Objects.requireNonNull(extractor, "No key extractor function was specified.");
            return new TableInserter(this);
        }
    }

}
