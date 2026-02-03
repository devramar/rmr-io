package dev.ramar.io;

import java.nio.file.Files;

import dev.ramar.utils.Lock;

import java.nio.file.Path;

public class BaseFileTable
{
    public final Lock lock;

    private final String fileName;
    private final Path tableRoot;

    public BaseFileTable(Path tableRoot, String fileName)
    {
        this(new Lock(), tableRoot, fileName);
    }

    public BaseFileTable(Lock lock, Path tableRoot, String fileName)
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
