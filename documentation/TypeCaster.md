# TypeCaster

## Example

**Code:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.Printer;
import com.mackenziehigh.socius.core.Processor;
import com.mackenziehigh.socius.core.TypeCaster;

public final class Example
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        /**
         * This actor merely simulates a data producer.
         */
        final Processor<Object> producer = Processor.fromIdentityScript(stage);

        /**
         * This is the actor whose functionality is being demonstrated.
         */
        final TypeCaster<Object, String> caster = TypeCaster.newTypeCaster(stage, String.class);

        /**
         * These actors will print the results to standard-output.
         */
        final Printer<String> success = Printer.newPrintln(stage, "Successfully cast (%s) to type String.");
        final Printer<Object> failure = Printer.newPrintln(stage, "Failed to cast (%s) to type String.");

        /**
         * Connect the actors to form a network.
         */
        producer.dataOut().connect(caster.dataIn());
        caster.dataOut().connect(success.dataIn());
        caster.errorOut().connect(failure.dataIn());

        /**
         * Cause data to flow through the network.
         */
        producer.accept("10"); // 10 is a String.
        producer.accept(13); // 13 is an Integer.
    }
}
```

**Output:**

```
Successfully cast (10) to type String.
Failed to cast (13) to type String.
```
