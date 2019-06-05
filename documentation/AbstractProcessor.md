# Abstract Processor

## Example

**Code - Concrete Class:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.AbstractProcessor;

/**
 * Multiplies messages by a coefficient and forwards the results.
 */
public final class Multiplier
        extends AbstractProcessor<Integer>
{
    private final int coefficient;

    public Multiplier (final Stage stage,
                       final int coefficent)
    {
        super(stage);
        this.coefficient = coefficent;
    }

    @Override
    protected void onMessage (final Integer message)
            throws Throwable
    {
        sendFrom(coefficient * message);
    }
}
```

**Code - Main Class:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
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
        final Processor<Integer> producer = Processor.fromIdentityScript(stage);

        /**
         * This is the actor whose functionality is being demonstrated.
         */
        final Multiplier multiplier = new Multiplier(stage, 13);

        /**
         * This actors will print the results to standard-output.
         */
        final Printer<Integer> printer = Printer.newPrintln(stage, "X = %d");

        /**
         * Connect the actors to form a network.
         */
        producer.dataOut().connect(multiplier.dataIn());
        multiplier.dataOut().connect(printer.dataIn());

        /**
         * Cause data to flow through the network.
         */
        producer.accept(1);
        producer.accept(2);
        producer.accept(3);
    }
}
```

**Output:**

```
X = 13
X = 26
X = 39
```
