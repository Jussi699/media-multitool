package model.utility;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Preparation {
    public static List<File> getFilesFromFolder(File path, List<String> filter) {

        File[] matches = path.listFiles((_, name) -> {
           String lowerName = name.toLowerCase();
           for(String ext : filter) {
               String suffix = ext.startsWith(".") ? ext.toLowerCase() : "." + ext.toLowerCase();
               if(lowerName.endsWith(suffix)) return true;
           }
           return false;
        });

        if(matches != null){
            return new ArrayList<>(Arrays.asList(matches));
        }

        return new ArrayList<>();
    }
}
