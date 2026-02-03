package test.ramar.io;

import dev.ramar.io.json.ArrayFileTableEntry;
import dev.ramar.io.json.ObjectFileTableEntry;
import dev.ramar.json.JToken;
import dev.ramar.json.JObject;
import dev.ramar.json.JArray;

import dev.ramar.utils.Lock;

import java.util.Random;

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

        JObject mdl = model.load(123123);
        JArray img = images.load(123123);

        System.out.println("mdl = " + mdl);
        System.out.println("img = " + img);

        Random rd = new Random();

        img.add(new JObject()
            .with("url", "TEMPTEMPTEMP " + rd.nextInt())
        );

        mdl.with("" + rd.nextInt(), "test");

        System.out.println("img.size() = " + img.size());

        images.trySave(123123, img);
        model.trySave(123123, mdl);
    }
}