# Minuteman

## Example

**Code:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.Minuteman;
import com.mackenziehigh.socius.core.Printer;
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
        minuteman.dataOut().connect(printer.dataIn());

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

**Output:**

```
Tick = 2019-05-19T23:23:00Z
Tick = 2019-05-19T23:24:00Z
Tick = 2019-05-19T23:25:00Z
```
