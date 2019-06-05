# Abstract Pushdown Automaton

## Example

**Code - Automaton Class:**

```java
package examples;

import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.core.AbstractPushdownAutomaton;

/**
 * Computes the areas of various geometric two-dimensional shapes.
 */
public final class AreaComputer
        extends AbstractPushdownAutomaton<Integer, String>
{
    private int inputX;

    private int inputY;

    public AreaComputer (final Stage stage)
    {
        super(stage);
    }

    @Override
    protected void onInitial (final Integer message)
            throws Throwable
    {
        if (message == 1)
        {
            then(this::readX);
            then(this::computeAreaOfSquare);
            then(this::reset);
        }
        else if (message == 2)
        {
            then(this::readX);
            then(this::computeAreaOfCircle);
            then(this::reset);
        }
        else if (message == 3)
        {
            then(this::readX);
            then(this::readY);
            then(this::computeAreaOfRectangle);
            then(this::reset);
        }
        else if (message == 4)
        {
            then(this::readX);
            then(this::readY);
            then(this::computeAreaOfTriangle);
            then(this::reset);
        }
        else
        {
            sendFrom("Unknown Option: " + message);
            then(this::reset);
        }
    }

    private void readX (final Integer message)
    {
        sendFrom("X = " + message);
        inputX = message;
    }

    private void readY (final Integer message)
    {
        sendFrom("Y = " + message);
        inputY = message;
    }

    private void computeAreaOfSquare ()
    {
        final int area = inputX * inputX;
        sendFrom(String.format("Area of Square = %d * %d = %d", inputX, inputX, area));
    }

    private void computeAreaOfRectangle ()
    {
        final int area = inputX * inputY;
        sendFrom(String.format("Area of Rectangle = %d * %d = %d", inputX, inputY, area));
    }

    private void computeAreaOfTriangle ()
    {
        final double area = (inputX * inputY) / 2;
        sendFrom(String.format("Area of Triangle = (%d * %d) / 2 = %.2f", inputX, inputY, area));
    }

    private void computeAreaOfCircle ()
    {
        final double area = 3.14 * (inputX * inputX);
        sendFrom(String.format("Area of Circle = 3.14 * (%d)^2 = %.2f", inputX, area));
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
        final AreaComputer computer = new AreaComputer(stage);

        /**
         * This actors will print the results to standard-output.
         */
        final Printer<String> printer = Printer.newPrintln(stage, "Announcement: %s");

        /**
         * Connect the actors to form a network.
         */
        producer.dataOut().connect(computer.dataIn());
        computer.dataOut().connect(printer.dataIn());

        /**
         * Cause data to flow through the network.
         */
        producer.accept(1); // Square
        producer.accept(10);
        producer.accept(2); // Circle
        producer.accept(21);
        producer.accept(3); // Rectangle
        producer.accept(32);
        producer.accept(33);
        producer.accept(4); // Triangle
        producer.accept(44);
        producer.accept(45);
    }
}
```

**Output:**

```
Announcement: X = 10
Announcement: Area of Square = 10 * 10 = 100
Announcement: X = 21
Announcement: Area of Circle = 3.14 * (21)^2 = 1384.74
Announcement: X = 32
Announcement: Y = 33
Announcement: Area of Rectangle = 32 * 33 = 1056
Announcement: X = 44
Announcement: Y = 45
Announcement: Area of Triangle = (44 * 45) / 2 = 990.00
```
