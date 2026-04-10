package dev.ramar.io.store.snapshotting;

import dev.ramar.io.AtomicOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.LongStream;

/// GPT-Made
public final class Snapshot
{

    // -=== Constants =================================================================
    public static final int MAGIC = 0x534E5053; /// 'SNPS'

    // -=== Read ======================================================================
    public static long Read(Path snapshotPath, SnapshotReader reader) throws IOException
    {
        if(!Files.exists(snapshotPath))
            return 0;

        try(var is = Files.newInputStream(snapshotPath))
        {
            DataInputStream dis = new DataInputStream(is);

            int magic;
            try
            { magic = dis.readInt(); }
            catch(EOFException ex)
            { return 0; }

            if(magic != MAGIC)
                throw new IOException("snapshot: bad magic");

            int version;
            try
            { version = dis.readUnsignedByte(); }
            catch(EOFException ex)
            { return 0; }

            if(version == 1)
                return V1Snapshot.Read(dis, reader);

            throw new IOException("snapshot: unsupported version: " + version);
        }
    }

    // -=== Write =====================================================================
    public static void Write(Path snapshotPath, int version, LongStream ids) throws IOException
    {
        if(version != 1)
            throw new IOException("snapshot: unsupported version: " + version);

        Files.createDirectories(snapshotPath.getParent());

        try(var aos = AtomicOutputStream.Open(snapshotPath))
        {
            DataOutputStream dos = new DataOutputStream(aos);

            dos.writeInt(MAGIC);
            dos.writeByte(1);

            V1Snapshot.Write(dos, ids);
        }
    }

    // -=== Types =====================================================================
    public interface SnapshotReader
    {
        void onId(long id);
    }

}
