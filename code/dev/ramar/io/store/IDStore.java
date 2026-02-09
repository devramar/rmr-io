/// -=== IDStore ======================================================================
/// Persists ids via:
/// - snapshot (binary) for fast startup
/// - append-only journal (binary) for cheap updates
///
/// Startup contract:
/// - loadSnapshot into IDIndex
/// - replayJournal into IDIndex
public class IDStore
{


    final Path snapshotPath;
    final Path journalPath;


    /// maintained during replay / append (implementation deferred)
    long journalEntries;


    public IDStore(Path snapshotPath, Path journalPath)
    {
        this.snapshotPath = snapshotPath;
        this.journalPath = journalPath;

        this.journalEntries = 0;
    }


    public void startup(IDIndex index) throws IOException
    {
        this.loadSnapshot(index);

        this.replayJournal(index);
    }


    public void appendAdd(long id) throws IOException
    {
        /// TODO:
        /// binary append:
        ///  byte 0x01
        ///  long id (big-endian)
        /// increment journalEntries
    }


    public void appendRemove(long id) throws IOException
    {
        /// TODO:
        /// binary append:
        ///  byte 0x02
        ///  long id (big-endian)
        /// increment journalEntries
    }


    public boolean shouldCompact()
    {
        return this.journalEntries > 250_000;
    }


    public void compactFromIndex(IDIndex index) throws IOException
    {
        /// TODO:
        /// 1) write new snapshot (sorted ids) using AtomicOutputStream
        /// 2) replace snapshot
        /// 3) reset journal (truncate / replace empty) and set journalEntries=0
    }


    void loadSnapshot(IDIndex index) throws IOException
    {
        /// TODO:
        /// if snapshotPath exists:
        ///  read binary ids
        ///  index.add(id)
    }


    void replayJournal(IDIndex index) throws IOException
    {
        /// TODO:
        /// if journalPath exists:
        ///  read entries in order
        ///  op 0x01 => index.add(id)
        ///  op 0x02 => index.remove(id)
        ///  stop cleanly on truncated tail
        ///  set journalEntries to number of valid entries read
    }


}