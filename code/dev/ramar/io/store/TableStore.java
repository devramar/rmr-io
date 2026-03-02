package dev.ramar.io.store;

import dev.ramar.utils.FileLock;
import dev.ramar.utils.RFiles;

import java.io.IOException;
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
public class TableStore
{

    // -=== Fields ====================================================================
    public final Path tableRoot;

    public Path pathFor(long id, Object... subpaths)
    {
        return RFiles.Resolve(this.tableRoot, "data", id, subpaths);
    }

    public final Path lockPath;

    public final IDIndex index;

    public final IDAllocator idAllocator;
    public final IDStore idStore;

    // -=== Ctor ======================================================================
    public TableStore(Path tableRoot) throws IOException
    {
        this.tableRoot = tableRoot.toAbsolutePath().normalize();
        this.index = new IDIndex();


        this.lockPath = this.tableRoot.resolve(".atom");

        Path clockPath = this.tableRoot.resolve("ids.clock");
        IDClock clock = new IDClock(clockPath);
        this.idAllocator = new IDAllocator(clock);

        Path snapshotPath = this.tableRoot.resolve("ids.snapshot");
        Path journalPath = this.tableRoot.resolve("ids.log");
        this.idStore = new IDStore(snapshotPath, journalPath);
    }


    public TableStore(Path tableRoot, IDAllocator allocator) throws IOException
    {
        this.tableRoot = tableRoot.toAbsolutePath().normalize();
        this.index = new IDIndex();
        this.idAllocator = allocator;

        this.lockPath = this.tableRoot.resolve(".atom");


        Path snapshotPath = this.tableRoot.resolve("ids.snapshot");
        Path journalPath = this.tableRoot.resolve("ids.log");
        this.idStore = new IDStore(snapshotPath, journalPath);
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

    // -=== ID Management =============================================================

    public long generateID() throws IOException
    {
        try(var lck = this.lockTable())
        {
            /// avoid collisions by looping until we get a new one.
            /// since this is time based, we're effectively guaranteed eventually.
            /// *touches wood*
            long id;
            do
            {
                id = this.idAllocator.newID();
            }
            while(this.index.contains(id));

            /// we have an id to use
            this.index.add(id);
            this.idStore.appendAdd(id);

            if( this.idStore.shouldCompact() )
                this.idStore.compactFromIndex(this.index);

            return id;
        }
    }

    public void delete(long id) throws IOException
    {
        try(var lck = this.lockTable())
        {
            this.index.remove(id);
            this.idStore.appendRemove(id);

            if( this.idStore.shouldCompact() )
                this.idStore.compactFromIndex(this.index);
        }
    }

    public boolean contains(long id)
    {
        return this.index.contains(id);
    }

    // -=== Lock ======================================================================
    public FileLock lockTable()
    {
        return FileLock.Blocking(this.lockPath);
    }

    // -=== Data ======================================================================

    public Path dataPath()
    { return RFiles.Resolve(this.tableRoot, "data"); }
}
