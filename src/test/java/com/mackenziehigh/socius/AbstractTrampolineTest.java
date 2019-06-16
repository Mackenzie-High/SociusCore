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

import com.mackenziehigh.socius.AbstractTrampoline.State;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class AbstractTrampolineTest
{
    private static final int GOTO_STATE_1 = 1;

    private static final int GOTO_STATE_2 = 2;

    private static final int GOTO_STATE_3 = 3;

    private static final int THROW_RUNTIME_EXCEPTION = 99;

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
            if (message == GOTO_STATE_1)
            {
                return this::state1;
            }
            else if (message == GOTO_STATE_2)
            {
                return this::state2;
            }
            else if (message == GOTO_STATE_3)
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
            sendFrom("STATE_1 = " + message);
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
            sendFrom("END = " + message);
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
        machine.accept(GOTO_STATE_1);
        machine.accept(13);
        machine.accept(17);
        tester.awaitEquals(machine.dataOut(), "STATE_1 = 13");
        tester.awaitEquals(machine.dataOut(), "END = 17");
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
        machine.accept(GOTO_STATE_2);
        machine.accept(13); // Throws Exception in State Function
        machine.accept(17);
        tester.awaitEquals(machine.dataOut(), "END = 17");
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
        machine.accept(GOTO_STATE_3);
        machine.accept(13); // Throws Exception in State Function *AND* Error Handler.
        tester.awaitSteadyState();
        tester.awaitTrue(() -> machine.isInitial());
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
        machine.accept(THROW_RUNTIME_EXCEPTION);
        machine.accept(13);
        machine.accept(17);
        tester.awaitEquals(machine.dataOut(), "END = 13");
        tester.awaitEquals(machine.dataOut(), "END = 17");
    }

    /**
     * Test: 20190615225919691702
     *
     * <p>
     * Case: By Default, <code>onError()</code> returns the initial state.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190615225919691702 ()
            throws Throwable
    {
        final AbstractTrampoline<String, String> trampoline = new AbstractTrampoline<String, String>(tester.stage())
        {
            @Override
            protected State<String> onInitial (final String message)
            {
                return null;
            }
        };

        assertTrue(trampoline.isInitial());
        final State<String> initial = trampoline.state();
        final State<String> error = trampoline.onError(null, null, null);
        assertSame(initial, error);
    }
}
