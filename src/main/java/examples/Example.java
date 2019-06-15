package examples;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.Printer;
import com.mackenziehigh.socius.core.Processor;
import com.mackenziehigh.socius.core.Valve;

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
        final Valve<String> valve = Valve.newOpenValve(stage);

        /**
         * This actor will print the results to standard-output.
         */
        final Printer<String> printer = Printer.newPrintln(stage, "Announcement: %s");

        /**
         * Connect the actors to form a network.
         */
        producer.dataOut().connect(valve.dataIn());
        valve.dataOut().connect(printer.dataIn());

        /**
         * Cause data to flow through the network.
         */
        producer.accept("");
    }
}
