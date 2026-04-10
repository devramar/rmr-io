package dev.ramar.io.entry.json;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import dev.ramar.utils.Lock;
import dev.ramar.utils.TimeSpan;
    
import dev.ramar.json.JArray;
import dev.ramar.json.JITJArray;

import dev.ramar.io.entry.FileTableEntry;

public class ArrayFileTableEntry extends FileTableEntry<JArray>
{

    public ArrayFileTableEntry(Path tableRoot, String name)
    {
        super(tableRoot, name);
    }

    public ArrayFileTableEntry(Lock lock, Path tableRoot, String name)
    {
        super(lock, tableRoot, name);
    }



    @Override
    public JArray load(long id)
    {
        try(var lck = this.lock.use())
        {
            return JITJArray.FromPath(this.fileFor(id));
        }
    }


    public void delete(long id) throws IOException
    {
        this.save(id, null);
    }
    

    @Override
    public void save(long id, JArray item) throws IOException
    {
        try(var lck = this.lock.use())
        {
            Path itemPath = this.fileFor(id);
            if( item == null || item.isEmpty() )
            {
                Files.deleteIfExists(itemPath);
                return;
            }

            item.writeTo(itemPath);
        }
        catch(IOException ex)
        {
            throw new IOException("failed to save " + id + ": " + ex.getMessage(), ex);
        }
    }

    public boolean exists(long id)
    {
        return Files.exists(this.fileFor(id));
    }

}
