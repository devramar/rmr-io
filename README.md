# `dev.ramar.io`

`dev.ramar.io` is a small file I/O support package built around one main idea:

> write files safely, keep table-like file stores simple, and avoid half-written data wherever possible.

The package is mainly split into two areas:

* **Atomic file controls** — helpers for writing files through temp files before replacing the real file.
* **TableStore** — a lightweight file-backed ID/table system for storing records under generated long IDs.

This package is not trying to be a database. It is closer to a “structured filesystem store” with some safety rails.

---

## Atomic file writing

The atomic controls are the core general-purpose part of this package.

The basic problem they solve is this:

> If the program crashes halfway through writing a file, the target file should not be left half-written.

Instead of writing directly to the real file, `AtomicOutputStream` writes to a temporary file first. When the stream closes successfully, the temp file is moved into place over the target file.

```java
AtomicFiles.WriteTo(path, "hello world");
```

or:

```java
AtomicFiles.WriteTo(path, out ->
{
    out.write(bytes);
});
```

There are also unique-temp variants:

```java
AtomicFiles.WriteToUnique(path, "hello world");
```

The normal variant uses a predictable temp file next to the target:

```text
file.json.tmp
```

The unique variant uses a random temp suffix:

```text
file.json.tmp.<uuid>
```

Use the unique variant when multiple writers might be aiming at the same target and you want to avoid temp-file collisions.

---

## `AtomicOutputStream`

`AtomicOutputStream` is the lower-level control behind `AtomicFiles`.

Typical use:

```java
try(var out = AtomicOutputStream.Open(path))
{
    out.write(bytes);
}
```

On close, it:

1. flushes the file
2. forces the file channel to disk
3. closes the underlying stream
4. moves the temp file over the target file

Where supported, it uses `ATOMIC_MOVE`. If the filesystem does not support atomic moves, it falls back to a normal replace move.

This means the intent is atomic replacement, but the final guarantee still depends on the filesystem.

You can also abort a write:

```java

AtomicFiles.WriteTo(path, out ->
{
    try
    {
        out.write(myClass.serialize())
    }
    catch(Exception ex)
    {
        out.abort();
    }
});
```

`abort()` closes the temp stream and deletes the temp file.
if you don't abort an AtomicOutputStream, it will attempt the move.
 - in general, on `.close()` the move attempt will be invoked.

---

## Atomic text writing

For simple UTF-8 text writing, use:

```java
try(var writer = AtomicOutputStream.OpenWriter(path))
{
    writer.write("hello");
}
```

There is also:

```java
AtomicOutputStream.OpenUniqueWriter(path);
```

This gives the same temp-file safety as `AtomicOutputStream`, but through a normal `Writer`.

---

## TableStore

`TableStore` is the higher-level file-backed storage helper.

It manages:
* a table root directory
* long IDs
* an in-memory ID index
* persistent ID tracking through snapshot + journal files
* a table-level file lock
* safe path resolution into the table’s `data` directory

A table roughly looks like this:

```text
table-root/
    .atom
    ids.clock
    ids.snapshot
    ids.log
    data/
        28291123456768/
            item.json
            metadata.json
        28291123456784/
            item.json
```

The main idea is:

```java
TableStore store = new TableStore(path);
store.startup();

long id = store.generateID();

Path itemPath = store.pathFor(id, "item.json");
AtomicFiles.WriteTo(itemPath, jsonText);
```

`TableStore` handles the ID lifecycle & organisation. You have ownership of what you do from there.

---

## Expected TableStore pattern

The intended usage is usually to make a small class for each table.

Example shape:

```java
public class UserStore extends TableStore
{
    public final ObjectFileTableEntry users;

    public final ObjectFileTableEntry tags;

    public UserStore(Path root) throws IOException
    {
        super(root);

        this.users = new ObjectFileTableEntry(this.dataPath(), "user.json");
        this.tags = FileTableEntry.Build(this.dataPath(), "tags.list", 
            // loader 
            (entry, id) ->
            {
                return Files.readAllLines(entry.fileFor(id));
            },

            // saver
            (entry, id, tags) ->
            {
                AtomicOutputStream.Open(entry.fileFor(id), out ->
                {
                    out.write(tags.toString());
                });
            }
        )
    }

    public long create(JObject user) throws IOException
    {
        long id = this.generateID();

        this.users.save(id, user);

        return id;
    }

    public JObject load(long id)
    {
        return this.users.load(id);
    }

    public void remove(long id) throws IOException
    {
        this.users.delete(id);
        this.delete(id);
    }
}
```

