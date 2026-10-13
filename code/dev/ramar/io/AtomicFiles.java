package dev.ramar.io;

import java.io.IOException;
import java.io.OutputStream;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.nio.file.Path;

import java.util.Objects;

public final class AtomicFiles
{
    private AtomicFiles()
    {
    }

    /* Bytes
    --===------
    */

    public static void WriteTo(Path targetPath, byte[] bytes) throws IOException
    {
        Objects.requireNonNull(targetPath);
        Objects.requireNonNull(bytes);

        try (AtomicOutputStream out = AtomicOutputStream.Open(targetPath))
        {
            out.write(bytes);
        }
    }

    public static void WriteToUnique(Path targetPath, byte[] bytes) throws IOException
    {
        Objects.requireNonNull(targetPath);
        Objects.requireNonNull(bytes);

        try (AtomicOutputStream out = AtomicOutputStream.OpenUnique(targetPath))
        {
            out.write(bytes);
        }
    }

    /* String
    --===-------
    */

    public static void WriteTo(Path targetPath, String text) throws IOException
    {
        WriteTo(targetPath, text, StandardCharsets.UTF_8);
    }

    public static void WriteTo(Path targetPath, String text, Charset charset) throws IOException
    {
        Objects.requireNonNull(text);
        Objects.requireNonNull(charset);

        WriteTo(targetPath, text.getBytes(charset));
    }

    public static void WriteToUnique(Path targetPath, String text) throws IOException
    {
        WriteToUnique(targetPath, text, StandardCharsets.UTF_8);
    }

    public static void WriteToUnique(Path targetPath, String text, Charset charset) throws IOException
    {
        Objects.requireNonNull(text);
        Objects.requireNonNull(charset);

        WriteToUnique(targetPath, text.getBytes(charset));
    }

    /* Stream callback
    --===----------------
    */

    @FunctionalInterface
    public interface IOConsumer<T>
    {
        void accept(T value) throws IOException;
    }

    public static void WriteTo(Path targetPath, IOConsumer<OutputStream> writer) throws IOException
    {
        Objects.requireNonNull(targetPath);
        Objects.requireNonNull(writer);

        try (AtomicOutputStream out = AtomicOutputStream.Open(targetPath))
        {
            writer.accept(out);
        }
    }

    public static void WriteToUnique(Path targetPath, IOConsumer<OutputStream> writer) throws IOException
    {
        Objects.requireNonNull(targetPath);
        Objects.requireNonNull(writer);

        try (AtomicOutputStream out = AtomicOutputStream.OpenUnique(targetPath))
        {
            writer.accept(out);
        }
    }
}
