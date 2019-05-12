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

import com.google.common.collect.Lists;
import java.util.List;
import static org.junit.Assert.*;
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
        final var tester = new AsyncTestTool();
        final List<String> collection = Lists.newArrayList();
        final CollectionSink<String> actor = CollectionSink.newCollectionSink(tester.stage(), collection);

        tester.connect(actor.dataOut());

        actor.dataIn().send("autumn");
        actor.dataIn().send("emma");
        actor.dataIn().send("erin");
        actor.dataIn().send("molly");
        actor.dataIn().send("t'pol");

        tester.expect(actor.dataOut(), "autumn");
        tester.expect(actor.dataOut(), "emma");
        tester.expect(actor.dataOut(), "erin");
        tester.expect(actor.dataOut(), "molly");
        tester.expect(actor.dataOut(), "t'pol");

        assertEquals(5, collection.size());
        assertEquals("autumn", collection.get(0));
        assertEquals("emma", collection.get(1));
        assertEquals("erin", collection.get(2));
        assertEquals("molly", collection.get(3));
        assertEquals("t'pol", collection.get(4));
    }
}
