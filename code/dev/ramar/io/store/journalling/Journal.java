package dev.ramar.io.store.journalling;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/// GPT-Made
public final class Journal
{

    // -=== Constants =================================================================
    public static final int MAGIC = 0x4A524E4C; /// 'JRNL'

    // -=== Read/Replay ===============================================================
    public static long Replay(Path journalPath, JournalReplay replay) throws IOException
    {
        if(!Files.exists(journalPath))
            return 0;

        try(var is = Files.newInputStream(journalPath))
        {
            DataInputStream dis = new DataInputStream(is);

            int magic;
            try
            { magic = dis.readInt(); }
            catch(EOFException ex)
            { return 0; }

            if(magic != MAGIC)
                throw new IOException("journal: bad magic");

            int version;
            try
            { version = dis.readUnsignedByte(); }
            catch(EOFException ex)
            { return 0; }

            if(version == 1)
                return V1Journal.Replay(dis, replay);

            throw new IOException("journal: unsupported version: " + version);
        }
    }

    // -=== Append ====================================================================
    public static void Append(Path journalPath, int version, JournalEntry entry) throws IOException
    {
        if(version != 1)
            throw new IOException("journal: unsupported version: " + version);

        Files.createDirectories(journalPath.getParent());

        if(!Files.exists(journalPath))
        {
            try(var os = Files.newOutputStream(journalPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))
            {
                DataOutputStream dos = new DataOutputStream(os);

                dos.writeInt(MAGIC);
                dos.writeByte(1);
            }
        }

        V1Journal.Append(journalPath, entry);
    }

    // -=== Types =====================================================================
    public interface JournalReplay
    {
        void onAdd(long id);

        void onRemove(long id);
    }

    public static final class JournalEntry
    {

        public final byte op;
        public final long id;

        public JournalEntry(byte op, long id)
        {
            this.op = op;
            this.id = id;
        }

    }

}
