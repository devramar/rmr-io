package dev.ramar.io.store.snapshotting;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.stream.LongStream;

/// GPT-Made
public final class V1Snapshot
{

    // -=== Read ======================================================================
    public static long Read(DataInputStream dis, Snapshot.SnapshotReader reader) throws IOException
    {
        long count;

        try
        { count = dis.readLong(); }
        catch(EOFException ex)
        { return 0; }

        long read = 0;

        for(long i = 0; i < count; i++)
        {
            long id;

            try
            { id = dis.readLong(); }
            catch(EOFException ex)
            { break; }

            reader.onId(id);
            read++;
        }

        return read;
    }

    // -=== Write =====================================================================
    public static void Write(DataOutputStream dos, LongStream ids) throws IOException
    {
        long[] arr = ids.toArray();

        dos.writeLong(arr.length);

        for(long id : arr)
            dos.writeLong(id);
    }

}
