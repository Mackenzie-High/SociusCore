package com.mackenziehigh.socius.utils.memory;

/**
 *
 * @author mackenzie
 */
public class Main
{
    public static void main (String[] args)
    {
        final ByteSequence seq = ByteSequences.allocate(100);
        seq.set(0, (byte) 'A');
        seq.set(1, (byte) 'B');
        seq.set(2, (byte) 'C');

        System.out.println("X = " + seq.toString());
    }
}
