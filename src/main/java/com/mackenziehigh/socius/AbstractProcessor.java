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

import com.mackenziehigh.cascade.Cascade.Stage;

/**
 * Facilitates easy implementation of a <code>Processor</code> via sub-classing.
 *
 * @param <T> is the type of the incoming and outgoing messages.
 */
public abstract class AbstractProcessor<T>
        extends AbstractPipeline<T, T>
        implements Processor<T>
{
    protected AbstractProcessor (final Stage stage)
    {
        super(stage);
    }
}
