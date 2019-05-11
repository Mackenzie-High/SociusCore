package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;

/**
 *
 */
public final class Console
        implements Processor<String>
{

    private final Processor<String> procOut;

    private final Processor<String> procErr;

    private final Processor<String> procIn;

    private Console (final Stage stage)
    {
        this.procOut = Processor.fromIdentityScript(stage);
        this.procErr = Processor.fromIdentityScript(stage);
        this.procIn = Processor.fromIdentityScript(stage);
    }

    public Input<String> stdout ()
    {
        return procOut.dataIn();
    }

    public Input<String> stderr ()
    {
        return procErr.dataIn();
    }

    public Output<String> stdin ()
    {
        return procIn.dataOut();
    }

    @Override
    public Input<String> dataIn ()
    {
        return stdout();
    }

    @Override
    public Output<String> dataOut ()
    {
        return stdin();
    }
}
