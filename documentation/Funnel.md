# Funnel

## Example

**Code:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.Funnel;
import com.mackenziehigh.socius.core.Printer;
import com.mackenziehigh.socius.core.Processor;

public final class Example
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        /**
         * These actors merely simulate data producers.
         */
        final Processor<Integer> producer0 = Processor.fromIdentityScript(stage);
        final Processor<Integer> producer1 = Processor.fromIdentityScript(stage);
        final Processor<Integer> producer2 = Processor.fromIdentityScript(stage);

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

**Output:**

```
Funneled $37 dollars into this project.
Funneled $43 dollars into this project.
Funneled $20 dollars into this project.
```
