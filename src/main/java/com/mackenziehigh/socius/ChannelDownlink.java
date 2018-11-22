package com.mackenziehigh.socius;

import com.mackenziehigh.cascade.Cascade.Stage.Actor;

/**
 *
 */
public interface ChannelDownlink<K, T>
{
    public ChannelDownlink<K, T> subscribe (Actor<T, ?> connector,
                                            K key);

    public ChannelDownlink<K, T> subscribe (Actor.Input<T> connector,
                                            K key);

    public ChannelDownlink<K, T> unsubscribe (Actor<T, ?> connector,
                                              K key);

    public ChannelDownlink<K, T> unsubscribe (Actor.Input<T> connector,
                                              K key);
}
