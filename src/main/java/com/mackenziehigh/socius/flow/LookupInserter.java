package com.mackenziehigh.socius.flow;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Conditionally routes messages based on an ordered series of option predicates.
 */
public final class LookupInserter<T>
{
    private final Processor<T> procDataIn;

    private final Processor<T> procDataOut;

    private final List<Tuple2<Predicate<T>, Input<T>>> routes;

    private LookupInserter (final Builder<T> builder)
    {
        this.procDataIn = Processor.newProcessor(builder.stage, this::onMessage);
        this.procDataOut = Processor.newProcessor(builder.stage);
        this.routes = ImmutableList.copyOf(builder.routes);
    }

    private void onMessage (final T message)
    {
        for (Tuple2<Predicate<T>, Input<T>> route : routes)
        {
            if (route._1().test(message))
            {
                route._2().send(message);
                return;
            }
        }

        procDataOut.dataIn().send(message);
    }

    public Stage.Actor.Input<T> dataIn ()
    {
        return procDataIn.dataIn();
    }

    public Output<T> dataOut ()
    {
        return procDataOut.dataOut();
    }

    public static <T> Builder<T> newLookupInserter (final Stage stage)
    {
        return new Builder(stage);
    }

    public static final class Builder<T>
    {
        private final Stage stage;

        private final List<Tuple2<Predicate<T>, Input<T>>> routes = Lists.newLinkedList();

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        public Builder withRoute (final Input<T> output,
                                  final Predicate<T> condition)
        {
            Objects.requireNonNull(output, "output");
            Objects.requireNonNull(condition, "condition");
            routes.add(Tuple.of(condition, output));
            return this;
        }

        public LookupInserter build ()
        {
            return new LookupInserter(this);
        }
    }
}
