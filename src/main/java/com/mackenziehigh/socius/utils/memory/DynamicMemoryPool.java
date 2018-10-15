package com.mackenziehigh.socius.utils.memory;

import java.util.Optional;

/**
 *
 */
public final class DynamicMemoryPool
        implements MemoryPool
{
    @Override
    public Optional<ByteSequence> allocate (final long size)
    {
        try
        {
            final ByteSequence seq = ByteSequences.allocate(size);
            return Optional.of(seq);
        }
        catch (OutOfMemoryError ex)
        {
            return Optional.empty();
        }
    }

    @Override
    public void free (final ByteSequence buffer)
    {
        // Pass.
    }

}
