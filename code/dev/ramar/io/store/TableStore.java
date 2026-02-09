package dev.ramar.io.store;

import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;

/// GPT-Made
/// entry-point for table storage of entries.
/// the strategy:
///  - make a class that extends TableStore (ie UserTableStore)
///  - construct with your table root path & aggregate as many
///    TableFileEntries that u want.
///  - TableStore is meant to handle ids, and locking mechanisms of the table
///  - if you want to ensure what you're doing is respected by the TableStore:
///    - use `FileLock.Blocking(this.lockPath)` to ensure you get atomic access
///      * WRITE ONLY is necessary to follow this. if you're reading just read
///    - write with `AtomicOutputStream` to ensure what you're saving saves 
///      correctly.
///  - want a new ID? go to `.idAllocator@newId`
public class TableStore<T>
{

    // -=== Fields ====================================================================
    public final Path tableRoot;

    public final Path lockPath;

    public final Path idClockPath;
    public final Path idSnapshotPath;
    public final Path idJournalPath;

    public final IDIndex index;

    public final IDClock idClock;
    public final IDAllocator idAllocator;
    public final IDStore idStore;

    // -=== Ctor ======================================================================
    public TableStore(Path tableRoot) throws IOException
    {
        this.tableRoot = tableRoot.toAbsolutePath().normalize();

        this.lockPath = this.tableRoot.resolve(".atom");
        this.idClockPath = this.tableRoot.resolve("ids.clock");
        this.idSnapshotPath = this.tableRoot.resolve("ids.snapshot");
        this.idJournalPath = this.tableRoot.resolve("ids.log");

        this.index = new IDIndex();
        this.idClock = new IDClock(this.idClockPath);
        this.idAllocator = new IDAllocator(this.idClock);
        this.idStore = new IDStore(this.idSnapshotPath, this.idJournalPath);
    }

    // -=== Startup ===================================================================
    public void startup() throws IOException
    {
        Files.createDirectories(this.tableRoot);

        try(var lck = FileLock.Blocking(this.lockPath))
        {
            this.idStore.startup(this.index);
        }
    }

    // -=== Lock ======================================================================
    public FileLock lockTable()
    {
        return FileLock.Blocking(this.lockPath);
    }

}
