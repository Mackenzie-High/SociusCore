# Clock

## Example 

**Code:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.Clock;
import com.mackenziehigh.socius.core.Printer;
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
        clock.dataOut().connect(printer.dataIn());
    }
}
```

**Output:**

```
Current Time = 2019-05-19T22:45:15.202895Z
Current Time = 2019-05-19T22:45:16.201168Z
Current Time = 2019-05-19T22:45:17.201130Z
Current Time = 2019-05-19T22:45:18.201141Z
Current Time = 2019-05-19T22:45:19.201218Z
Current Time = 2019-05-19T22:45:20.201133Z
Current Time = 2019-05-19T22:45:21.201123Z
```
