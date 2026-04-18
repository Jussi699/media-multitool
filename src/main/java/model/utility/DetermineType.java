package model.utility;

import model.logger.ErrorLogger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;

public class DetermineType {
    public static String determineFormatImage(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File is null");
        }

        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            if (iis == null) {
                throw new IOException("Unable to create image input stream");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(iis, true, true);
                    return reader.getFormatName();
                } finally {
                    reader.dispose();
                }
            }

            throw new IOException("Unknown image format");
        }
    }

    public static String determineFormat(File file) {
        try {
            String type = Files.probeContentType(file.toPath());
            if (type != null && type.contains("/")) {
                return type.split("/")[1];
            }
        }
        catch (IOException e) {
            ErrorLogger.warn("Unable to determine file type!");
            ErrorLogger.log(113, ErrorLogger.Level.ERROR, "IOException", e);
            return null;
        }
        return getExtensionByString(file.getName());
    }

    private static String getExtensionByString(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
