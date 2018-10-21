package com.mackenziehigh.socius.dev;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.io.FileWatcher;
import com.mackenziehigh.socius.io.Printer;
import com.mackenziehigh.socius.time.Clock;
import java.io.File;

public final class Main04
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        final Clock now = Clock.newClock().build().start();
        final FileWatcher fw = FileWatcher.newFileWatcher(stage, new File("/tmp//folder"));
        final Printer p = Printer.newPrintln(stage);

        now.clockOut().connect(fw.clockIn());
        fw.eventsOut().connect(p.dataIn());
    }
}
