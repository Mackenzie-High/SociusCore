package com.mackenziehigh.socius.flow;

import com.mackenziehigh.socius.flow.ChannelDownlink;
import com.mackenziehigh.socius.flow.ChannelUplink;

/**
 *
 */
public interface ChannelBilink<K, T>
        extends ChannelUplink<K, T>,
                ChannelDownlink<K, T>
{
    // Pass.
}
