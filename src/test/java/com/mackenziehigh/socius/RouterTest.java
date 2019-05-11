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

import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.socius.Router.EndPoint;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class RouterTest
{
    private final ActorTester tester = new ActorTester();

    private final Router<Integer> dispatcher = Router.newRouter(tester.stage());

    private final Actor<Integer, Integer> actor1 = tester.stage().newActor().withFunctionScript((Integer x) -> x).create();

    private final Actor<Integer, Integer> actor2 = tester.stage().newActor().withFunctionScript((Integer x) -> x).create();

    private final Actor<Integer, Integer> actor3 = tester.stage().newActor().withFunctionScript((Integer x) -> x).create();

    private final Actor<Integer, Integer> actor4 = tester.stage().newActor().withFunctionScript((Integer x) -> x).create();

    private final Actor<Integer, Integer> actor5 = tester.stage().newActor().withFunctionScript((Integer x) -> x).create();

    /**
     * Test: 20180923042734271117
     *
     * <p>
     * Method: <code>publish</code>
     * </p>
     *
     * <p>
     * Case: Normal.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20180923042734271117 ()
            throws Throwable
    {
        /**
         * Case: Multiple publishers to the same channel.
         */
        actor1.output().connect(dispatcher.newEndPoint().subscribe("X").dataIn());
        actor2.output().connect(dispatcher.newEndPoint().subscribe("X").dataIn());

        /**
         * Case: Single publisher to a channel.
         */
        actor3.output().connect(dispatcher.newEndPoint().subscribe("Y").dataIn());

        /**
         * Verify the results.
         */
        actor4.input().connect(dispatcher.newEndPoint().subscribe("X").dataOut());
        actor5.input().connect(dispatcher.newEndPoint().subscribe("Y").dataOut());
        tester.send(actor1.input(), 100);
        tester.send(actor2.input(), 200);
        tester.send(actor3.input(), 300);
        tester.expect(actor4.output(), 100);
        tester.expect(actor4.output(), 200);
        tester.expect(actor5.output(), 300);
        tester.requireEmptyOutputs();
        tester.run();
    }

    /**
     * Test: 20180923042734271259
     *
     * <p>
     * Method: <code>publish</code>
     * </p>
     *
     * <p>
     * Case: Duplicate Registration.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20180923042734271259 ()
            throws Throwable
    {
        /**
         * Duplicate registrations are ignored.
         */
        final EndPoint<Integer> endpoint = dispatcher.newEndPoint();
        endpoint.subscribe("X");
        endpoint.subscribe("X"); // duplicate
        actor1.output().connect(endpoint.dataIn());

        /**
         * Verify the results.
         */
        actor2.input().connect(dispatcher.newEndPoint().subscribe("X").dataOut());
        tester.send(actor1.input(), 100);
        tester.send(actor1.input(), 200);
        tester.expect(actor2.output(), 100);
        tester.expect(actor2.output(), 200);
        tester.requireEmptyOutputs();
        tester.run();
    }
