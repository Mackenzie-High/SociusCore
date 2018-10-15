package com.mackenziehigh.socius.utils.memory;

import java.util.Optional;

/**
 *
 */
public interface MemoryPool
        extends Allocator
{
    @Override
    public Optional<ByteSequence> allocate (long size);

    public void free (ByteSequence buffer);
}
