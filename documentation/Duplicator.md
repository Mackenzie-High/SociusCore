# Duplicator

## Example

**Code:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.Duplicator;
import com.mackenziehigh.socius.core.Printer;
import com.mackenziehigh.socius.core.Processor;

public final class Example
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        /**
         * This actor merely simulates a data producer.
         */
        final Processor<String> producer = Processor.fromIdentityScript(stage);

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

**Output:**

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
