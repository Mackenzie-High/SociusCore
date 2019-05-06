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
package com.mackenziehigh.socius.flow;

import com.mackenziehigh.socius.flow.Trampoline.State;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class TrampolineTest
{
    private static final class TestScript
            implements Trampoline.Script<Integer, String>
    {
        @Override
        public State<Integer, String> initial ()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public State<Integer, String> error (State<Integer, String> source,
                                             Throwable cause)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    /**
     * Test: 20190505204903661071
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
    public void test20190505204903661071 ()
    {
        System.out.println("Test: 20190505204903661071");
        fail();
    }

    /**
     * Test: 20190505204903661208
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
    public void test20190505204903661208 ()
    {
        System.out.println("Test: 20190505204903661208");
        fail();
    }

    /**
     * Test: 20190505204903661240
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
    public void test20190505204903661240 ()
    {
        System.out.println("Test: 20190505204903661240");
        fail();
    }
}
