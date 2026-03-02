package dev.ramar.io.entry;

import java.nio.file.Files;

import dev.ramar.utils.Lock;

import java.io.IOException;
import java.nio.file.Path;

public abstract class FileTableEntry<T>
{

    @FunctionalInterface
    public interface Loader<T>
    {
        public T load(FileTableEntry<T> entry, long id);
    }

    @FunctionalInterface
    public interface Saver<T>
    {
        public T save(FileTableEntry<T> entry, long id, T item);
    }

    /*
    FileTableEntry.Build(root, name, this::loadJSON, this::saveJSON)
    */
    public static <T> FileTableEntry<T> Build(Path tableRoot, String name, Loader<T> loader, Saver<T> saver)
    {
        return new FileTableEntry<T>(tableRoot, name)
        {
            @Override
            public T load(long id)
            {
                return loader.load(this, id);
            }

            @Override
            public void save(long id, T item) throws IOException
            {
                saver.save(this, id, item);
            }
        };
    }

    public final Lock lock;

    /// NOT including .json
    public final String name;
    public final Path tableRoot;

    public FileTableEntry(Path tableRoot, String name)
    {
        this(new Lock(), tableRoot, name);
    }

    public FileTableEntry(Lock lock, Path tableRoot, String name)
    {
        this.name = name;
        this.tableRoot = tableRoot;
        this.lock = lock;
    }

    protected Path rootFor(long id)
    {
        return this.tableRoot.resolve(Long.toString(id));
    }

    protected Path fileFor(long id)
    {
        return this.rootFor(id).resolve(name + ".json");
    }

    protected Path pathFor(long id, Object... subpaths)
    {
        return Resolve(this.rootFor(id), subpaths);
    }


    /* FileTableEntry I/O
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
