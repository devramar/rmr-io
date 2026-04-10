package dev.ramar.io;

import java.io.IOException;
import java.io.Writer;

public final class AtomicWriter extends Writer
{
    private final AtomicOutputStream aos;
    private final Writer inner;

    private boolean isClosed;

    public AtomicWriter(AtomicOutputStream aos, Writer inner)
    {
        this.aos = aos;
        this.inner = inner;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException
    {
        this.ensureOpen();
        this.inner.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException
    {
        this.ensureOpen();
        this.inner.flush();
    }

    @Override
    public void close() throws IOException
    {
        if( this.isClosed )
            return;

        this.isClosed = true;

        IOException closeEx = null;

        try
        { this.inner.close(); }

        catch (IOException ex)
        { closeEx = ex; }

        try
        { this.aos.close(); }

        catch (IOException ex2)
        {
            if( closeEx == null )
                closeEx = ex2;
        }

        if( closeEx != null )
        {
            this.aos.abort();
            throw closeEx;
        }
    }

    void ensureOpen() throws IOException
    {
        if( this.isClosed )
            throw new IOException("AtomicWriter is closed.");
    }
}