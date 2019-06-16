# Printer

## Example

**Code:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.Printer;
import com.mackenziehigh.socius.core.Processor;
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

**Output:**

```
Welcome to Earth.
Welcome to Mars.
```
