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

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class AbstractProcessorTest
{
    private final AsyncTestTool tester = new AsyncTestTool();

    private final AbstractProcessor<String> actor = new AbstractProcessor<String>(tester.stage())
    {
        @Override
        protected void onMessage (final String message)
                throws Throwable
        {
            sendFrom("X" + message + "X");
        }
    };


    {
        tester.connect(actor.dataOut());
    }

    /**
     * Test: 20190513224934685030
     *
     * <p>
     * Method: <code>onMessage()</code>
     * </p>
     *
     * <p>
     * Case: Basic Throughput.
     * </p>
     */
    @Test
    public void test20190513224934685030 ()
    {
        actor.accept("100");
        actor.accept("200");
        actor.accept("300");

        tester.expect(actor.dataOut(), "X100X");
        tester.expect(actor.dataOut(), "X200X");
        tester.expect(actor.dataOut(), "X300X");
    }

    /**
     * Test: 20190513225814516270
     *
     * <p>
     * Method: <code>context</code>
     * </p>
     */
    @Test
    public void test20190513225814516270 ()
    {
        assertNotNull(actor.context());
        assertNotNull(actor.context().actor());
    }

    /**
     * Test: 20190513225814516373
     *
     * <p>
     * Method: <code>sendTo</code>
     * </p>
     */
    @Test
    public void test20190513225814516373 ()
    {
        actor.sendTo("17");
        tester.expect(actor.dataOut(), "X17X");
    }

    /**
     * Test: 20190513225814516391
     *
     * <p>
     * Method: <code>sendFrom</code>
     * </p>
     */
    @Test
    public void test20190513225814516391 ()
    {
        actor.sendFrom("Y23Y");
        tester.expect(actor.dataOut(), "Y23Y");
    }

    /**
     * Test: 20190513225814516409
     *
     * <p>
     * Method: <code>offerTo</code>
     * </p>
     */
    @Test
    public void test20190513225814516409 ()
    {
        assertTrue(actor.offerTo("27"));
        tester.expect(actor.dataOut(), "X27X");
    }

    /**
     * Test: 20190513225814516428
     *
     * <p>
     * Method: <code>offerFrom</code>
     * </p>
     */
    @Test
    public void test20190513225814516428 ()
    {
        assertTrue(actor.offerFrom("Y31Y"));
        tester.expect(actor.dataOut(), "Y31Y");
    }
}
