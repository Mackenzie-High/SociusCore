# Fanout

## Example

**Code:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.Fanout;
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
        final Processor<String> commander = Processor.fromIdentityScript(stage);

        /**
         * This is the actor whose functionality is being demonstrated.
         */
        final Fanout<String> fanout = Fanout.newFanout(stage);

        /**
         * These actors will print the commands to standard-output.
         */
        final Printer<String> silo0 = Printer.newPrintln(stage, "Silo #0 received command (%s).");
        final Printer<String> silo1 = Printer.newPrintln(stage, "Silo #1 received command (%s).");
        final Printer<String> silo2 = Printer.newPrintln(stage, "Silo #2 received command (%s).");

        /**
         * Connect the actors to form a network.
         */
        commander.dataOut().connect(fanout.dataIn());
        fanout.dataOut("S0").connect(silo0.dataIn());
        fanout.dataOut("S1").connect(silo1.dataIn());
        fanout.dataOut("S2").connect(silo2.dataIn());

        /**
         * Cause data to flow through the network.
         */
        commander.accept("Goto DEFCON 1");
        commander.accept("Launch Strike #1");
        commander.accept("Launch Strike #2");
    }
}
```

**Output:**

```
Silo #0 received command (Goto DEFCON 1).
Silo #1 received command (Goto DEFCON 1).
Silo #2 received command (Goto DEFCON 1).
Silo #0 received command (Launch Strike #1).
Silo #1 received command (Launch Strike #1).
Silo #2 received command (Launch Strike #1).
Silo #0 received command (Launch Strike #2).
Silo #1 received command (Launch Strike #2).
Silo #2 received command (Launch Strike #2).
```
