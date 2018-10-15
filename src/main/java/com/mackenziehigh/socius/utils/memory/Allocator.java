package com.mackenziehigh.socius.utils.memory;

import java.util.Optional;

/**
 * A function that allocates a <code>ByteSequence</code>.
 */
@FunctionalInterface
public interface Allocator
{
    public Optional<ByteSequence> allocate (long size);
}
