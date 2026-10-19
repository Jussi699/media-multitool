package model.utility.io;

import lombok.Getter;
import net.ifok.image.image4j.codec.ico.ICODecoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static model.utility.io.FileUtils.getFileExtension;

public class FileHelper {
    @Getter private final File logFile;
    private final Path pathToLogFile;

    public FileHelper(File logFile) {
        this.logFile = logFile;
        pathToLogFile = logFile.toPath();
    }

    public ArrayList<String> getStringsFromFile() throws IOException {
        try (Stream<String> lines = Files.lines(logFile.toPath())) {
            return  lines
                    .filter(line -> !line.isBlank())
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    public void clearAllStringsFromFile() throws IOException {
        Files.writeString(pathToLogFile, "");
    }

    public static Optional<BufferedImage> readPreviewImage(File imageFile) throws IOException {
        return Optional.ofNullable(readImage(imageFile));
    }

    public static BufferedImage readImage(File imageFile) throws IOException {
        if ("ico".equals(getFileExtension(imageFile))) {
            List<BufferedImage> images = ICODecoder.read(imageFile);
            if (images.isEmpty()) {
                return null;
            }

            return getLargestImage(images);
        }
        return ImageIO.read(imageFile);
    }

    public static BufferedImage getLargestImage(List<BufferedImage> images) {
        BufferedImage largestImage = images.getFirst();

        for (BufferedImage imageCandidate : images) {
            if (imageCandidate.getWidth() * imageCandidate.getHeight()
                    > largestImage.getWidth() * largestImage.getHeight()) {
                largestImage = imageCandidate;
            }
        }

        return largestImage;
    }
}
