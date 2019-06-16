# Oscillator

## Example

**Code:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.Oscillator;
import com.mackenziehigh.socius.core.Printer;
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
         * The waveform will ensure that (N + 1) seconds pass between
         * tick (N) and tick (N + 1). As time progresses, (N) increases from zero.
         */
        final LongFunction<Duration> waveform = (long N) -> Duration.ofSeconds(N + 1);
        final Oscillator generator = Oscillator.newOscillator().withWaveform(waveform).build();

        /**
         * This actor will print the clock-ticks.
         */
        final Printer<Instant> printer = Printer.newPrintln(stage, "Tick = %s");

        /**
         * Connect the actors to form a network.
         */
        generator.dataOut().connect(printer.dataIn());

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

**Output:**

```
Tick = 2019-05-20T00:50:47.118952Z
Tick = 2019-05-20T00:50:48.137780Z
Tick = 2019-05-20T00:50:50.139272Z
Tick = 2019-05-20T00:50:53.140484Z
Tick = 2019-05-20T00:50:57.141767Z
```
