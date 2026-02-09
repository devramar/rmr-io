package dev.ramar.io.json;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import dev.ramar.utils.Lock;

import dev.ramar.json.JToken;
import dev.ramar.json.JObject;
import dev.ramar.json.JITJObject;

import dev.ramar.io.FileTableEntry;

public class ObjectFileTableEntry extends FileTableEntry<JObject>
{

    public static JObject AggregateLoad(long id, ObjectFileTableEntry base, ObjectFileTableEntry... children)
    {
        JObject exp = base.load(id);
        if( exp == null )
            return null;

        for(ObjectFileTableEntry child : children)
            exp.with(child.name, child.load(id));

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
            JObject item = out.get(child.name).asObject();
            child.save(id, item);
        }
    }

    public ObjectFileTableEntry(Path tableRoot, String name)
    {
        super(tableRoot, name);
    }

    public ObjectFileTableEntry(Lock lock, Path tableRoot, String name)
    {
        super(lock, tableRoot, name);
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
