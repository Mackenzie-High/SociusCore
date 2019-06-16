# LookupSwitch

## Example

**Code:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.LookupSwitch;
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
        final Processor<Integer> processor = Processor.fromIdentityScript(stage);

        /**
         * This is the actor whose behavior is being demonstrated.
         */
        final LookupSwitch<Integer> inserter = LookupSwitch.newLookupSwitch(stage);

        /**
         * These actors will print the results to standard-output.
         */
        final Printer<Integer> printer0 = Printer.newPrintln(stage, "Small = %s");
        final Printer<Integer> printer1 = Printer.newPrintln(stage, "Medium = %s");
        final Printer<Integer> printer2 = Printer.newPrintln(stage, "Large = %s");

        /**
         * Connect the actors to form a network.
         */
        processor.dataOut().connect(inserter.dataIn());
        inserter.selectIf(x -> x < 10).connect(printer0.dataIn());
        inserter.selectIf(x -> x < 100).connect(printer1.dataIn());
        inserter.dataOut().connect(printer2.dataIn());

        /**
         * Cause data to flow through the network.
         */
        processor.accept(3);
        processor.accept(19);
        processor.accept(5);
        processor.accept(123);
        processor.accept(7);
        processor.accept(41);
    }
}
```

**Output:**
```
Small = 3
Medium = 19
Small = 5
Large = 123
Small = 7
Medium = 41
```
