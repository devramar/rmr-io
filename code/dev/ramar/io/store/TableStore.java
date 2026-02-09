package dev.ramar.io.store;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;

import dev.ramar.utils.FileLock;

/// -=== TableStore ===================================================================
/// Owns the table root layout and coordinates:
/// - single-writer lock
/// - ID subsystem startup (snapshot + journal replay)
/// - future CRUD entrypoints (not implemented here)
public class TableStore<T>
{
    public final Path tableRoot;

    public final Path lockPath;
    public final Path idClockPath;
    public final Path idSnapshotPath;
    public final Path idJournalPath;


    public final IDIndex idIndex;
    public final IDClock idClock;
    public final IDAllocator idAllocator;
    public final IDStore idStore;


    public TableStore(Path tableRoot)
    {
        this.tableRoot = tableRoot.toAbsolutePath().normalize();

        this.lockPath = this.tableRoot.resolve(".atom");

        this.idClockPath = this.tableRoot.resolve("ids.clock");
        this.idSnapshotPath = this.tableRoot.resolve("ids.snapshot");
        this.idJournalPath = this.tableRoot.resolve("ids.log");

        this.idIndex = new IDIndex();

        this.idClock = new IDClock(this.idClockPath);

        this.idAllocator = new IDAllocator(this.idClock);

        this.idStore = new IDStore(this.idSnapshotPath, this.idJournalPath);
    }



    public void startup() throws IOException
    {
        Files.createDirectories(this.tableRoot);

        try(var lck = FileLock.Blocking(this.lockPath))
        {
            this.idStore.startup(this.idIndex);
        }
    }


    public FileLock lockTable()
    {
        return FileLock.Blocking(this.lockPath);
    }


}