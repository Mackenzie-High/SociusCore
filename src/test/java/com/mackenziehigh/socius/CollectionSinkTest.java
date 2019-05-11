/*
 * Copyright 2019 Michael Mackenzie High
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mackenziehigh.socius;

import com.mackenziehigh.socius.CollectionSink;
import com.google.common.collect.Lists;
import com.mackenziehigh.socius.util.ActorTester;
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
