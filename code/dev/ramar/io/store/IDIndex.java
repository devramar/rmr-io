package dev.ramar.io.store;

import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.stream.LongStream;

/// GPT-Made
public class IDIndex
{

    // -=== Fields ====================================================================
    private final TreeSet<Long> ids = new TreeSet<>();

    // -=== Mutation (Table lock only) ================================================
    public void add(long id)
    {
        this.ids.add(id);
    }

    public void remove(long id)
    {
        this.ids.remove(id);
    }

    public boolean contains(long id)
    {
        return this.ids.contains(id);
    }

    public int size()
    {
        return this.ids.size();
    }

    // -=== Snapshots =================================================================
    public long[] snapshotNatural()
    {
        long[] arr = new long[this.ids.size()];
        int i = 0;

        for(long id : this.ids)
        {
            arr[i] = id;
            i++;
        }

        return arr;
    }

    public long[] snapshot(Comparator<Long> comparator)
    {
        long[] arr = this.snapshotNatural();

        Long[] boxed = new Long[arr.length];

        for(int i = 0; i < arr.length; i++)
            boxed[i] = arr[i];

        Arrays.sort(boxed, comparator);

        for(int i = 0; i < arr.length; i++)
            arr[i] = boxed[i];

        return arr;
    }

    public LongStream snapshot()
    {
        return this.ids.stream()
            .mapToLong(x -> x)
            ;
    }

}
