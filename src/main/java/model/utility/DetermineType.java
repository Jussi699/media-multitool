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
import java.util.Optional;

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

    public static Optional<String> determineFormat(File file) {
        if (file == null) {
            return Optional.empty();
        }

        try {
            String type = Files.probeContentType(file.toPath());
            if (type != null && type.contains("/")) {
                String format = type.split("/")[1].toLowerCase();
                return switch (format) {
                    case "svg+xml" -> Optional.of("svg");
                    case "x-icon", "vnd.microsoft.icon" -> Optional.of("ico");
                    case "quicktime" -> Optional.of("mov");
                    case "x-msvideo", "avi" -> Optional.of("avi");
                    case "webm" -> Optional.of("webm");
                    case "x-matroska", "matroska" -> Optional.of("mkv");
                    case "x-flv", "flv" -> Optional.of("flv");
                    case "x-ms-wmv", "wmv" -> Optional.of("wmv");
                    case "3gpp", "3gpp2" -> Optional.of("3gp");
                    default -> Optional.of(format);
                };
            }
        } catch (IOException e) {
            ErrorLogger.warn("Unable to determine file type!");
            ErrorLogger.log(113, ErrorLogger.Level.WARN, "IOException", e);
        }

        try {
            return Optional.of(determineFormatImage(file).toLowerCase());
        } catch (IOException e) {
            ErrorLogger.warn("Unable to determine image format via ImageIO, fallback to extension.");
            ErrorLogger.log(114, ErrorLogger.Level.WARN, "Image format detection fallback", e);
            return Optional.of(getExtensionByString(file.getName()));
        }
    }

    public static String getExtensionByString(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT);
    }
}
