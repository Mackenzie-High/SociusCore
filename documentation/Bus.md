# Bus

## Example 

**Code:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.Bus;
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
        final Processor<String> producer0 = Processor.fromIdentityScript(stage);
        final Processor<String> producer1 = Processor.fromIdentityScript(stage);
        final Processor<String> producer2 = Processor.fromIdentityScript(stage);

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
         */
        producer0.dataOut().connect(bus.dataIn("producer0"));
        producer1.dataOut().connect(bus.dataIn("producer1"));
        producer2.dataOut().connect(bus.dataIn("producer2"));
        consumer0.dataIn().connect(bus.dataOut("consumer0"));
        consumer1.dataIn().connect(bus.dataOut("consumer1"));
        consumer2.dataIn().connect(bus.dataOut("consumer2"));

        /**
         * Cause data to flow through the network.
         */
        producer0.accept("Message #1");
        producer1.accept("Message #2");
        producer2.accept("Message #3");
    }
}
```

**Output:**

```
(Consumer #2) got (Message #1).
(Consumer #1) got (Message #1).
(Consumer #0) got (Message #1).
(Consumer #2) got (Message #2).
(Consumer #1) got (Message #2).
(Consumer #0) got (Message #2).
(Consumer #2) got (Message #3).
(Consumer #1) got (Message #3).
(Consumer #0) got (Message #3).
```
