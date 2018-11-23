package com.mackenziehigh.socius.core;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 *
 */
public final class Duplicator<T>
{
    private final Processor<T> dataIn;

    private final Processor<T> dataOut;

    private final Deque<T> sequence;

    private final int sequenceLen;

    private final int repeatCount;

    private Duplicator (final Stage stage,
                        final int sequenceLen,
                        final int repeatCount)
    {
        this.dataIn = Processor.newProcessor(stage, this::onMessage);
        this.dataOut = Processor.newProcessor(stage);
        this.sequence = new ArrayDeque<>(sequenceLen);
        this.sequenceLen = sequenceLen;
        this.repeatCount = repeatCount;
    }

    public Input<T> dataIn ()
    {
        return dataIn.dataIn();
    }

    public Output<T> dataOut ()
    {
        return dataOut.dataOut();
    }

    public static <T> Duplicator<T> newDuplicator (final Stage stage,
                                                   final int repeatCount)
    {
        return new Duplicator<>(stage, 1, repeatCount);
    }

    public static <T> Duplicator<T> newDuplicator (final Stage stage,
                                                   final int sequenceLen,
                                                   final int repeatCount)
    {
        return new Duplicator<>(stage, sequenceLen, repeatCount);
    }

    private void onMessage (final T message)
    {
        sequence.add(message);

        if (sequenceLen == sequence.size())
        {
            for (int i = 0; i < repeatCount; i++)
            {
                for (T element : sequence)
                {
                    dataOut.dataIn().send(element);
                }
            }

            sequence.clear();
        }
    }
}
