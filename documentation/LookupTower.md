# Lookup Tower

## Example

**Code - Floor for Prime Numbers:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.AbstractPipeline;
import com.mackenziehigh.socius.core.LookupTower;
import java.math.BigInteger;

/**
 * A floor that sums prime numbers.
 */
public final class PrimeSummationFloor
        extends AbstractPipeline<Integer, String>
        implements LookupTower.PredicatedFloor<Integer, String>
{
    private long sum = 0;

    public PrimeSummationFloor (final Stage stage)
    {
        super(stage);
    }

    @Override
    public boolean test (final Integer message)
    {
        return BigInteger.valueOf(message).isProbablePrime(100);
    }

    @Override
    protected void onMessage (final Integer message)
            throws Throwable
    {
        sum += message;
        sendFrom(String.format("Add (%d) to Sum of Prime Numbers. Sum = (%d).", message, sum));
    }
}
```

**Code - Floor for Even Numbers:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.AbstractPipeline;
import com.mackenziehigh.socius.core.LookupTower;

/**
 * A floor that sums non-negative even numbers.
 */
public final class EvenSummationFloor
        extends AbstractPipeline<Integer, String>
        implements LookupTower.PredicatedFloor<Integer, String>
{
    private long sum = 0;

    public EvenSummationFloor (final Stage stage)
    {
        super(stage);
    }

    @Override
    public boolean test (final Integer message)
    {
        return message > 0 && message % 2 == 0;
    }

    @Override
    protected void onMessage (final Integer message)
            throws Throwable
    {
        sum += message;
        sendFrom("Current Sum of Even Numbers = " + sum);
    }
}
```

**Code - Floor for Odd Numbers:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.AbstractPipeline;
import com.mackenziehigh.socius.core.LookupTower;

/**
 * A floor that sums non-negative even numbers.
 */
public final class EvenSummationFloor
        extends AbstractPipeline<Integer, String>
        implements LookupTower.PredicatedFloor<Integer, String>
{
    private long sum = 0;

    public EvenSummationFloor (final Stage stage)
    {
        super(stage);
    }

    @Override
    public boolean test (final Integer message)
    {
        return message > 0 && message % 2 == 0;
    }

    @Override
    protected void onMessage (final Integer message)
            throws Throwable
    {
        sum += message;
        sendFrom(String.format("Add (%d) to Sum of Even Numbers. Sum = (%d).", message, sum));
    }
}
```

**Code - Main Class:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.LookupTower;
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
         * These are the floors (pipelines) that messages will be routed to.
         */
        final PrimeSummationFloor floor1 = new PrimeSummationFloor(stage);
        final EvenSummationFloor floor2 = new EvenSummationFloor(stage);
        final OddSummationFloor floor3 = new OddSummationFloor(stage);

        /**
         * This is the actor whose functionality is being demonstrated.
         */
        final LookupTower<Integer, String> tower = LookupTower.<Integer, String>newLookupTower(stage)
                .withFloor(floor1)
                .withFloor(floor2)
                .withFloor(floor3)
                .build();

        /**
         * This actor will print the results to standard-output.
         */
        final Printer<String> printer = Printer.newPrintln(stage, "Announcement: %s");

        /**
         * Connect the actors to form a network.
         */
        producer.dataOut().connect(tower.dataIn());
        tower.dataOut().connect(printer.dataIn());

        /**
         * Cause data to flow through the network.
         */
        producer.accept(2); //  prime, sum = 2
        producer.accept(3); //  prime, sum = 5
        producer.accept(4); //  even,  sum = 4
        producer.accept(5); //  prime, sum = 10
        producer.accept(6); //  even,  sum = 10
        producer.accept(7); //  prime, sum = 17
        producer.accept(8); //  even,  sum = 18
        producer.accept(9); //  odd,   sum = 9
        producer.accept(10); // even,  sum = 28
        producer.accept(11); // prime, sum = 28
        producer.accept(12); // even,  sum = 40
        producer.accept(13); // prime, sum = 41
        producer.accept(14); // even,  sum = 54
        producer.accept(15); // odd,   sum = 24
    }
}
```

**Output:**

```
Announcement: Add (2) to Sum of Prime Numbers. Sum = (2).
Announcement: Add (3) to Sum of Prime Numbers. Sum = (5).
Announcement: Add (4) to Sum of Even Numbers. Sum = (4).
Announcement: Add (5) to Sum of Prime Numbers. Sum = (10).
Announcement: Add (6) to Sum of Even Numbers. Sum = (10).
Announcement: Add (7) to Sum of Prime Numbers. Sum = (17).
Announcement: Add (8) to Sum of Even Numbers. Sum = (18).
Announcement: Add (9) to Sum of Odd Numbers. Sum = (9).
Announcement: Add (10) to Sum of Even Numbers. Sum = (28).
Announcement: Add (11) to Sum of Prime Numbers. Sum = (28).
Announcement: Add (12) to Sum of Even Numbers. Sum = (40).
Announcement: Add (13) to Sum of Prime Numbers. Sum = (41).
Announcement: Add (14) to Sum of Even Numbers. Sum = (54).
Announcement: Add (15) to Sum of Odd Numbers. Sum = (24).
```
