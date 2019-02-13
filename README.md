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
### Bus
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




