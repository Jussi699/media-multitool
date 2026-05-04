package model.utility;

import model.logger.ErrorLogger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Locale;

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
                    return reader.getFormatName().toLowerCase(Locale.ROOT);
                } finally {
                    reader.dispose();
                }
            }

            throw new IOException("Unknown image format");
        }
    }

    public static String determineFormat(File file) {
        if (file == null) {
            return null;
        }

        try {
            String type = Files.probeContentType(file.toPath());
            if (type != null && type.contains("/")) {
                String format = type.split("/")[1].toLowerCase();
                if ("svg+xml".equals(format)) {
                    return "svg";
                }
                if ("x-icon".equals(format) || "vnd.microsoft.icon".equals(format)) {
                    return "ico";
                }
                return format;
            }
        } catch (IOException e) {
            ErrorLogger.warn("Unable to determine file type!");
            ErrorLogger.log(113, ErrorLogger.Level.WARN, "IOException", e);
        }

        try {
            return determineFormatImage(file).toLowerCase();
        } catch (IOException e) {
            ErrorLogger.warn("Unable to determine image format via ImageIO, fallback to extension.");
            ErrorLogger.log(114, ErrorLogger.Level.WARN, "Image format detection fallback", e);
            return getExtensionByString(file.getName());
        }
    }

    public static String getExtensionByString(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT);
    }
}