//
//    /**
//     * Test: 20180923042734271342
//     *
//     * <p>
//     * Method: <code>unpublish</code>
//     * </p>
//     *
//     * <p>
//     * Case: Normal.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20180923042734271342 ()
//            throws Throwable
//    {
//        tester.execute(() -> dispatcher.publish(actor1.output(), "X"));
//        tester.execute(() -> dispatcher.subscribe(actor2.input(), "X"));
//        tester.send(actor1.input(), 100);
//        tester.send(actor1.input(), 200);
//        tester.expect(actor2.output(), 100);
//        tester.expect(actor2.output(), 200);
//        tester.execute(() -> dispatcher.unpublish(actor1.output(), "X"));
//        tester.send(actor1.input(), 300);
//        tester.send(actor1.input(), 400);
//        tester.requireEmptyOutputs();
//        tester.run();
//
//    }
//
//    /**
//     * Test: 20180923042734271369
//     *
//     * <p>
//     * Method: <code>unpublish</code>
//     * </p>
//     *
//     * <p>
//     * Case: Duplicate Deregistration.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20180923042734271369 ()
//            throws Throwable
//    {
//        tester.execute(() -> dispatcher.publish(actor1.output(), "X"));
//        tester.execute(() -> dispatcher.subscribe(actor2.input(), "X"));
//        tester.send(actor1.input(), 100);
//        tester.send(actor1.input(), 200);
//        tester.expect(actor2.output(), 100);
//        tester.expect(actor2.output(), 200);
//        tester.execute(() -> dispatcher.unpublish(actor1.output(), "X"));
//        tester.execute(() -> dispatcher.unpublish(actor1.output(), "X"));
//        tester.send(actor1.input(), 300);
//        tester.send(actor1.input(), 400);
//        tester.requireEmptyOutputs();
//        tester.run();
//    }
//
//    /**
//     * Test: 20180923042734271443
//     *
//     * <p>
//     * Method: <code>subscribe</code>
//     * </p>
//     *
//     * <p>
//     * Case: Normal.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20180923042734271443 ()
//            throws Throwable
//    {
//        /**
//         * Case: Multiple subscribers to the same channel.
//         */
//        dispatcher.subscribe(actor1.input(), "X");
//        dispatcher.subscribe(actor2.input(), "X");
//
//        /**
//         * Case: Single subscriber to a channel.
//         */
//        dispatcher.subscribe(actor3.input(), "Y");
//
//        /**
//         * Verify the results.
//         */
//        dispatcher.publish(actor4.output(), "X");
//        dispatcher.publish(actor5.output(), "Y");
//        tester.send(actor4.input(), 100);
//        tester.send(actor4.input(), 200);
//        tester.send(actor5.input(), 300);
//        tester.send(actor5.input(), 400);
//        tester.expect(actor1.output(), 100);
//        tester.expect(actor1.output(), 200);
//        tester.expect(actor2.output(), 100);
//        tester.expect(actor2.output(), 200);
//        tester.expect(actor3.output(), 300);
//        tester.expect(actor3.output(), 400);
//        tester.requireEmptyOutputs();
//        tester.run();
//    }
//
//    /**
//     * Test: 20180923042734271465
//     *
//     * <p>
//     * Method: <code>subscribe</code>
//     * </p>
//     *
//     * <p>
//     * Case: Duplicate Registration.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20180923042734271465 ()
//            throws Throwable
//    {
//        /**
//         * Duplicate registrations are ignored.
//         */
//        dispatcher.subscribe(actor1.input(), "X");
//        dispatcher.subscribe(actor1.input(), "X");
//
//        /**
//         * Verify the results.
//         */
//        dispatcher.publish(actor2.output(), "X");
//        tester.send(actor2.input(), 100);
//        tester.send(actor2.input(), 200);
//        tester.expect(actor1.output(), 100);
//        tester.expect(actor1.output(), 200);
//        tester.requireEmptyOutputs();
//        tester.run();
//    }
//
//    /**
//     * Test: 20180923042734271532
//     *
//     * <p>
//     * Method: <code>unsubscribe</code>
//     * </p>
//     *
//     * <p>
//     * Case: Normal.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20180923042734271532 ()
//            throws Throwable
//    {
//        tester.execute(() -> dispatcher.publish(actor1.output(), "X"));
//        tester.execute(() -> dispatcher.subscribe(actor2.input(), "X"));
//        tester.send(actor1.input(), 100);
//        tester.send(actor1.input(), 200);
//        tester.expect(actor2.output(), 100);
//        tester.expect(actor2.output(), 200);
//        tester.execute(() -> dispatcher.unsubscribe(actor2.input(), "X"));
//        tester.send(actor1.input(), 300);
//        tester.send(actor1.input(), 400);
//        tester.requireEmptyOutputs();
//        tester.run();
//    }
//
//    /**
//     * Test: 20180923042734271553
//     *
//     * <p>
//     * Method: <code>unsubscribe</code>
//     * </p>
//     *
//     * <p>
//     * Case: Duplicate Deregistration.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20180923042734271553 ()
//            throws Throwable
//    {
//        tester.execute(() -> dispatcher.publish(actor1.output(), "X"));
//        tester.execute(() -> dispatcher.subscribe(actor2.input(), "X"));
//        tester.send(actor1.input(), 100);
//        tester.send(actor1.input(), 200);
//        tester.expect(actor2.output(), 100);
//        tester.expect(actor2.output(), 200);
//        tester.execute(() -> dispatcher.unsubscribe(actor2.input(), "X"));
//        tester.execute(() -> dispatcher.unsubscribe(actor2.input(), "X"));
//        tester.send(actor1.input(), 300);
//        tester.send(actor1.input(), 400);
//        tester.requireEmptyOutputs();
//        tester.run();
//    }
//
//    /**
//     * Test: 20180923044223676685
//     *
//     * <p>
//     * Method: <code>sinkAll()</code>
//     * </p>
//     *
//     * <p>
//     * Case: Normal.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20180923044223676685 ()
//            throws Throwable
//    {
//        /**
//         * Setup.
//         */
//        dispatcher.publish(actor1.output(), "X");
//        dispatcher.subscribe(actor2.input(), "Y");
//
//        /**
//         * Run Test.
//         */
//        tester.execute(() -> dispatcher.send("X", 100)); // channel 'X' has a publisher.
//        tester.execute(() -> dispatcher.send("Y", 200)); // channel 'Y' has a subscriber.
//        tester.execute(() -> dispatcher.send("Z", 300)); // channel 'Z' has neither.
//        tester.expect(dispatcher.sinkAll(), 100);
//        tester.expect(dispatcher.sinkAll(), 200);
//        tester.expect(dispatcher.sinkAll(), 300);
//        tester.requireEmptyOutputs();
//        tester.run();
//    }
//
//    /**
//     * Test: 20180923044223676713
//     *
//     * <p>
//     * Method: <code>sinkDead()</code>
//     * </p>
//     *
//     * <p>
//     * Case: Normal.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20180923044223676713 ()
//            throws Throwable
//    {
//        /**
//         * Setup.
//         */
//        dispatcher.publish(actor1.output(), "X");
//        dispatcher.subscribe(actor2.input(), "Y");
//
//        /**
//         * Run Test.
//         */
//        tester.execute(() -> dispatcher.send("X", 100)); // channel 'X' has a publisher.
//        tester.execute(() -> dispatcher.send("Y", 200)); // channel 'Y' has a subscriber.
//        tester.execute(() -> dispatcher.send("Z", 300)); // channel 'Z' has neither.
//        tester.expect(dispatcher.sinkDead(), 100);
//        tester.expect(dispatcher.sinkDead(), 300);
//        tester.requireEmptyOutputs();
//        tester.run();
//    }
}
