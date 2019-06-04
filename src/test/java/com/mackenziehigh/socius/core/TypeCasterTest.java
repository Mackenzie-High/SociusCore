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
package com.mackenziehigh.socius.core;

import com.mackenziehigh.socius.core.AsyncTestTool;
import com.mackenziehigh.socius.core.TypeCaster;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class TypeCasterTest
{
    @Test
    public void test ()
            throws Throwable
    {
        final var tester = new AsyncTestTool();
        final TypeCaster<Number, Integer> caster = TypeCaster.newTypeCaster(tester.stage(), Integer.class);

        tester.connect(caster.dataOut());
        tester.connect(caster.errorOut());

        /**
         * Casting an integer should succeed.
         */
        caster.accept(3);
        tester.expect(caster.dataOut(), 3);

        /**
         * Casting a double should fail.
         */
        caster.accept(3.0);
        tester.expect(caster.errorOut(), 3.0);
    }
}
