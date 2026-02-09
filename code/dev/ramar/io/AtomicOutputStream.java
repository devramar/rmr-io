package dev.ramar.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.nio.channels.FileChannel;

import java.util.Objects;
import java.util.UUID;

public final class AtomicOutputStream extends OutputStream implements AutoCloseable
{
    /* Static Constructors
    --===--------------------
    */

    public static AtomicOutputStream Open(Path targetPath) throws IOException
    {
        Path tempPath = targetPath.resolveSibling(targetPath.getFileName().toString() + ".tmp");
        return new AtomicOutputStream(targetPath, tempPath);
    }

    public static AtomicOutputStream OpenUnique(Path targetPath) throws IOException
    {
        String fileName = targetPath.getFileName().toString();
        String suffix = ".tmp." + UUID.randomUUID().toString().replace("-", "");
        Path tempPath = targetPath.resolveSibling(fileName + suffix);
        return new AtomicOutputStream(targetPath, tempPath);
    }

    public static BufferedWriter OpenWriter(Path targetPath) throws IOException
    {
        AtomicOutputStream aos = AtomicOutputStream.Open(targetPath);

        try
        {
            return new BufferedWriter(new AtomicWriter(aos, new OutputStreamWriter(aos, StandardCharsets.UTF_8)));
        }
        catch (RuntimeException ex)
        {
            aos.abort();
            throw ex;
        }
    }

    public static BufferedWriter OpenUniqueWriter(Path targetPath) throws IOException
    {
        AtomicOutputStream aos = AtomicOutputStream.OpenUnique(targetPath);

        try
        {
            return new BufferedWriter(new AtomicWriter(aos, new OutputStreamWriter(aos, StandardCharsets.UTF_8)));
        }
        catch (RuntimeException ex)
        {
            aos.abort();
            throw ex;
        }
    }

    /* Object Declaration
    --===-------------------
    */


    private final Path targetPath;
    private final Path tempPath;

    private final FileOutputStream fos;
    private final FileChannel channel;

    private boolean isClosed;
    private boolean isAborted;
    private boolean isCommitted;

    private AtomicOutputStream(Path targetPath, Path tempPath) throws IOException
    {
        this.targetPath = Objects.requireNonNull(targetPath);
        this.tempPath = Objects.requireNonNull(tempPath);

        Path parent = this.targetPath.getParent();
        if( parent == null )
            throw new IOException("targetPath has no parent directory: " + this.targetPath);

        Files.createDirectories(parent);

        this.fos = new FileOutputStream(this.tempPath.toFile(), false);
        this.channel = this.fos.getChannel();
    }






    public Path targetPath()
    {
        return this.targetPath;
    }

    public Path tempPath()
    {
        return this.tempPath;
    }

    @Override
    public void write(int b) throws IOException
    {
        this.ensureWritable();

        this.fos.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException
    {
        this.ensureWritable();

        this.fos.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        this.ensureWritable();

        this.fos.write(b, off, len);
    }

    @Override
    public void flush() throws IOException
    {
        this.ensureWritable();

        this.fos.flush();
    }

    public void abort()
    {
        if( this.isClosed )
            return;

        this.isAborted = true;

        try
        { this.fos.close(); }

        catch (IOException ex)
        { }

        try
        { Files.deleteIfExists(this.tempPath); }

        catch (IOException ex2)
        { }

        this.isClosed = true;
    }

    @Override
    public void close() throws IOException
    {
        if( this.isClosed )
            return;

        this.isClosed = true;

        if( this.isAborted )
            return;

        IOException closeEx = null;

        try
        {
            this.fos.flush();
            this.channel.force(true);
        }

        catch (IOException ex)
        {
            closeEx = ex;
        }

        try
        { this.fos.close(); }

        catch (IOException ex2)
        {
            if( closeEx == null )
                closeEx = ex2;
        }

        if( closeEx != null )
        {
            this.tryDeleteTemp();

            throw closeEx;
        }

        this.commitMove();
        this.isCommitted = true;
    }

    private void commitMove() throws IOException
    {
        try
        {
            Files.move
            (
                this.tempPath,
                this.targetPath,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            );

            return;
        }

        catch (AtomicMoveNotSupportedException ex)
        {
            // Silent fallback (non-atomic). Caller opts into using AtomicOutputStream.
        }

        Files.move
        (
            this.tempPath,
            this.targetPath,
            StandardCopyOption.REPLACE_EXISTING
        );
    }

    private void tryDeleteTemp()
    {
        try
        { Files.deleteIfExists(this.tempPath); }

        catch (IOException ex)
        { }
    }

    private void ensureWritable() throws IOException
    {
        if( this.isClosed )
            throw new IOException("AtomicOutputStream is closed.");

        if( this.isAborted )
            throw new IOException("AtomicOutputStream was aborted.");

        if( this.isCommitted )
            throw new IOException("AtomicOutputStream already committed.");
    }
}
