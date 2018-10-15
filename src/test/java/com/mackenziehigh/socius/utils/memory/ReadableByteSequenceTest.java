package com.mackenziehigh.socius.utils.memory;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class ReadableByteSequenceTest
{
    /**
     * Test: 20181015003803041532
     *
     * <p>
     * Method: <code>length()</code>
     * </p>
     */
    @Test
    public void test20181015003803041532 ()
    {
        for (int i = 0; i < 10; i++)
        {
            final ByteSequence seq = ByteSequences.allocate(i);
            assertEquals(i, seq.length());
        }
    }

    /**
     * Test: 20181015003935394346
     *
     * <p>
     * Method: <code>get()</code>
     * </p>
     *
     * <p>
     * Case: Normal.
     * </p>
     */
    @Test
    public void test20181015003935394346 ()
    {
        fail();
    }

    /**
     * Test: 20181015003935394430
     *
     * <p>
     * Method: <code></code>
     * </p>
     *
     * <p>
     * Case:
     * </p>
     */
    @Test
    public void test20181015003935394430 ()
    {
        System.out.println("Test: 20181015003935394430");
        fail();
    }

    /**
     * Test: 20181015003935394460
     *
     * <p>
     * Method: <code></code>
     * </p>
     *
     * <p>
     * Case:
     * </p>
     */
    @Test
    public void test20181015003935394460 ()
    {
        System.out.println("Test: 20181015003935394460");
        fail();
    }
}
