
package dev.ramar.io.store;
import dev.ramar.io.store.IDClock;
import java.io.IOException;

/// -=== IDAllocator ==================================================================
/// Generates time-sortable 64-bit ids:
/// - high bits: timestampMillis
/// - low 4 bits: sequence (0..15)

/// GPT-Made
public class IDAllocator
{

    final IDClock clock;

    long lastTimestampMillis;

    int sequence; /// 0..15

    public IDAllocator(IDClock clock)
    {
        this.clock = clock;

        this.lastTimestampMillis = -1;
        this.sequence = 0;
    }

    public synchronized long newID() throws IOException
    {
        long ts = this.clock.nowClampedMillis();

        if(ts == this.lastTimestampMillis)
        {
            int next = this.sequence + 1;

            if(next > 15)
            {
                /// simple spin until next millisecond
                do
                { ts = this.clock.nowClampedMillis(); }
                while(ts == this.lastTimestampMillis);

                this.lastTimestampMillis = ts;
                this.sequence = 0;
            }
            else
            {
                this.sequence = next;
            }
        }
        else
        {
            this.lastTimestampMillis = ts;
            this.sequence = 0;
        }

        return (ts << 4) | (this.sequence & 0x0F);
    }

}
