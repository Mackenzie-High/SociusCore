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

import com.mackenziehigh.socius.AbstractTrampoline;
import com.mackenziehigh.socius.AsyncTestTool;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class AbstractTrampolineTest
{
    private final AsyncTestTool tester = new AsyncTestTool();

    private final AbstractTrampoline<Integer, String> machine = new AbstractTrampoline<Integer, String>(tester.stage())
    {
        @Override
        protected State<Integer> onError (final State<Integer> state,
                                          final Integer message,
                                          final Throwable cause)
                throws Throwable
        {
            if (cause.getClass() == Throwable.class)
            {
                throw cause;
            }
            else
            {
                return this::end;
            }
        }

        @Override
        protected State<Integer> onInitial (final Integer message)
        {
            if (message == 1)
            {
                return this::state1;
            }
            else if (message == 2)
            {
                return this::state2;
            }
            else if (message == 2)
            {
                return this::state2;
            }
            else if (message == 3)
            {
                return this::state3;
            }
            else
            {
                throw new RuntimeException();
            }
        }

        private State<Integer> state1 (final Integer message)
        {
            sendFrom("X" + message + "X");
            return this::end;
        }

        private State<Integer> state2 (final Integer message)
                throws Throwable
        {
            throw new RuntimeException();
        }

        private State<Integer> state3 (final Integer message)
                throws Throwable
        {
            throw new Throwable();
        }

        private State<Integer> end (final Integer message)
        {
            sendFrom("Y" + message + "Y");
            return this::end;
        }
    };


    {
        tester.connect(machine.dataOut());
    }

    /**
     * Test: 20190513233337094556
     *
     * <p>
     * Case: Normal Transitions.
     * </p>
     */
    @Test
    public void test20190513233337094556 ()
    {
        machine.accept(1);
        machine.accept(13);
        machine.accept(17);
        tester.awaitEquals(machine.dataOut(), "X13X");
        tester.awaitEquals(machine.dataOut(), "Y17Y");
        assertFalse(machine.isNop());
    }

    /**
     * Test: 20190513233337094605
     *
     * <p>
     * Case: Exception in State Function.
     * </p>
     */
    @Test
    public void test20190513233337094605 ()
    {
        machine.accept(2);
        machine.accept(13);
        machine.accept(17);
        tester.awaitEquals(machine.dataOut(), "Y17Y");
        assertFalse(machine.isNop());
    }

    /**
     * Test: 20190513233337094648
     *
     * <p>
     * Case: Exception in <code>onError()</code>.
     * </p>
     */
    @Test
    public void test20190513233337094648 ()
    {
        machine.accept(3);
        machine.accept(13);
        tester.awaitTrue(() -> machine.isNop());
        assertTrue(machine.isNop());
    }

    /**
     * Test: 20190513233337094628
     *
     * <p>
     * Case: Exception in <code>onInitial()</code>.
     * </p>
     */
    @Test
    public void test20190513233337094628 ()
    {
        machine.accept(99);
        machine.accept(13);
        machine.accept(17);
        tester.awaitEquals(machine.dataOut(), "Y13Y");
        tester.awaitEquals(machine.dataOut(), "Y17Y");
        assertFalse(machine.isNop());
    }

    /**
     * Test: 20190513233337094670
     *
     * <p>
     * Case: Behavior of No-Op State.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190513233337094670 ()
            throws Throwable
    {
        assertSame(machine.nop(), machine.nop().onMessage(17));
    }
}
