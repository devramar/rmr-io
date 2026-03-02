package dev.ramar.io.store;

import dev.ramar.io.AtomicOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/// GPT-Made
public class IDClock
{

    // -=== Fields ====================================================================
    public final Path clockPath;

    private volatile long lastTimestampMillis;

    // -=== Ctor ======================================================================
    public IDClock(Path clockPath) throws IOException
    {
        this.clockPath = clockPath;

        this.lastTimestampMillis = this.tryLoadLastTimestampMillis(clockPath);
    }

    // -=== Public ====================================================================
    public long nowClampedMillis() throws IOException
    {
        long now = System.currentTimeMillis();

        long prev = this.lastTimestampMillis;

        long ts = now;
        if(ts < prev)
            ts = prev;

        if(ts > prev)
        {
            this.lastTimestampMillis = ts;

            this.persistLastTimestampMillis(ts);
        }

        return ts;
    }

    // -=== Persistence ===============================================================
    private long tryLoadLastTimestampMillis(Path path) throws IOException
    {
        if(!Files.exists(path))
            return 0;

        byte[] buf = Files.readAllBytes(path);
        if(buf.length < 8)
            return 0;

        return ByteBuffer.wrap(buf)
            .order(ByteOrder.BIG_ENDIAN)
            .getLong()
        ;
    }

    private void persistLastTimestampMillis(long ts) throws IOException
    {
        Files.createDirectories(this.clockPath.getParent());

        try(var aos = AtomicOutputStream.Open(this.clockPath))
        {
            ByteBuffer bb = ByteBuffer.allocate(8)
                .order(ByteOrder.BIG_ENDIAN)
            ;

            bb.putLong(ts);

            aos.write(bb.array());
        }
    }

}
