# Socius 

Note: This project is still awaiting its first formal release.

Socius provides commonly used actor types based on the minimalist [Cascade](https://github.com/Mackenzie-High/Cascade) framework. 

## Related Projects:

* [Socius - Core](#Socius): Base Components. 
* [Socius - Nats](https://github.com/Mackenzie-High/SociusNats): Provides the ability to connect to [NATS-based](https://nats.io/) message-oriented-middleware.
* [Socius - Web](https://github.com/Mackenzie-High/SociusWeb): Provides a server for creating HTTP-based RESTful micro-services. 

## Core Classes:
* [Batcher](#Batcher)
* [BatchInserter](#BatchInserter)
* [Bus](#Bus)
* [Caster](#Caster)
* [Clock](#Clock)
* [CollectionSink](#CollectionSink)
* [DelayedSender](#DelayedSender)
* [Duplicator](#Duplicator)
* [Fanout](#Fanout)
* [Filter](#Filter)
* [Funnel](#Funnel)
* [IfElse](#IfElse)
* [LookupInserter](#LookupInserter)
* [Mapper](#Mapper)
* [Minuteman](#Minuteman)
* [Oscillator](#Oscillator)
* [Printer](#Printer)
* [Processor](#Processor)
* [Requester](#Requester)
* [RoundRobin](#RoundRobin)
* [Router](#Router)
* [ShuntingYard](#ShuntingYard)
* [TableInserter](#TableInserter)
* [Unbatcher](#Unbatcher)
* [Valve](#Valve)
* [Variable](#Variable)
* [WakeupCaller](#WakeupCaller)

### Batcher

#### Example Program:

```Java
package example;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.flow.Batcher;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.io.Printer;
import java.util.List;

public final class Example
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        /**
         * These actors merely simulate data producers.
         */
        final Processor<String> producer0 = Processor.newConnector(stage);
        final Processor<String> producer1 = Processor.newConnector(stage);
        final Processor<String> producer2 = Processor.newConnector(stage);

        /**
         * This is the actor whose functionality is being demonstrated.
         */
        final Batcher<String> batcher = Batcher.<String>newBatcher(stage)
                .withArity(3)
                .build();

        /**
         * This actor will print the output of the batcher to standard-output.
         */
        final Printer<List<String>> sink = Printer.newPrintln(stage, "List = %s");

        /**
         * Connect the actors to form a network.
         */
        producer0.dataOut().connect(batcher.dataIn(0));
        producer1.dataOut().connect(batcher.dataIn(1));
        producer2.dataOut().connect(batcher.dataIn(2));
        batcher.dataOut().connect(sink.dataIn());

        /**
         * Cause data to flow through the network.
         */
        producer0.accept("X1");
        producer0.accept("X2");
        producer0.accept("X3");
        producer0.accept("X4");
        producer0.accept("X5");
        producer1.accept("Y1");
        producer1.accept("Y2");
        producer1.accept("Y3");
        producer1.accept("Y4");
        producer1.accept("Y5");
        producer2.accept("Z1");
        producer2.accept("Z2");
        producer2.accept("Z3");
        producer2.accept("Z4");
        producer2.accept("Z5");
    }
}
```

#### Example Output:

```
List = [X1, Y1, Z1]
List = [X2, Y2, Z2]
List = [X3, Y3, Z3]
List = [X4, Y4, Z4]
List = [X5, Y5, Z5]
```

### BatchInserter

#### Example Program:

```Java
package example;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.flow.BatchInserter;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.io.Printer;
import java.util.List;

public final class Example
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        /**
         * These actors merely simulate data producers.
         */
        final Processor<String> producer = Processor.newConnector(stage);

        /**
         * This is the actor whose functionality is being demonstrated.
         * A batch consists of one engine, one transmission, and four wheels.
         */
        final BatchInserter<String> inserter = BatchInserter.<String>newBatchInserter(stage)
                .require(x -> x.contains("Engine"))
                .require(x -> x.contains("Transmission"))
                .require(x -> x.contains("Wheel"))
                .require(x -> x.contains("Wheel"))
                .require(x -> x.contains("Wheel"))
                .require(x -> x.contains("Wheel"))
                .build();

        /**
         * This actor will print the batches to standard-output.
         */
        final Printer<List<String>> factory = Printer.newPrintln(stage, "Build Vehicle Using: %s");

        /**
         * This actor will print the unused parts to standard-output.
         */
        final Printer<String> sink = Printer.newPrintln(stage, "Skip Part: %s");

        /**
         * Connect the actors to form a network.
         */
        producer.dataOut().connect(inserter.dataIn());
        inserter.batchOut().connect(factory.dataIn());
        inserter.dataOut().connect(sink.dataIn());

        /**
         * Cause data to flow through the network.
         */
        producer.accept("Engine #1");
        producer.accept("Engine #2");
        producer.accept("Wheel #1");
        producer.accept("Wheel #2");
        producer.accept("Wheel #3");
        producer.accept("Wheel #4");
        producer.accept("Wheel #5");
        producer.accept("Transmission #1");
        producer.accept("Engine #3");
        producer.accept("Wheel #6");
        producer.accept("Wheel #7");
        producer.accept("Wheel #8");
        producer.accept("Wheel #9");
        producer.accept("Transmission #2");
    }
}
```

#### Example Output:

```
Skip Part: Engine #2
Skip Part: Wheel #5
Build Vehicle Using: [Engine #1, Transmission #1, Wheel #1, Wheel #2, Wheel #3, Wheel #4]
Build Vehicle Using: [Engine #3, Transmission #2, Wheel #6, Wheel #7, Wheel #8, Wheel #9]
```

### Bus

#### Example Program:

```Java
package example;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.flow.Bus;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.io.Printer;

public final class Example
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        /**
         * These actors merely simulate data producers.
         */
        final Processor<String> producer0 = Processor.newConnector(stage);
        final Processor<String> producer1 = Processor.newConnector(stage);
        final Processor<String> producer2 = Processor.newConnector(stage);

        /**
         * This is the actor whose functionality is being demonstrated.
         * Any message from any producer will be sent to every consumer.
         */
        final Bus<String> bus = Bus.newBus(stage);

        /**
         * These actors will print the messages to standard-output.
         */
        final Printer<String> consumer0 = Printer.newPrintln(stage, "(Consumer #0) got (%s).");
        final Printer<String> consumer1 = Printer.newPrintln(stage, "(Consumer #1) got (%s).");
        final Printer<String> consumer2 = Printer.newPrintln(stage, "(Consumer #2) got (%s).");

        /**
         * Connect the actors to form a network.
         * The keys P0, P1, P2 and C0, C1, C2 where chosen at random.
         */
        producer0.dataOut().connect(bus.dataIn("P0"));
        producer1.dataOut().connect(bus.dataIn("P1"));
        producer2.dataOut().connect(bus.dataIn("P2"));
        consumer0.dataIn().connect(bus.dataOut("C0"));
        consumer1.dataIn().connect(bus.dataOut("C1"));
        consumer2.dataIn().connect(bus.dataOut("C2"));

        /**
         * Cause data to flow through the network.
         */
        producer0.accept("Message #1");
        producer1.accept("Message #2");
        producer2.accept("Message #3");
    }
}
```

#### Example Output:

```
(Consumer #0) got (Message #1).
(Consumer #1) got (Message #1).
(Consumer #2) got (Message #1).
(Consumer #0) got (Message #2).
(Consumer #1) got (Message #2).
(Consumer #2) got (Message #2).
(Consumer #0) got (Message #3).
(Consumer #1) got (Message #3).
(Consumer #2) got (Message #3).
```

### Caster

#### Example Program: 

```Java
package example;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.flow.Caster;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.io.Printer;

public final class Example
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        /**
         * This actor merely simulates a data producer.
         */
        final Processor<Object> producer = Processor.newConnector(stage);

        /**
         * This is the actor whose functionality is being demonstrated.
         */
        final Caster<Object, String> caster = Caster.newCaster(stage, String.class);

        /**
         * These actors will print the results to standard-output.
         */
        final Printer<String> success = Printer.newPrintln(stage, "Successfully cast (%s) to type String.");
        final Printer<Object> failure = Printer.newPrintln(stage, "Failed to cast (%s) to type String.");

        /**
         * Connect the actors to form a network.
         */
        producer.dataOut().connect(caster.dataIn());
        caster.dataOut().connect(success.dataIn());
        caster.errorOut().connect(failure.dataIn());

        /**
         * Cause data to flow through the network.
         */
        producer.accept("10"); // 10 is a String.
        producer.accept(13); // 13 is an Integer.
    }
}
```

#### Example Output:

```
Successfully cast (10) to type String.
Failed to cast (13) to type String.
```

### Clock

#### Example Program:

```Java
package example;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.io.Printer;
import com.mackenziehigh.socius.time.Clock;
import java.time.Duration;
import java.time.Instant;

public final class Example
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        /**
         * This is the actor whose functionality is being demonstrated.
         */
        final Clock clock = Clock.newClock().withPeriod(Duration.ofSeconds(1)).build();

        /**
         * Start the flow of clock-ticks; otherwise, nothing meaningful will happen.
         */
        clock.start();

        /**
         * These actors will print the clock-ticks to standard-output.
         */
        final Printer<Instant> printer = Printer.newPrintln(stage, "Current Time = %s");

        /**
         * Connect the actors to form a network.
         */
        clock.clockOut().connect(printer.dataIn());
    }
}
```

#### Example Output:

```
Current Time = 2019-02-13T03:54:13.529Z
Current Time = 2019-02-13T03:54:14.528Z
Current Time = 2019-02-13T03:54:15.528Z
Current Time = 2019-02-13T03:54:16.528Z
Current Time = 2019-02-13T03:54:17.528Z
Current Time = 2019-02-13T03:54:18.528Z
Current Time = 2019-02-13T03:54:19.528Z
Current Time = 2019-02-13T03:54:20.528Z
```

### CollectionSink

#### Example Program:

```Java
package example;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.io.CollectionSink;
import com.mackenziehigh.socius.io.Printer;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Example
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        /**
         * This actor merely simulates a data producer.
         */
        final Processor<String> producer = Processor.newConnector(stage);

        /**
         * This is the collection that the sink will add elements to.
         */
        final Collection<String> collection = new CopyOnWriteArrayList<>();

        /**
         * This is the actor whose functionality is being demonstrated.
         */
        final CollectionSink<String> sink = CollectionSink.newCollectionSink(stage, collection);

        /**
         * These actors will print the results to standard-output.
         */
        final Printer<String> printer = Printer.newPrintln(stage, "Collection: " + collection);

        /**
         * Connect the actors to form a network.
         */
        producer.dataOut().connect(sink.dataIn());
        sink.dataOut().connect(printer.dataIn());

        /**
         * Cause data to flow through the network.
         */
        producer.accept("A");
        producer.accept("B");
        producer.accept("C");
        producer.accept("X");
        producer.accept("Y");
        producer.accept("Z");
    }
}
```

#### Example Output:

TODO

### DelayedSender

#### Example Program:

#### Example Output:

### Duplicator

#### Example Program:

```Java
package example;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.flow.Duplicator;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.io.Printer;

public final class Example
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        /**
         * This actor merely simulates a data producer.
         */
        final Processor<String> producer = Processor.newConnector(stage);

        /**
         * This is the actor whose functionality is being demonstrated.
         */
        final Duplicator<String> dup = Duplicator.<String>newDuplicator(stage)
                .withSequenceLength(2)
                .withRepeatCount(3)
                .build();

        /**
         * These actors will print the messages to standard-output.
         */
        final Printer<String> printer = Printer.newPrintln(stage);

        /**
         * Connect the actors to form a network.
         */
        producer.dataOut().connect(dup.dataIn());
        dup.dataOut().connect(printer.dataIn());

        /**
         * Cause data to flow through the network.
         */
        producer.accept("A");
        producer.accept("B");
        producer.accept("X");
        producer.accept("Y");
    }
}
```

#### Example Output:

```
A
B
A
B
A
B
X
Y
X
Y
X
Y
```

### Fanout

#### Example Program:

```Java
package example;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.flow.Fanout;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.io.Printer;

public final class Example
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        /**
         * This actor merely simulates a data producer.
         */
        final Processor<String> commander = Processor.newConnector(stage);

        /**
         * This is the actor whose functionality is being demonstrated.
         */
        final Fanout<String> fanout = Fanout.newFanout(stage);

        /**
         * These actors will print the commands to standard-output.
         */
        final Printer<String> silo0 = Printer.newPrintln(stage, "Silo #0 received command (%s).");
        final Printer<String> silo1 = Printer.newPrintln(stage, "Silo #1 received command (%s).");
        final Printer<String> silo2 = Printer.newPrintln(stage, "Silo #2 received command (%s).");

        /**
         * Connect the actors to form a network.
         */
        commander.dataOut().connect(fanout.dataIn());
        fanout.dataOut("S0").connect(silo0.dataIn());
        fanout.dataOut("S1").connect(silo1.dataIn());
        fanout.dataOut("S2").connect(silo2.dataIn());

        /**
         * Cause data to flow through the network.
         */
        commander.accept("Goto DEFCON 1");
        commander.accept("Launch Strike #1");
        commander.accept("Launch Strike #2");
    }
}
```

#### Example Output:

```
Silo #0 received command (Goto DEFCON 1).
Silo #1 received command (Goto DEFCON 1).
Silo #2 received command (Goto DEFCON 1).
Silo #0 received command (Launch Strike #1).
Silo #1 received command (Launch Strike #1).
Silo #2 received command (Launch Strike #1).
Silo #0 received command (Launch Strike #2).
Silo #1 received command (Launch Strike #2).
Silo #2 received command (Launch Strike #2).
```

### Filter

#### Example Program:

```Java
package example;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.flow.Filter;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.io.Printer;

public final class Example
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        /**
         * This actor merely simulates a data producer.
         */
        final Processor<String> producer = Processor.newConnector(stage);

        /**
         * This is the actor whose functionality is being demonstrated.
         */
        final Filter<String> filter = Filter.newFilter(stage, x -> x.startsWith("E"));

        /**
         * This actor will print the message that were *not* dropped.
         */
        final Printer<String> printer = Printer.newPrintln(stage, "%s escaped the filter.");

        /**
         * Connect the actors to form a network.
         */
        producer.dataOut().connect(filter.dataIn());
        filter.dataOut().connect(printer.dataIn());

        /**
         * Cause data to flow through the network.
         */
        producer.accept("Autumn");
        producer.accept("Elle");
        producer.accept("Ashley");
        producer.accept("Emma");
        producer.accept("Anna");
        producer.accept("Erin");
    }
}
```

#### Example Output:

```
Elle escaped the filter.
Emma escaped the filter.
Erin escaped the filter.
```

### Funnel

#### Example Program:

```Java
package example;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.flow.Funnel;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.io.Printer;

public final class Example
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        /**
         * These actors merely simulate data producers.
         */
        final Processor<Integer> producer0 = Processor.newConnector(stage);
        final Processor<Integer> producer1 = Processor.newConnector(stage);
        final Processor<Integer> producer2 = Processor.newConnector(stage);

        /**
         * This is the actor whose functionality is being demonstrated.
         */
        final Funnel<Integer> funnel = Funnel.newFunnel(stage);

        /**
         * This actor will print the message that were *not* dropped.
         */
        final Printer<Integer> printer = Printer.newPrintln(stage, "Funneled $%d dollars into this project.");

        /**
         * Connect the actors to form a network.
         */
        producer0.dataOut().connect(funnel.dataIn("P0"));
        producer1.dataOut().connect(funnel.dataIn("P1"));
        producer2.dataOut().connect(funnel.dataIn("P2"));
        funnel.dataOut().connect(printer.dataIn());

        /**
         * Cause data to flow through the network.
         */
        producer0.accept(37);
        producer1.accept(43);
        producer2.accept(20);
    }
}
```

#### Example Output:

```
Funneled $37 dollars into this project.
Funneled $43 dollars into this project.
Funneled $20 dollars into this project.
```

### IfElse

#### Example Program:

```Java
package example;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.flow.IfElse;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.io.Printer;

public final class Example
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        /**
         * This actor merely simulates a data producer.
         */
        final Processor<String> producer = Processor.newConnector(stage);

        /**
         * This is the actor whose functionality is being demonstrated.
         */
        final IfElse<String> filter = IfElse.newIfElse(stage, x -> x.startsWith("A"));

        /**
         * This actor will print the messages that obeyed the condition.
         */
        final Printer<String> success = Printer.newPrintln(stage, "(%s) matched.");

        /**
         * This actor will print the messages that disobeyed the condition.
         */
        final Printer<String> failure = Printer.newPrintln(stage, "(%s) did not match.");

        /**
         * Connect the actors to form a network.
         */
        producer.dataOut().connect(filter.dataIn());
        filter.trueOut().connect(success.dataIn());
        filter.falseOut().connect(failure.dataIn());

        /**
         * Cause data to flow through the network.
         */
        producer.accept("Autumn");
        producer.accept("Elle");
        producer.accept("Ashley");
        producer.accept("Emma");
        producer.accept("Anna");
        producer.accept("Erin");
    }
}
```

#### Example Output:

```
(Autumn) matched.
(Elle) did not match.
(Ashley) matched.
(Emma) did not match.
(Anna) matched.
(Erin) did not match.
```

### LookupInserter

#### Example Program:

```Java
package example;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.flow.LookupInserter;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.io.Printer;
import java.io.IOException;

public final class Example
{
    public static void main (String[] args)
            throws IOException
    {
        final Stage stage = Cascade.newStage();

        /**
         * This actor merely simulates a data producer.
         */
        final Processor<Integer> processor = Processor.newConnector(stage);

        /**
         * This is the actor whose behavior is being demonstrated.
         */
        final LookupInserter<Integer> inserter = LookupInserter.newLookupInserter(stage);

        /**
         * These actors will print the results to standard-output.
         */
        final Printer<Integer> printer0 = Printer.newPrintln(stage, "Small = %s");
        final Printer<Integer> printer1 = Printer.newPrintln(stage, "Medium = %s");
        final Printer<Integer> printer2 = Printer.newPrintln(stage, "Large = %s");

        /**
         * Connect the actors to form a network.
         */
        processor.dataOut().connect(inserter.dataIn());
        inserter.selectIf(x -> x < 10).connect(printer0.dataIn());
        inserter.selectIf(x -> x < 100).connect(printer1.dataIn());
        inserter.dataOut().connect(printer2.dataIn());

        /**
         * Cause data to flow through the network.
         */
        processor.accept(3);
        processor.accept(19);
        processor.accept(5);
        processor.accept(123);
        processor.accept(7);
        processor.accept(41);
    }
}
```

#### Example Output:

```
Small = 3
Medium = 19
Small = 5
Large = 123
Small = 7
Medium = 41
```

### Mapper

#### Example Program:

#### Example Output:

### Minuteman

#### Example Program:

```Java
package example;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.io.Printer;
import com.mackenziehigh.socius.time.Minuteman;
import java.io.IOException;
import java.time.Instant;

public final class Example
{
    public static void main (String[] args)
            throws IOException
    {
        final Stage stage = Cascade.newStage();

        /**
         * This is the actor whose functionality is being demonstrated.
         */
        final Minuteman minuteman = Minuteman.newMinuteman(stage);

        /**
         * This actor will print the clock-ticks.
         */
        final Printer<Instant> printer = Printer.newPrintln(stage, "Tick = %s");

        /**
         * Connect the actors to form a network.
         */
        minuteman.clockOut().connect(printer.dataIn());

        /**
         * Start the clock; otherwise, nothing meaningful will happen.
         */
        minuteman.start();

        /**
         * Prevent the process from closing too soon.
         */
        System.in.read();
    }
}
```

#### Example Output:

```
Tick = 2019-02-13T04:47:00Z
Tick = 2019-02-13T04:48:00Z
Tick = 2019-02-13T04:49:00Z
Tick = 2019-02-13T04:50:00Z
Tick = 2019-02-13T04:51:00Z
```

### Oscillator

#### Example Program:

```Java
package example;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.io.Printer;
import com.mackenziehigh.socius.time.Oscillator;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.function.LongFunction;

public final class Example
{
    public static void main (String[] args)
            throws IOException
    {
        final Stage stage = Cascade.newStage();

        /**
         * This is the actor whose functionality is being demonstrated.
         * The waveform will ensure that (N) seconds pass between
         * tick (N - 1) and tick (N). As time progresses, (N) increases.
         */
        final LongFunction<Duration> waveform = (long N) -> Duration.ofSeconds(N);
        final Oscillator generator = Oscillator.newOscillator().withWaveform(waveform).build();

        /**
         * This actor will print the clock-ticks.
         */
        final Printer<Instant> printer = Printer.newPrintln(stage, "Tick = %s");

        /**
         * Connect the actors to form a network.
         */
        generator.clockOut().connect(printer.dataIn());

        /**
         * Start the clock; otherwise, nothing meaningful will happen.
         */
        generator.start();

        /**
         * Prevent the process from closing too soon.
         */
        System.in.read();
    }
}
```

#### Example Output:

```
Tick = 2019-02-13T04:57:56.842Z
Tick = 2019-02-13T04:57:56.844Z
Tick = 2019-02-13T04:57:57.845Z
Tick = 2019-02-13T04:57:59.846Z
Tick = 2019-02-13T04:58:02.846Z
Tick = 2019-02-13T04:58:06.846Z
Tick = 2019-02-13T04:58:11.847Z
Tick = 2019-02-13T04:58:17.847Z
Tick = 2019-02-13T04:58:24.848Z
Tick = 2019-02-13T04:58:32.848Z
Tick = 2019-02-13T04:58:41.849Z
Tick = 2019-02-13T04:58:51.849Z
```

### Printer

#### Example Program:

```Java
package example;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.io.Printer;
import java.io.IOException;

public final class Example
{
    public static void main (String[] args)
            throws IOException
    {
        final Stage stage = Cascade.newStage();

        /**
         * This actor merely simulates a data producer.
         */
        final Processor<String> processor = Processor.newConnector(stage);

        /**
         * These actors will print the results to standard-output.
         */
        final Printer<String> printer = Printer.newPrintln(stage, "Welcome to %s.");

        /**
         * Connect the actors to form a network.
         */
        processor.dataOut().connect(printer.dataIn());

        /**
         * Cause data to flow through the network.
         */
        processor.accept("Earth");
        processor.accept("Mars");
    }
}
```

#### Example Output:

```
Welcome to Earth.
Welcome to Mars.
```

### Processor

#### Example Program:

#### Example Output:

### Requester

#### Example Program:

#### Example Output:

### RoundRobin

#### Example Program:

```Java
package example;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.flow.RoundRobin;
import com.mackenziehigh.socius.io.Printer;
import java.io.IOException;

public final class Example
{
    public static void main (String[] args)
            throws IOException
    {
        final Stage stage = Cascade.newStage();

        /**
         * This actor merely simulates a data producer.
         */
        final Processor<String> processor = Processor.newConnector(stage);

        /**
         * This is the actor whose behavior is being demonstrated.
         */
        final RoundRobin<String> dispatcher = RoundRobin.newRoundRobin(stage, 3);

        /**
         * These actors will print the results to standard-output.
         */
        final Printer<String> printer0 = Printer.newPrintln(stage, "Printer #0 got message (%s).");
        final Printer<String> printer1 = Printer.newPrintln(stage, "Printer #1 got message (%s).");
        final Printer<String> printer2 = Printer.newPrintln(stage, "Printer #2 got message (%s).");

        /**
         * Connect the actors to form a network.
         */
        processor.dataOut().connect(dispatcher.dataIn());
        dispatcher.dataOut(0).connect(printer0.dataIn());
        dispatcher.dataOut(1).connect(printer1.dataIn());
        dispatcher.dataOut(2).connect(printer2.dataIn());

        /**
         * Cause data to flow through the network.
         */
        processor.accept("A");
        processor.accept("B");
        processor.accept("C");
        processor.accept("D");
        processor.accept("E");
        processor.accept("F");
    }
}
```

#### Example Output:

```
Printer #0 got message (A).
Printer #1 got message (B).
Printer #2 got message (C).
Printer #0 got message (D).
Printer #1 got message (E).
Printer #2 got message (F).
```

### Router

#### Example Program:

#### Example Output:

### ShuntingYard


#### Example Program:

#### Example Output:

### TableInserter

#### Example Program:

```Java
package example;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.flow.TableInserter;
import com.mackenziehigh.socius.io.Printer;
import java.io.IOException;
import java.util.function.Function;

public final class Example
{
    public static void main (String[] args)
            throws IOException
    {
        final Stage stage = Cascade.newStage();

        /**
         * This actor merely simulates a data producer.
         */
        final Processor<String> processor = Processor.newConnector(stage);

        /**
         * This is the actor whose behavior is being demonstrated.
         */
        final Function<String, Integer> keyFunction = x -> x.length();
        final TableInserter<Integer, String> inserter = TableInserter.<Integer, String>newTableInserter(stage, keyFunction);

        /**
         * These actors will print the results to standard-output.
         */
        final Printer<String> printer0 = Printer.newPrintln(stage, "len(five) = %s");
        final Printer<String> printer1 = Printer.newPrintln(stage, "len(six) = %s");
        final Printer<String> printer2 = Printer.newPrintln(stage, "other = %s");

        /**
         * Connect the actors to form a network.
         */
        processor.dataOut().connect(inserter.dataIn());
        inserter.selectIf(5).connect(printer0.dataIn());
        inserter.selectIf(6).connect(printer1.dataIn());
        inserter.dataOut().connect(printer2.dataIn());

        /**
         * Cause data to flow through the network.
         */
        processor.accept("Mercury");
        processor.accept("Venus");
        processor.accept("Saturn");
        processor.accept("Earth");
        processor.accept("Uranus");
        processor.accept("Neptune");
    }
}
```

#### Example Output:

```
other = Mercury
len(five) = Venus
len(six) = Saturn
len(five) = Earth
len(six) = Uranus
other = Neptune
```

### Unbatcher

#### Example Program:

#### Example Output:

### Valve

#### Example Program:

#### Example Output:

### Variable

#### Example Program:

#### Example Output:

### WakeupCaller

#### Example Program:

#### Example Output:


