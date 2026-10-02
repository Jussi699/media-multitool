package model.helper;

import lombok.Getter;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileHelper {
    @Getter private final File logFile;

    public FileHelper(File logFile) {
        this.logFile = logFile;
    }

    public ArrayList<String> getStringsFromFile() throws IOException {
        if (!logFile.exists()) {
            return new ArrayList<>();
        }

        try (Stream<String> lines = Files.lines(logFile.toPath())) {
            return  lines
                    .filter(line -> !line.isBlank())
                    .collect(Collectors.toCollection(ArrayList::new));


        }
    }
}
