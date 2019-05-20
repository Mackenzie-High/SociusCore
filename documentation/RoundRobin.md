# RoundRobin

## Example

**Code:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.Printer;
import com.mackenziehigh.socius.core.Processor;
import com.mackenziehigh.socius.core.RoundRobin;
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
        final Processor<String> processor = Processor.fromIdentityScript(stage);

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

**Output:**

```
Printer #0 got message (A).
Printer #1 got message (B).
Printer #2 got message (C).
Printer #0 got message (D).
Printer #1 got message (E).
Printer #2 got message (F).
```
