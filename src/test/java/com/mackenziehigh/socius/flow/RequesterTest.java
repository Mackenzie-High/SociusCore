package com.mackenziehigh.socius.flow;

import com.mackenziehigh.socius.testing.ActorTester;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class RequesterTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();

        final Requester<Character, String, String, String> requester = Requester
                .<Character, String, String, String>newRequester(tester.stage())
                .withComposer((String x, String y) -> (x + "." + y.length()))
                .withRequestKeyFunction(x -> x.charAt(0))
                .withReplyKeyFunction(x -> x.charAt(0))
                .build();

        final Mapper<String, String> server = Mapper.newMapper(tester.stage());

        requester.requestOut().connect(server.dataIn());
        requester.replyIn().connect(server.dataOut());

        tester.send(requester.requestIn(), "Avril");
        tester.expect(requester.resultOut(), "Avril.5");

        tester.send(requester.requestIn(), "Emma");
        tester.expect(requester.resultOut(), "Emma.4");

        tester.send(requester.requestIn(), "Erin");
        tester.expect(requester.resultOut(), "Erin.4");

        tester.run();
    }
}
