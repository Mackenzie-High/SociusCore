package com.mackenziehigh.socius.flow;

import com.mackenziehigh.cascade.Cascade;

/**
 *
 */
public interface ChannelUplink<K, T>
{
    public ChannelUplink<K, T> publish (Cascade.Stage.Actor<?, T> connector,
                                        K key);

    public ChannelUplink<K, T> publish (Cascade.Stage.Actor.Output<T> connector,
                                        K key);

    public ChannelUplink<K, T> unpublish (Cascade.Stage.Actor<?, T> connector,
                                          K key);

    public ChannelUplink<K, T> unpublish (Cascade.Stage.Actor.Output<T> connector,
                                          K key);
}
