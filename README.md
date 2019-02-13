# Socius 

Note: This project is still awaiting its first formal release.

Socius provides commonly used actor types based on the minimalist [Cascade](https://github.com/Mackenzie-High/Cascade) framework. 

## Related Projects:

* [Socius - Core](#Socius): Base Components. 
* [Socius - Nats](https://github.com/Mackenzie-High/SociusNats): Provides the ability to connect to NATS-based message-oriented-middleware.
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
### Clock
### CollectionSink
### DelayedSender
### Duplicator
### Fanout
### Filter
### Funnel
### IfElse
### LookupInserter
### Mapper
### Minuteman
### Oscillator
### Printer
### Processor
### Requester
### RoundRobin
### Router
### ShuntingYard
### TableInserter
### Unbatcher
### Valve
### Variable
### WakeupCaller




