# Abstract Trampoline

## Example

**Code - Finite State Machine:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.AbstractTrampoline;

/**
 * Rocket Launch Control as a Finite State Machine.
 */
public final class Launcher
        extends AbstractTrampoline<Integer, String>
{
    public Launcher (final Stage stage)
    {
        super(stage);
    }

    @Override
    protected State<Integer> onInitial (final Integer message)
            throws Throwable
    {
        sendFrom("Begin Preparing Rocket for Launch.");
        return this::onFuelUp;
    }

    private State<Integer> onFuelUp (final Integer quantity)
    {
        sendFrom(String.format("Fuel Loaded = %d kg", quantity));
        return this::onCrewUp;
    }

    private State<Integer> onCrewUp (final Integer count)
    {
        sendFrom(String.format("Crew Loaded = %d astronauts", count));
        return this::onCountDown;
    }

    private State<Integer> onCountDown (final Integer seconds)
    {
        if (seconds < 0)
        {
            sendFrom("Abort!");
            return this::onInitial;
        }
        else if (seconds == 0)
        {
            sendFrom("Launch!");
            return this::onInitial;
        }
        else
        {
            sendFrom("Countdown = " + seconds);
            sendTo(seconds - 1);
            return this::onCountDown;
        }
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
        final Launcher converter = new Launcher(stage);

        /**
         * This actors will print the results to standard-output.
         */
        final Printer<String> printer = Printer.newPrintln(stage, "Announcement: %s");

        /**
         * Connect the actors to form a network.
         */
        producer.dataOut().connect(converter.dataIn());
        converter.dataOut().connect(printer.dataIn());

        /**
         * Cause data to flow through the network.
         */
        producer.accept(0);
        producer.accept(25000);
        producer.accept(3);
        producer.accept(10);
    }
}
```

**Output:**

```
Announcement: Begin Preparing Rocket for Launch.
Announcement: Fuel Loaded = 25000 kg
Announcement: Crew Loaded = 3 astronauts
Announcement: Countdown = 10
Announcement: Countdown = 9
Announcement: Countdown = 8
Announcement: Countdown = 7
Announcement: Countdown = 6
Announcement: Countdown = 5
Announcement: Countdown = 4
Announcement: Countdown = 3
Announcement: Countdown = 2
Announcement: Countdown = 1
Announcement: Launch!
```
