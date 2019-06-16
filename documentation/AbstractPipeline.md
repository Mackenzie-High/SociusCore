# Abstract Pipeline

## Example

**Code - Concrete Class:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.AbstractPipeline;

/**
 * Converts integer messages to their textual binary representations.
 */
public final class BinaryConverter
        extends AbstractPipeline<Integer, String>
{
    public BinaryConverter (final Stage stage)
    {
        super(stage);
    }

    @Override
    protected void onMessage (final Integer message)
            throws Throwable
    {
        final String text = Integer.toBinaryString(message);
        sendFrom(text);
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
        final BinaryConverter converter = new BinaryConverter(stage);

        /**
         * This actors will print the results to standard-output.
         */
        final Printer<String> printer = Printer.newPrintln(stage, "X = %s");

        /**
         * Connect the actors to form a network.
         */
        producer.dataOut().connect(converter.dataIn());
        converter.dataOut().connect(printer.dataIn());

        /**
         * Cause data to flow through the network.
         */
        producer.accept(1);
        producer.accept(2);
        producer.accept(3);
        producer.accept(4);
        producer.accept(5);
        producer.accept(6);
        producer.accept(7);
        producer.accept(8);
    }
}
```

**Output:**

```
X = 1
X = 10
X = 11
X = 100
X = 101
X = 110
X = 111
X = 1000
```
