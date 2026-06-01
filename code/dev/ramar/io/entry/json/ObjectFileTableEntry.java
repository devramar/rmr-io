package dev.ramar.io.entry.json;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import dev.ramar.utils.Lock;

import dev.ramar.json.JToken;
import dev.ramar.json.JObject;
import dev.ramar.json.JITJObject;

import dev.ramar.io.entry.FileTableEntry;

/// represents a JObject FileTableEntry
public class ObjectFileTableEntry extends FileTableEntry<JObject>
{

    public static JObject AggregateLoad(long id, ObjectFileTableEntry base, ObjectFileTableEntry... children)
    {
        JObject exp = base.load(id);
        if( exp == null )
            return null;

        for(ObjectFileTableEntry child : children)
            exp.with(child.fileName, child.load(id));

        return exp;
    }

    public static void AggregateSave(long id, JObject out, ObjectFileTableEntry base, ObjectFileTableEntry... children) throws IOException
    {
        /// base owns the folder, if its file doesn't exist, we can safely delete the folder
        if( out == null )
        {
            Files.deleteIfExists(base.rootFor(id));
            return;
        }


        for(ObjectFileTableEntry child : children)
        {
            JObject item = out.get(child.fileName).asObject();
            child.save(id, item);
        }
    }

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

    public void delete(long id) throws IOException
    {
        this.save(id, null);
    }
    
    public void save(long id, JObject item) throws IOException
    {
        Path itemPath = this.fileFor(id);
        try(var lck = this.lock.use())
        {
            if( item == null || item.isEmpty() )
            {
                Files.deleteIfExists(itemPath);
                this.deleteIfEmpty(id);
                return;
            }
            item.writeTo(itemPath); 
        }
        catch(IOException ex)
        {
            throw new IOException("failed to save " + id + ": " + ex.getMessage(), ex);
        }
    }

    private void deleteIfEmpty(long id) throws IOException
    {
        try
        {
            Path root = this.rootFor(id);
            if( !Files.exists(root) )
                return;

            if( Files.list(root).findFirst().isEmpty() )
                Files.deleteIfExists(root);
        }
        catch(IOException ex)
        {
            throw new IOException("failed to delete empty root: " + ex.getMessage(), ex);
        }
    }
}
