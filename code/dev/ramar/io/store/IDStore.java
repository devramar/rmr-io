package dev.ramar.io.store;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import dev.ramar.io.store.journalling.Journal;
import dev.ramar.io.store.journalling.V1Journal;

import dev.ramar.io.store.snapshotting.Snapshot;

/// GPT-Made
public class IDStore
{

    // -=== Fields ====================================================================
    private final Path snapshotPath;
    private final Path journalPath;

    private long journalEntries;

    // -=== Ctor ======================================================================
    public IDStore(Path snapshotPath, Path journalPath)
    {
        this.snapshotPath = snapshotPath;
        this.journalPath = journalPath;

        this.journalEntries = 0;
    }



    // -=== Startup ===================================================================
    public void startup(IDIndex index) throws IOException
    {
        this.loadSnapshot(index);

        this.replayJournal(index);
    }

    // -=== Append ====================================================================
    public void appendAdd(long id) throws IOException
    {
        Journal.Append(this.journalPath, 1, new Journal.JournalEntry(V1Journal.OP_ADD, id));
        this.journalEntries++;
    }

    public void appendRemove(long id) throws IOException
    {
        Journal.Append(this.journalPath, 1, new Journal.JournalEntry(V1Journal.OP_REMOVE, id));
        this.journalEntries++;
    }

    // -=== Compaction ================================================================
    public boolean shouldCompact()
    {
        return this.journalEntries > 250_000;
    }

    public void compactFromIndex(IDIndex index) throws IOException
    {
        Snapshot.Write(this.snapshotPath, 1, index.snapshot());

        this.resetJournal();
    }

    private void resetJournal() throws IOException
    {
        OpenOption[] options = Files.exists(this.journalPath)
            ? new OpenOption[] { StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE }
            : new OpenOption[] { StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE }
        ;
        
        try(var os = Files.newOutputStream(this.journalPath, options))
        {
            DataOutputStream dos = new DataOutputStream(os);

            dos.writeInt(Journal.MAGIC);
            dos.writeByte(1);
        }

        this.journalEntries = 0;
    }

    // -=== Snapshot ==================================================================
    private void loadSnapshot(IDIndex index) throws IOException
    {
        Snapshot.Read(this.snapshotPath, id ->
        {
            index.add(id);
        });
    }

    // -=== Journal ===================================================================
    private void replayJournal(IDIndex index) throws IOException
    {
        this.journalEntries = Journal.Replay(this.journalPath, new Journal.JournalReplay()
        {

            @Override
            public void onAdd(long id)
            {
                index.add(id);
            }

            @Override
            public void onRemove(long id)
            {
                index.remove(id);
            }

        });
    }

}
