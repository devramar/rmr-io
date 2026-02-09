package dev.ramar.io.store;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;

import dev.ramar.utils.FileLock;

/// -=== IDIndex ======================================================================
/// In-memory known-id snapshot.
/// Mutable only under TableStore lock (contract).
public class IDIndex
{


    TreeSet<Long> ids = new TreeSet<>();


    public void add(long id)
    { this.ids.add(id); }

    public void remove(long id)
    { this.ids.remove(id); }


    public boolean contains(long id)
    { return this.ids.contains(id); }


    public int size()
    { return this.ids.size(); }



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


}