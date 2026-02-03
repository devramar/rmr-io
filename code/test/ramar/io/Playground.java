package test.ramar.io;

import dev.ramar.io.json.ArrayFileTableEntry;
import dev.ramar.io.json.ObjectFileTableEntry;
import dev.ramar.json.JToken;

import dev.ramar.utils.Lock;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Playground
{

    public static Path ROOT = Paths.get("../test_data");

    public static void main(String... args)
    {
        Lock folderLock = new Lock();
        ObjectFileTableEntry model = new ObjectFileTableEntry(folderLock, 
            ROOT.resolve("model"), "model.json"
        );

        ArrayFileTableEntry images = new ArrayFileTableEntry(folderLock, 
            ROOT.resolve("model"), "images.json"
        );

        JToken mdl = model.load(123123);
        JToken img = images.load(123123);

    }
}