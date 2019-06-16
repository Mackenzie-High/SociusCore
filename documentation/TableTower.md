# Table Tower

## Example

**Code - Account Class:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.AbstractPipeline;

/**
 * A floor that acts like a bank account.
 */
public final class Account
        extends AbstractPipeline<Transaction, String>
{
    private final String owner;

    private long balance = 0;

    public Account (final Stage stage,
                    final String owner)
    {
        super(stage);
        this.owner = owner;
    }

    @Override
    protected void onMessage (final Transaction message)
            throws Throwable
    {
        balance += message.amount();
        sendFrom(String.format("The new balance of (%s)'s account is $(%d)", owner, balance));
    }

    public String owner ()
    {
        return owner;
    }
}
```

**Code - Transaction Class:**

```java
package examples;

/**
 * A message object for manipulating an account.
 */
public final class Transaction
{
    private final String accountHolder;

    private final long amount;

    public Transaction (final String accountHolder,
                        final long amount)
    {
        this.accountHolder = accountHolder;
        this.amount = amount;
    }

    public String accountHolder ()
    {
        return accountHolder;
    }

    public long amount ()
    {
        return amount;
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
import com.mackenziehigh.socius.core.TableTower;

public final class Example
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        /**
         * This actor merely simulates a data producer.
         */
        final Processor<Transaction> producer = Processor.fromIdentityScript(stage);

        /**
         * These are the floors (pipelines) that messages will be routed to.
         */
        final Account floor1 = new Account(stage, "Erin");
        final Account floor2 = new Account(stage, "Emma");
        final Account floor3 = new Account(stage, "Elle");

        /**
         * This is the actor whose functionality is being demonstrated.
         */
        final TableTower<String, Transaction, String> tower = TableTower.<String, Transaction, String>newTableTower(stage)
                .withFloor(floor1.owner(), floor1)
                .withFloor(floor2.owner(), floor2)
                .withFloor(floor3.owner(), floor3)
                .withKeyFunction(Transaction::accountHolder)
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
        producer.accept(new Transaction("Erin", +10_000));
        producer.accept(new Transaction("Emma", +20_000));
        producer.accept(new Transaction("Elle", +30_000));
        producer.accept(new Transaction("Erin", -300));
        producer.accept(new Transaction("Emma", -100));
        producer.accept(new Transaction("Elle", -700));
    }
}
```

**Output:**

```
Announcement: The new balance of (Erin)'s account is $(10000)
Announcement: The new balance of (Emma)'s account is $(20000)
Announcement: The new balance of (Elle)'s account is $(30000)
Announcement: The new balance of (Erin)'s account is $(9700)
Announcement: The new balance of (Emma)'s account is $(19900)
Announcement: The new balance of (Elle)'s account is $(29300)
```
