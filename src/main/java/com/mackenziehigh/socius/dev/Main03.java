package com.mackenziehigh.socius.dev;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.io.Printer;
import com.mackenziehigh.socius.time.Minuteman;

public final class Main03
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        final Minuteman mm = Minuteman.newMinuteman(stage).start();

        final Printer p = Printer.newPrintln(stage);

        mm.clockOut().connect(p.dataIn());
    }
}
