package com.mackenziehigh.socius.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Simple Inserter.
 */
public final class SimpleInserter<T>
{
    private final Actor<T, T> dataIn;

    private final Actor<T, T> dataOut;

    private final Actor<T, T> selectionsOut;

    private final List<Predicate<T>> actions;

    private SimpleInserter (final Stage stage,
                            final List<Predicate<T>> actions)
    {
        this.actions = ImmutableList.copyOf(actions);
        this.dataIn = stage.newActor().withScript(this::onMessage).create();
        this.dataOut = stage.newActor().withScript((T x) -> x).create();
        this.selectionsOut = stage.newActor().withScript((T x) -> x).create();
    }

    private void onMessage (final T message)
    {
        for (Predicate<T> action : actions)
        {
            if (action.test(message))
            {
                selectionsOut.accept(message);
                return;
            }
        }

        dataOut.accept(message);
    }

    public Input<T> dataIn ()
    {
        return dataIn.input();
    }

    public Output<T> dataOut ()
    {
        return dataOut.output();
    }

    public Output<T> selectionsOut ()
    {
        return selectionsOut.output();
    }

    public static <T> Builder<T> newInserter (final Stage stage)
    {
        return new Builder<>(stage);
    }

    public static final class Builder<T>
    {
        private final Stage stage;

        private final List<Predicate<T>> actions = Lists.newLinkedList();

        private Builder (final Stage stage)
        {
            this.stage = Objects.requireNonNull(stage, "stage");
        }

        public Builder<T> selectIf (final Predicate<T> condition)
        {
            actions.add(condition);
            return this;
        }

        public SimpleInserter<T> build ()
        {
            return new SimpleInserter<>(stage, actions);
        }
    }
}
