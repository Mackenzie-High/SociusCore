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
        sendFrom("Current Sum of Primes = " + sum);
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
 * A floor that sums non-negative odd numbers.
 */
public final class OddSummationFloor
        extends AbstractPipeline<Integer, String>
        implements LookupTower.PredicatedFloor<Integer, String>
{
    private long sum = 0;

    public OddSummationFloor (final Stage stage)
    {
        super(stage);
    }

    @Override
    public boolean test (final Integer message)
    {
        return message > 0 && message % 2 != 0;
    }

    @Override
    protected void onMessage (final Integer message)
            throws Throwable
    {
        sum += message;
        sendFrom("Current Sum of Odd Numbers = " + sum);
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
        final Printer<String> printer = Printer.newPrintln(stage, "State: %s");

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
State: Current Sum of Primes = 2
State: Current Sum of Primes = 5
State: Current Sum of Even Numbers = 4
State: Current Sum of Primes = 10
State: Current Sum of Even Numbers = 10
State: Current Sum of Primes = 17
State: Current Sum of Even Numbers = 18
State: Current Sum of Odd Numbers = 9
State: Current Sum of Even Numbers = 28
State: Current Sum of Primes = 28
State: Current Sum of Even Numbers = 40
State: Current Sum of Primes = 41
State: Current Sum of Even Numbers = 54
State: Current Sum of Odd Numbers = 24
```
