package dev.ramar.io.json;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import dev.ramar.utils.Lock;
import dev.ramar.utils.TimeSpan;
    
import dev.ramar.json.JArray;
import dev.ramar.json.JITJArray;

import dev.ramar.io.FileTable;

public class ArrayFileTableEntry extends FileTable<JArray>
{

    public ArrayFileTableEntry(Path tableRoot, String fileName)
    {
        super(tableRoot, fileName);
    }

    public ArrayFileTableEntry(Lock lock, Path tableRoot, String fileName)
    {
        super(lock, tableRoot, fileName);
    }



    @Override
    public JArray load(long id)
    {
        try(var lck = this.lock.use())
        {
            return JITJArray.FromPath(this.fileFor(id));
        }
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
