# IfElse

## Example

**Code:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.IfElse;
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
        final IfElse<String> filter = IfElse.newIfElse(stage, x -> x.startsWith("A"));

        /**
         * This actor will print the messages that obeyed the condition.
         */
        final Printer<String> success = Printer.newPrintln(stage, "(%s) matched.");

        /**
         * This actor will print the messages that disobeyed the condition.
         */
        final Printer<String> failure = Printer.newPrintln(stage, "(%s) did not match.");

        /**
         * Connect the actors to form a network.
         */
        producer.dataOut().connect(filter.dataIn());
        filter.trueOut().connect(success.dataIn());
        filter.falseOut().connect(failure.dataIn());

        /**
         * Cause data to flow through the network.
         */
        producer.accept("Autumn");
        producer.accept("Elle");
        producer.accept("Ashley");
        producer.accept("Emma");
        producer.accept("Anna");
        producer.accept("Erin");
    }
}
```

**Output:**

```
(Autumn) matched.
(Elle) did not match.
(Ashley) matched.
(Emma) did not match.
(Anna) matched.
(Erin) did not match.
```