The `TableStore` owns the table-level ID tracking.

The `FileTableEntry` classes own the per-record files.

---

## IDs

IDs are generated by `IDAllocator`.

They are 64-bit long values made from:

* the current timestamp in milliseconds
* a small 4-bit sequence value

This makes generated IDs naturally time-sortable.

The allocator can generate up to 16 IDs per millisecond before waiting for the next millisecond.

`IDClock` persists the last timestamp to disk using `ids.clock`, which helps prevent clock-backwards issues from producing older IDs after restart.

---

## ID persistence

`TableStore` does not rebuild the ID list by scanning the `data` folder.

Instead, it tracks IDs through:

```text
ids.snapshot
ids.log
```

On startup:

1. the snapshot is loaded
2. the journal is replayed
3. the in-memory `IDIndex` is rebuilt

When IDs are added or removed, the journal is appended.

After enough journal entries, the store compacts back into a new snapshot and resets the journal.

This keeps startup cheap without needing to walk every record folder.

---

## Locking

`TableStore` uses a table-level lock file:

```text
.atom
```

ID mutations go through this lock.

That includes:

```java
store.generateID();
store.add(id);
store.delete(id);
```

For custom write operations that need to be coordinated with the table, use:

```java
try(var lck = store.lockTable())
{
    // write files / mutate table state here
}
```

Reads do not necessarily need the table lock, depending on the data being read. 
If you need singular access to the table, use this method.

You have two other ways to gain locked access:
1. `FileTableEntry.lock` is a public `dev.ramar.utils.Lock`

Writes should generally use:

* `store.lockTable()` for coordinated table-level changes
* `AtomicOutputStream` / `AtomicFiles` for safe file replacement

---

## FileTableEntry

`FileTableEntry<T>` is a small abstraction for a named file inside each record folder.

For an ID like:

```text
12345
```

and an entry named:

```text
profile
```

the file path is:

```text
data/12345/profile.json
```

The JSON-specific implementations are:

```java
ObjectFileTableEntry
ArrayFileTableEntry
```

These load and save through `dev.ramar.json`.

Example:

```java
ObjectFileTableEntry profile = new ObjectFileTableEntry(store.dataPath(), "profile");

JObject obj = profile.load(id);

profile.save(id, obj);

profile.delete(id);
```

Saving `null` or an empty object/array deletes the file.

---

## Aggregate object entries

`ObjectFileTableEntry` also supports a simple aggregate pattern, where a base object can be loaded alongside child object files.

For example, a record might be stored as:

```text
data/123/
    user.json
    settings.json
    stats.json
```

Then loaded into one combined object.

The helper methods are:

```java
ObjectFileTableEntry.AggregateLoad(id, base, children...);
ObjectFileTableEntry.AggregateSave(id, out, base, children...);
```

This is useful when a record is conceptually one thing, but you want to split pieces of it across multiple files.

---

## Safe path resolving

`TableStore.pathFor(...)` and `FileTableEntry.Resolve(...)` protect against path escape.

This means paths are normalized and checked to make sure they stay inside the intended root.

So this kind of thing should be rejected:

```java
store.pathFor(id, "..", "..", "outside.txt");
```

That matters because table paths are often built from IDs, filenames, or user-provided fragments.

---

## When to use this package

Use `dev.ramar.io` when you want:

* simple file-backed storage
* predictable folder structure
* generated sortable long IDs
* atomic file replacement
* JSON records split across files
* lightweight table locking
* readable filesystem state without a full database

Do not use it as a replacement for a real database when you need:

* complex queries
* transactions across many records
* indexes over arbitrary fields
* high write concurrency
* remote/multi-machine coordination
* relational constraints

This package is best for local structured storage where the filesystem is the database, and the goal is to make that approach safer and cleaner.
