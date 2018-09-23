package com.mackenziehigh.socius.plugins;

import com.mackenziehigh.cascade.Cascade.Stage.Actor;

/**
 *
 */
public interface ChannelDownlink<T>
{
    public ChannelDownlink<T> subscribe (Actor<T, ?> connector,
                                         String channelName);

    public ChannelDownlink<T> subscribe (Actor.Input<T> connector,
                                         String channelName);
}
