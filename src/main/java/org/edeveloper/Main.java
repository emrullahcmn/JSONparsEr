package org.edeveloper;

import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {

        JSONparsEr jsoNparsEr = new JSONparsEr(Paths.get("./src/jsontesting.txt"));  //relative path, or full path as String can be passed here
        jsoNparsEr.parse();  // parses json file
        System.out.println(jsoNparsEr.getRawResult());    // raw result (do not use it, just use for observing)

        var topMap = jsoNparsEr.getJsonMap();    // use getJsonMap, getJsonList, getJsonValue to handle data securely
        var parts = topMap.getMap("parts");   // JsonMap's getMap method returns a JsonMap for the given key (the structure the key mapped to must be JSON Object)
        var wheelsList = parts.getList("wheels");  // JsonMap's getList method returns a JsonList for the given key. (the structure the key mapped to must be JSON Array)
        var brandsList = parts.getList("brands"); // same as above

        for(int i = 0; i< wheelsList.getSize(); i++){   // getting list size using getSize()
            int wheelNumber = (int)wheelsList.getValue(i);   // JsonList's getValue method returns values such as string, int etc in the form of Object, casting is required.
            String brand = (String)brandsList.getValue(i);
            System.out.println("Wheel number:"+wheelNumber);
            System.out.println("Brand name: "+brand);
        }

        boolean canBeSupplied = (boolean)topMap.getValue("canBeSupplied");   // getting boolean
        System.out.println(canBeSupplied);

        Object notes = topMap.getValue("notes");  // get value for notes key
        System.out.println(notes);

        /*
         * Notes about integer handling, integers will be stored in long type if they cannot be stored in int data type.
         * But if they exceed the limit of long data type, they won't be stored in double data type.
         */
    }
}