package com.mackenziehigh.socius;

import com.google.common.collect.ImmutableList;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.List;

/**
 *
 */
public final class ConcurrentOptionList<I, O>
{
    public interface Option<I, O>
    {
        public boolean isMatch (I message);

        public Input<I> dataIn ();

        public Output<O> dataOut ();
    }

    private final List<Option<I, O>> options;

    private final Processor<I> router;

    private final Processor<I> deadDrop;

    private final Funnel<O> funnel;

    private ConcurrentOptionList (final Stage stage,
                                  final List<Option<I, O>> mappers)
    {
        this.router = Processor.newProcessor(stage, this::onInput);
        this.deadDrop = Processor.newProcessor(stage);
        this.funnel = Funnel.newFunnel(stage);
        this.options = ImmutableList.copyOf(mappers);

        for (Option<I, O> option : options)
        {
            option.dataOut().connect(funnel.dataIn(new Object()));
        }
    }

    private void onInput (final I message)
    {
        for (Option<I, O> option : options)
        {
            if (option.isMatch(message))
            {
                option.dataIn().send(message);
                return;
            }
        }

        deadDrop.dataIn().send(message);
    }

    public Input<I> dataIn ()
    {
        return router.dataIn();
    }

    public Output<O> dataOut ()
    {
        return funnel.dataOut();
    }

    public Output<I> dropsOut ()
    {
        return deadDrop.dataOut();
    }

}
