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
package com.mackenziehigh.socius.io;

import com.mackenziehigh.socius.util.ActorTester;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class PrinterTest
{
    private final PrintStream originalOut = System.out;

    private final PrintStream originalErr = System.err;

    private final ByteArrayOutputStream arrayOut = new ByteArrayOutputStream();

    private final ByteArrayOutputStream arrayErr = new ByteArrayOutputStream();

    private final ActorTester tester = new ActorTester();

    @Before
    public void setup ()
    {
        System.setOut(new PrintStream(arrayOut));
        System.setErr(new PrintStream(arrayErr));
    }

    @After
    public void destroy ()
    {
        System.setOut(originalOut);
        System.setOut(originalErr);
    }

    /**
     * Test print, with custom format, to standard-out.
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void testPrintWithCustomFormat ()
            throws Throwable
    {
        final Printer<Integer> printer = Printer.newPrint(tester.stage(), "X = %d");

        tester.send(printer.dataIn(), 100);
        tester.expect(printer.dataOut(), 100);
        tester.requireEmptyOutputs();
        tester.run();
        assertEquals("X = 100", arrayOut.toString());
    }

    /**
     * Test print, with custom format, to standard-err.
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void testPrinterrWithCustomFormat ()
            throws Throwable
    {
        final Printer<Integer> printer = Printer.newPrinterr(tester.stage(), "X = %d");

        tester.send(printer.dataIn(), 100);
        tester.expect(printer.dataOut(), 100);
        tester.requireEmptyOutputs();
        tester.run();
        assertEquals("X = 100", arrayErr.toString());
    }

    /**
     * Test print-line, with custom format, to standard-err.
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void testPrinterrlnWithCustomFormat ()
            throws Throwable
    {
        final Printer<Integer> printer = Printer.newPrinterrln(tester.stage(), "X = %d");

        tester.send(printer.dataIn(), 100);
        tester.expect(printer.dataOut(), 100);
        tester.requireEmptyOutputs();
        tester.run();
        assertEquals("X = 100\n", arrayErr.toString());
    }

    /**
     * Test print-line, with custom format, to standard-out.
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void testPrintlnWithCustomFormat ()
            throws Throwable
    {
        final Printer<Integer> printer = Printer.newPrintln(tester.stage(), "X = %d");

        tester.send(printer.dataIn(), 100);
        tester.expect(printer.dataOut(), 100);
        tester.requireEmptyOutputs();
        tester.run();
        assertEquals("X = 100\n", arrayOut.toString());
    }

    /**
     * Test print, without custom format, to standard-out.
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void testPrintWithoutCustomFormat ()
            throws Throwable
    {
        final Printer<Integer> printer = Printer.newPrint(tester.stage());

        tester.send(printer.dataIn(), 100);
        tester.expect(printer.dataOut(), 100);
        tester.requireEmptyOutputs();
        tester.run();
        assertEquals("100", arrayOut.toString());
    }

    /**
     * Test print, without custom format, to standard-err.
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void testPrinterrWithoutCustomFormat ()
            throws Throwable
    {
        final Printer<Integer> printer = Printer.newPrinterr(tester.stage());

        tester.send(printer.dataIn(), 100);
        tester.expect(printer.dataOut(), 100);
        tester.requireEmptyOutputs();
        tester.run();
        assertEquals("100", arrayErr.toString());
    }

    /**
     * Test print-line, without custom format, to standard-err.
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void testPrinterrlnWithoutCustomFormat ()
            throws Throwable
    {
        final Printer<Integer> printer = Printer.newPrinterrln(tester.stage());

        tester.send(printer.dataIn(), 100);
        tester.expect(printer.dataOut(), 100);
        tester.requireEmptyOutputs();
        tester.run();
        assertEquals("100\n", arrayErr.toString());
    }

    /**
     * Test print-line, without custom format, to standard-out.
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void testPrintlnWithoutCustomFormat ()
            throws Throwable
    {
        final Printer<Integer> printer = Printer.newPrintln(tester.stage());

        tester.send(printer.dataIn(), 100);
        tester.expect(printer.dataOut(), 100);
        tester.requireEmptyOutputs();
        tester.run();
        assertEquals("100\n", arrayOut.toString());
    }
}
