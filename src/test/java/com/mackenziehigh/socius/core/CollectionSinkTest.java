package com.mackenziehigh.socius.core;

import com.mackenziehigh.socius.core.ActorTester;
import com.mackenziehigh.socius.core.CollectionSink;
import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class CollectionSinkTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final ActorTester tester = new ActorTester();
        final List<String> collection = Lists.newArrayList();
        final CollectionSink<String> actor = CollectionSink.newCollectionSink(tester.stage(), collection);

        tester.send(actor.dataIn(), "autumn");
        tester.send(actor.dataIn(), "emma");
        tester.send(actor.dataIn(), "erin");
        tester.send(actor.dataIn(), "molly");
        tester.send(actor.dataIn(), "t'pol");
        tester.requireEmptyOutputs();
        tester.run();
        tester.require(() -> collection.get(0).equals("autumn"));
        tester.require(() -> collection.get(1).equals("emma"));
        tester.require(() -> collection.get(2).equals("erin"));
        tester.require(() -> collection.get(3).equals("molly"));
        tester.require(() -> collection.get(4).equals("t'pol"));
        tester.require(() -> collection.size() == 5);
    }
}
