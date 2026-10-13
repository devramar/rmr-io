# Atomic Writing Basic Usage

The atomic writing system is provided under `dev.ramar.io`.

Its main classes are:

* `AtomicFiles` — convenience methods for atomic file writes.
* `AtomicOutputStream` — atomic `OutputStream`.
* `AtomicWriter` — writer wrapper used for atomic text output.

The system writes data to a temporary file first, then replaces the target file when the write successfully completes. This prevents a partially-written target file from being left behind when writing fails.

## Basic Writes

For most uses, prefer `AtomicFiles`.

Write bytes:

```java
AtomicFiles.WriteTo(path, bytes);
```

Write text:

```java
AtomicFiles.WriteTo(path, "hello world");
```

Strings use UTF-8 by default, or a charset can be supplied:

```java
AtomicFiles.WriteTo(path, text, charset);
```

The target's parent directories are created automatically when the underlying atomic stream is opened.

## Stream-Based Writing

For data that should be written incrementally, use the callback form:

```java
AtomicFiles.WriteTo(path, out ->
{
    out.write(header);
    out.write(data);
});
```

The supplied value is a normal `OutputStream`.

When the callback and stream close successfully, the temporary file replaces the target. If writing fails before completion, the target is not replaced.

## AtomicOutputStream

Use `AtomicOutputStream` directly when normal `OutputStream` APIs are required:

```java
try (AtomicOutputStream out = AtomicOutputStream.Open(path))
{
    out.write(data);
}
```

Data is written to a sibling temporary file.

Closing the stream:

1. flushes the data,
2. forces it to disk,
3. closes the temporary file,
4. moves it over the target.

The move attempts `ATOMIC_MOVE` first and falls back to a normal replacing move when the filesystem does not support atomic moves.

## Aborting

A write can explicitly be abandoned:

```java
AtomicOutputStream out = AtomicOutputStream.Open(path);

try
{
    writeData(out);
}
catch(Exception ex)
{
    out.abort();
    throw ex;
}
```

`abort()` closes the stream and deletes its temporary file without replacing the target.

## Text Writers

For normal character-based writing:

```java
try (BufferedWriter writer = AtomicOutputStream.OpenWriter(path))
{
    writer.write("hello");
    writer.newLine();
    writer.write("world");
}
```

`OpenWriter(...)` uses UTF-8.

The returned writer is backed by `AtomicWriter`, which ensures closing the writer also completes the underlying atomic output operation.

## Unique Temporary Files

The normal methods use a temporary sibling named approximately:

```text
target.ext.tmp
```

When concurrent writers may target the same file, use the `Unique` variants:

```java
AtomicFiles.WriteToUnique(path, data);

AtomicOutputStream.OpenUnique(path);
AtomicOutputStream.OpenUniqueWriter(path);
```

These use a unique temporary filename for each operation:

```text
target.ext.tmp.<unique-id>
```

Use the standard variants for ordinary single-writer files and the unique variants when separate writes could occur concurrently.
