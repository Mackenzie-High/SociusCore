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

import com.google.common.base.Preconditions;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import java.util.Collection;

/**
 * Adds incoming messages to a specified <code>Collection</code>.
 *
 * @param <T> is the type of messages in the collection.
 */
public final class CollectionSink<T>
        implements Processor<T>
{
    private final Processor<T> actor;

    private CollectionSink (final Processor<T> actor)
    {
        this.actor = actor;
    }

    /**
     * Input Connection.
     *
     * @return the input that provides the messages to add to the collection.
     */
    @Override
    public Input<T> dataIn ()
    {
        return actor.dataIn();
    }

    /**
     * Output Connection.
     *
     * @return the output that merely forwards the messages from data-in.
     */
    @Override
    public Output<T> dataOut ()
    {
        return actor.dataOut();
    }

    /**
     * Factory Method.
     *
     * @param <T> is the type of messages in the collection.
     * @param stage will be used to create private actors.
     * @param collection will receive the messages from data-in.
     * @return the new sink.
     */
    public static <T> CollectionSink<T> newCollectionSink (final Cascade.ActorFactory stage,
                                                           final Collection<T> collection)
    {
        Preconditions.checkNotNull(stage, "stage");
        Preconditions.checkNotNull(collection, "collection");
        final Processor<T> proc = Processor.fromConsumerScript(stage, (T x) -> collection.add(x));
        return new CollectionSink<>(proc);
    }
}
