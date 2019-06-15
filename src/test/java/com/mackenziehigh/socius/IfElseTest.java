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

import com.mackenziehigh.socius.IfElse;
import com.mackenziehigh.socius.AsyncTestTool;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class IfElseTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final var tester = new AsyncTestTool();
        final var actor = IfElse.newIfElse(tester.stage(), (String x) -> x.contains("e"));

        tester.connect(actor.trueOut());
        tester.connect(actor.falseOut());

        actor.dataIn().send("avril");
        actor.dataIn().send("emma");
        actor.dataIn().send("erin");
        actor.dataIn().send("t;pol");

        tester.awaitEquals(actor.falseOut(), "avril");
        tester.awaitEquals(actor.trueOut(), "emma");
        tester.awaitEquals(actor.trueOut(), "erin");
        tester.awaitEquals(actor.falseOut(), "t'pol");
    }
}
