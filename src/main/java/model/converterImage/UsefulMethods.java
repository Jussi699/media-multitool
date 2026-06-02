package model.converterImage;

import net.ifok.image.image4j.codec.ico.ICODecoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class UsefulMethods {
    public static Optional<BufferedImage> readPreviewImage(File imageFile) throws IOException {
        if ("ico".equals(getFileExtension(imageFile))) {
            List<BufferedImage> images = ICODecoder.read(imageFile);
            if (images.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(getLargestImage(images));
        }
        return Optional.ofNullable(ImageIO.read(imageFile));
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

    public static String getFileExtension(File file) {
        if (file == null) {
            return "";
        }

        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    public static String normalizeFormat(String format) {
        if (format == null) {
            return "";
        }

        String normalizedFormat = format.toLowerCase();
        return switch (normalizedFormat) {
            case "jpg" -> "jpeg";
            case "svg+xml" -> "svg";
            case "tif" -> "tiff";
            case "x-icon", "vnd.microsoft.icon" -> "ico";
            default -> normalizedFormat;
        };

    }
}
