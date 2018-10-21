package com.mackenziehigh.socius.dev;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.socius.io.Printer;
import com.mackenziehigh.socius.time.FrequencyOscillator;
import java.util.concurrent.TimeUnit;

public final class Main02
{
    public static void main (String[] args)
    {
        final Stage stage = Cascade.newStage();

        final FrequencyOscillator freq = FrequencyOscillator
                .newOscillator()
                .withWaveform(x -> TimeUnit.SECONDS.toNanos(x))
                .build()
                .start();

        final Printer p = Printer.newPrintln(stage);

        freq.clockOut().connect(p.dataIn());
    }
}
