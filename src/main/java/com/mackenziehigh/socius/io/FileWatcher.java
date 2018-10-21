package com.mackenziehigh.socius.io;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.flow.Processor;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

/**
 * A simple file watcher.
 */
public final class FileWatcher
{
    /**
     * Event Type.
     */
    public enum FileWatcherEventType
    {
        /**
         * Meaning: The file was detected.
         */
        DETECTION,
        /**
         * Meaning: The file was removed.
         */
        REMOVAL,
        /**
         * Meaning: Either the size of the file or the modification-time of the file changed.
         */
        MODIFICATION,
    }

    /**
     * Event.
     */
    public static final class FileWatcherEvent
    {
        private final FileWatcherEventType type;

        private final File file;

        private FileWatcherEvent (final FileWatcherEventType type,
                                  final File file)
        {
            this.type = type;
            this.file = file;
        }

        public FileWatcherEventType type ()
        {
            return type;
        }

        public File file ()
        {
            return file;
        }

        @Override
        public String toString ()
        {
            return String.format("%s: %s", type(), file().getAbsolutePath());
        }
    }

    private static final class Record
    {
        public long modtime = 0;

        public long size = 0;
    }

    private final Processor<Instant> procClock;

    private final Processor<FileWatcherEvent> procEventsOut;

    private final Set<File> visited = Sets.newConcurrentHashSet();

    private final Map<File, Record> known = Maps.newConcurrentMap();

    private final File monitored;

    private FileWatcher (final Stage stage,
                         final File root)
    {
        this.procClock = Processor.newProcessor(stage, this::onTick);
        this.procEventsOut = Processor.newProcessor(stage);
        this.monitored = Objects.requireNonNull(root, "root");
    }

    public Input<Instant> clockIn ()
    {
        return procClock.dataIn();
    }

    public Output<Instant> clockOut ()
    {
        return procClock.dataOut();
    }

    public Output<FileWatcherEvent> eventsOut ()
    {
        return procEventsOut.dataOut();
    }

    private void onTick (final Instant tick)
    {
        if (monitored.exists())
        {
            visited.clear();
            enumerateFiles(monitored);
            detectAddAndRemove();
            detectModifications();
        }
    }

    private void enumerateFiles (final File root)
    {
        if (visited.contains(root))
        {
            return;
        }

        visited.add(root);

        final File[] list = root.listFiles();

        if (list != null)
        {
            Arrays.asList(list).forEach(x -> enumerateFiles(x));
        }
    }

    private void detectAddAndRemove ()
    {
        Sets.difference(visited, known.keySet()).forEach(x -> add(x));
        Sets.difference(known.keySet(), visited).forEach(x -> remove(x));
    }

    private void add (final File file)
    {
        final Record record = new Record();
        record.modtime = file.lastModified();
        record.size = file.length();
        known.put(file, record);

        final FileWatcherEvent event = new FileWatcherEvent(FileWatcherEventType.DETECTION, file);
        procEventsOut.dataIn().send(event);
    }

    private void remove (final File file)
    {
        known.remove(file);

        final FileWatcherEvent event = new FileWatcherEvent(FileWatcherEventType.REMOVAL, file);
        procEventsOut.dataIn().send(event);
    }

    private void detectModifications ()
    {
        for (Entry<File, Record> entry : known.entrySet())
        {
            final File file = entry.getKey();
            final Record record = entry.getValue();
            final boolean case1 = file.lastModified() != record.modtime;
            final boolean case2 = file.length() != record.size;
            final boolean modified = case1 | case2;

            if (modified)
            {
                record.modtime = file.lastModified();
                record.size = file.length();

                final FileWatcherEvent event = new FileWatcherEvent(FileWatcherEventType.MODIFICATION, file);
                procEventsOut.dataIn().send(event);
            }
        }
    }

    public static FileWatcher newFileWatcher (final Stage stage,
                                              final File root)
    {
        return new FileWatcher(stage, root);
    }

}
