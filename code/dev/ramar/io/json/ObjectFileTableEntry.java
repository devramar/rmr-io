package dev.ramar.io.json;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import dev.ramar.utils.Lock;

import dev.ramar.json.JObject;
import dev.ramar.json.JITJObject;

import dev.ramar.io.FileTable;

public class ObjectFileTableEntry extends FileTable<JObject>
{

    public ObjectFileTableEntry(Path tableRoot, String fileName)
    {
        super(tableRoot, fileName);
    }

    public ObjectFileTableEntry(Lock lock, Path tableRoot, String fileName)
    {
        super(lock, tableRoot, fileName);
    }

    public JObject load(long id)
    {
        try(var lck = this.lock.use())
        {
            return JITJObject.FromPath(this.fileFor(id));
        }
    }

    public void save(long id, JObject item) throws IOException
    {
        Path itemPath = this.fileFor(id);
        try(var lck = this.lock.use())
        {
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

}
