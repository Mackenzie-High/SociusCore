package com.mackenziehigh.socius.plugins;

import com.mackenziehigh.cascade.Cascade;

/**
 *
 */
public interface ChannelUplink<T>
{
    public ChannelDownlink<T> publish (Cascade.Stage.Actor<?, T> connector,
                                       String channelName);

    public ChannelDownlink<T> publish (Cascade.Stage.Actor.Input<T> connector,
                                       String channelName);
}
