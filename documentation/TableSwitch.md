# TableSwitch

## Example

**Code:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.Printer;
import com.mackenziehigh.socius.core.Processor;
import com.mackenziehigh.socius.core.TableSwitch;
import java.io.IOException;
import java.util.function.Function;

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
        final Function<String, Integer> keyFunction = x -> x.length();
        final TableSwitch<Integer, String> inserter = TableSwitch.newTableSwitch(stage, keyFunction);

        /**
         * These actors will print the results to standard-output.
         */
        final Printer<String> printer0 = Printer.newPrintln(stage, "len(five) = %s");
        final Printer<String> printer1 = Printer.newPrintln(stage, "len(six) = %s");
        final Printer<String> printer2 = Printer.newPrintln(stage, "other = %s");

        /**
         * Connect the actors to form a network.
         */
        processor.dataOut().connect(inserter.dataIn());
        inserter.selectIf(5).connect(printer0.dataIn());
        inserter.selectIf(6).connect(printer1.dataIn());
        inserter.dataOut().connect(printer2.dataIn());

        /**
         * Cause data to flow through the network.
         */
        processor.accept("Mercury");
        processor.accept("Venus");
        processor.accept("Saturn");
        processor.accept("Earth");
        processor.accept("Uranus");
        processor.accept("Neptune");
    }
}
```

**Output:**

```
other = Mercury
len(five) = Venus
len(six) = Saturn
len(five) = Earth
len(six) = Uranus
other = Neptune
```
