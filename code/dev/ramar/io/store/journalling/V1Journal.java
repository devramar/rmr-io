package dev.ramar.io.store.journalling;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/// GPT-Made
public final class V1Journal
{

    // -=== Constants =================================================================
    public static final byte OP_ADD = 0x01;

    public static final byte OP_REMOVE = 0x02;

    // -=== Replay ====================================================================
    public static long Replay(DataInputStream dis, Journal.JournalReplay replay) throws IOException
    {
        long entries = 0;

        while(true)
        {
            byte op;
            long id;

            try
            { op = dis.readByte(); }
            catch(EOFException ex)
            { break; }

            try
            { id = dis.readLong(); }
            catch(EOFException ex)
            { break; }

            if(op == OP_ADD)
            {
                replay.onAdd(id);
                entries++;
                continue;
            }

            if(op == OP_REMOVE)
            {
                replay.onRemove(id);
                entries++;
                continue;
            }

            throw new IOException("journal v1: unknown op: " + op);
        }

        return entries;
    }

    // -=== Append ====================================================================
    public static void Append(Path journalPath, Journal.JournalEntry entry) throws IOException
    {
        try(var os = Files.newOutputStream(
            journalPath,
            StandardOpenOption.WRITE,
            StandardOpenOption.APPEND
        ))
        {
            DataOutputStream dos = new DataOutputStream(os);

            dos.writeByte(entry.op);
            dos.writeLong(entry.id);
        }
    }

}
