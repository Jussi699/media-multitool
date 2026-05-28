package model.imagePreprocessing;

import javafx.scene.control.Alert;
import model.logger.ErrorLogger;
import viewHelp.Alerts;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class ImagePreprocessing {

    public static Optional<BufferedImage> toNegative(File file) {
        try {
            BufferedImage image = ImageIO.read(file);

            if (image == null) {
                ErrorLogger.error("Unsupported image format or file is not an image: " + file.getName());
                return Optional.empty();
            }

            int width = image.getWidth();
            int height = image.getHeight();

            BufferedImage negative = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgba = image.getRGB(x, y);

                    int alpha = (rgba >> 24) & 0xff;
                    int r = 255 - ((rgba >> 16) & 0xff);
                    int g = 255 - ((rgba >> 8) & 0xff);
                    int b = 255 - (rgba & 0xff);

                    int invertedRgba = (alpha << 24) | (r << 16) | (g << 8) | b;
                    negative.setRGB(x, y, invertedRgba);
                }
            }

            return Optional.of(negative);
        } catch (IOException e) {
            ErrorLogger.error("There was an error converting the image to negative: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Determines the direction of movement or placement.
     * <p>
     * A value of {@code true} means moving to the right,
     * and {@code false} — to left.
     */
    public static Optional<BufferedImage> turnImage(BufferedImage image, boolean side) {
        if (image == null) {
            return Optional.empty();
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // For 90 degree rotation, width and height are swapped
        BufferedImage outputImage = new BufferedImage(height, width,
                image.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : image.getType());
        
        java.awt.Graphics2D g2d = outputImage.createGraphics();
        
        if (side) { // Right (90 degrees clockwise)
            g2d.translate(height, 0);
            g2d.rotate(Math.toRadians(90));
        } else { // Left (90 degrees counter-clockwise)
            g2d.translate(0, width);
            g2d.rotate(Math.toRadians(-90));
        }
        
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        return Optional.of(outputImage);
    }

    public static void downloadImage(BufferedImage image, String formatFile, File output) throws IOException {
        if (image == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Error delivered", "Error delivered", "Error, image not delivered to download!");
            ErrorLogger.info("Unfortunately, current image = null!");
            return;
        }

        if (output == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Error delivered", "Error delivered", "Error, output path not delivered to download!");
            ErrorLogger.error("Output file is null!");
            return;
        }

        if (formatFile.equalsIgnoreCase("jpg") || formatFile.equalsIgnoreCase("jpeg")) {
            BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2d = rgbImage.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
            image = rgbImage;
        }

        ErrorLogger.info("Attempting to save image. Format: " + formatFile + ", Path: " + output.getAbsolutePath());

        if (ImageIO.write(image, formatFile, output)) {
            ErrorLogger.info("ImageIO.write returned true. File size: " + output.length() + " bytes");
        } else {
            ErrorLogger.error("ImageIO.write returned false! This usually means no appropriate writer was found for format: " + formatFile);
            throw new IOException("Failed to save image: no writer found for format " + formatFile);
        }
    }
}
