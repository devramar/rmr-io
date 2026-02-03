package dev.ramar.io;

import java.nio.file.Files;

import dev.ramar.utils.Lock;

import java.io.IOException;
import java.nio.file.Path;

public abstract class FileTable<T>
{
    public final Lock lock;

    private final String fileName;
    private final Path tableRoot;

    public FileTable(Path tableRoot, String fileName)
    {
        this(new Lock(), tableRoot, fileName);
    }

    public FileTable(Lock lock, Path tableRoot, String fileName)
    {
        this.fileName = fileName;
        this.tableRoot = tableRoot;
        this.lock = lock;
    }

    protected Path rootFor(long id)
    {
        return this.tableRoot.resolve(Long.toString(id));
    }

    protected Path fileFor(long id)
    {
        return this.rootFor(id).resolve(fileName);
    }

    protected Path pathFor(long id, Object... subpaths)
    {
        return Resolve(this.rootFor(id), subpaths);
    }


    /* FileTable I/O
    --===---------------
    */

    public abstract T load(long id);
    public abstract void save(long id, T item) throws IOException;


    public boolean trySave(long id, T item)
    {
        try
        {
            this.save(id, item);
            return true;
        }
        catch(IOException ex)
        {
            return false;
        }
    }


    public boolean exists(long id)
    {
        return Files.exists(this.fileFor(id));
    }

    /* Static Functions
    --===-----------------
    */

    public static Path Resolve(Path root, Object... subpaths)
    {
        Path resolved = root;

        for(Object subpath : subpaths)
            resolved = resolved.resolve(subpath.toString());

        Path normalizedRoot = root.normalize();
        Path normalizedResolved = resolved.normalize();

        if(!normalizedResolved.startsWith(normalizedRoot))
            throw new IllegalArgumentException(
                "Resolved path escapes root. (Root: " + normalizedRoot + ", Resolved: " + normalizedResolved + ")"
            );

        return normalizedResolved;
    }
}
