package com.mackenziehigh.socius.flow;

import com.mackenziehigh.socius.testing.ReactionTester;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class MultiplexerTest
{

    @Test
    public void test1 ()
            throws Throwable
    {
        final ReactionTester tester = new ReactionTester();
        final Multiplexer<Character, String> mux = Multiplexer.newMultiplexer(tester.stage());

        tester.send(mux.dataIn('A'), "autumn");
        tester.send(mux.dataIn('A'), "avril");
        tester.send(mux.dataIn('E'), "emma");
        tester.send(mux.dataIn('E'), "erin");
        tester.expect(mux.dataOut(), Multiplexer.newMessage('A', "autumn"));
        tester.expect(mux.dataOut(), Multiplexer.newMessage('A', "avril"));
        tester.expect(mux.dataOut(), Multiplexer.newMessage('E', "emma"));
        tester.expect(mux.dataOut(), Multiplexer.newMessage('E', "erin"));
        tester.requireEmptyOutputs();
        tester.run();
    }

    @Test
    public void testSameDataIn ()
    {
        final ReactionTester tester = new ReactionTester();
        final Multiplexer<Character, String> mux = Multiplexer.newMultiplexer(tester.stage());
        assertEquals(mux.dataIn('X'), mux.dataIn('X'));
    }

    @Test
    public void testMessageEquality ()
    {
        assertEquals(Multiplexer.newMessage('A', 'B'), Multiplexer.newMessage('A', 'B'));
        assertNotEquals(Multiplexer.newMessage('A', 'B'), Multiplexer.newMessage('A', 'X'));
        assertNotEquals(Multiplexer.newMessage('A', 'B'), Multiplexer.newMessage('X', 'B'));
        assertFalse(Multiplexer.newMessage('A', 'B').equals((Object) "AB"));
    }

    @Test
    public void testMessageToString ()
    {
        assertEquals("A => B", Multiplexer.newMessage('A', 'B').toString());
    }
}
