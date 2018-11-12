package com.mackenziehigh.socius;

import com.mackenziehigh.socius.Multiplexer;
import com.mackenziehigh.socius.Demultiplexer;
import com.mackenziehigh.socius.ReactionTester;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class DemultiplexerTest
{
    @Test
    public void test1 ()
            throws Throwable
    {
        final ReactionTester tester = new ReactionTester();
        final Demultiplexer<Character, String> demux = Demultiplexer.newDemultiplexer(tester.stage());

        tester.send(demux.dataIn(), Multiplexer.newMessage('A', "autumn"));
        tester.send(demux.dataIn(), Multiplexer.newMessage('A', "avril"));
        tester.send(demux.dataIn(), Multiplexer.newMessage('E', "emma"));
        tester.send(demux.dataIn(), Multiplexer.newMessage('E', "erin"));
        tester.expect(demux.dataOut('A'), "autumn");
        tester.expect(demux.dataOut('A'), "avril");
        tester.expect(demux.dataOut('E'), "emma");
        tester.expect(demux.dataOut('E'), "erin");
        tester.requireEmptyOutputs();
        tester.run();
    }

    @Test
    public void testSameDataOut ()
    {
        final ReactionTester tester = new ReactionTester();
        final Demultiplexer<Character, String> demux = Demultiplexer.newDemultiplexer(tester.stage());
        assertEquals(demux.dataOut('X'), demux.dataOut('X'));
    }
}
