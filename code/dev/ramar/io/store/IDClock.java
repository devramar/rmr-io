
/// -=== IDClock ======================================================================
/// Monotonic (clamped) wall-clock millis provider.
/// Persists lastTimestampMillis to disk (implementation deferred).
public class IDClock
{


    final Path clockPath;


    volatile long lastTimestampMillis;


    public IDClock(Path clockPath)
    {
        this.clockPath = clockPath;
        this.lastTimestampMillis = 0;
    }


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

            /// TODO:
            /// persist ts to clockPath using AtomicOutputStream (binary long)
        }

        return ts;
    }


}