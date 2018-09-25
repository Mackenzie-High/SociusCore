package com.mackenziehigh.socius.plugins.jfx;

import com.mackenziehigh.cascade.Cascade.AbstractStage;
import javafx.application.Platform;

/**
 * A <code>Stage</code> powered by the Java FX Platform Thread.
 *
 * <p>
 * This stage is intended for use when an actor manipulates a Java FX GUI.
 * Since the actor will be executed on the Platform Thread, thread-safety is assured.
 * </p>
 */
public final class StageFx
        extends AbstractStage
{
    private static final StageFx INSTANCE = new StageFx();

    private StageFx ()
    {
        // Pass.
    }

    @Override
    protected void onSubmit (final ActorTask task)
    {
        Platform.runLater(task);
    }

    @Override
    protected void onStageClose ()
    {
        // Pass.
    }

    /**
     * Get the singleton instance of this class.
     *
     * @return the singleton.
     */
    public static StageFx instance ()
    {
        return INSTANCE;
    }

}
