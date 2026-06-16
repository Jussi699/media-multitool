package model.preprocessing;

import javafx.scene.control.Alert;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.paint.Color;
import model.logger.ErrorLogger;
import viewHelp.Alerts;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
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
     * Rotates or flips the image based on the specified side.
     *
     * @param image the image to be processed
     * @param side the transformation type: "turn_right", "turn_left", "flip_horizontally", or "flip_vertically"
     * @return an Optional containing the processed BufferedImage, or empty if the input image is null
     */
    public static Optional<BufferedImage> turnImage(BufferedImage image, String side) {
        if (image == null) {
            return Optional.empty();
        }

        int width = image.getWidth();
        int height = image.getHeight();

        int targetWidth = (side.equals("turn_right") || side.equals("turn_left")) ? height : width;
        int targetHeight = (side.equals("turn_right") || side.equals("turn_left")) ? width : height;

        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight,
                image.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : image.getType());
        
        java.awt.Graphics2D g2d = outputImage.createGraphics();

        switch (side) {
            // Right (90 degrees clockwise)
            case "turn_right" -> {
                g2d.translate(targetWidth, 0);
                g2d.rotate(Math.toRadians(90));
            }

            // Left (90 degrees counter-clockwise)
            case "turn_left" -> {
                g2d.translate(0, targetHeight);
                g2d.rotate(Math.toRadians(-90));
            }

            case "flip_horizontally" -> {
                g2d.translate(width, 0);
                g2d.scale(-1, 1);
            }

            case "flip_vertically" -> {
                g2d.translate(0, height);
                g2d.scale(1, -1);
            }
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

    public static Optional<BufferedImage> brightnessImage(BufferedImage image, int offset) {
        if (image == null) {
            return Optional.empty();
        }

        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        int[] pixels = ((DataBufferInt) result.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];

            int a = (argb >> 24) & 0xFF;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;

            r = Math.clamp(r + offset, 0, 255);
            g = Math.clamp(g + offset, 0, 255);
            b = Math.clamp(b + offset, 0, 255);

            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        return Optional.of(result);
    }

    public static Lighting colorizeImage(BufferedImage image, Color color) {
        if (image == null) {
            return null;
        }

        Light.Distant light = new Light.Distant();
        light.setAzimuth(45.0);
        light.setElevation(90.0);
        light.setColor(color);

        Lighting lighting = new Lighting();
        lighting.setLight(light);
        lighting.setSurfaceScale(0.0);

        return lighting;
    }

    public static BufferedImage applyColorizeEffect(BufferedImage image, Color fxColor) {
        if (image == null) return null;

        if (fxColor.equals(Color.WHITE)) {
            return image;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        double targetR = fxColor.getRed();
        double targetG = fxColor.getGreen();
        double targetB = fxColor.getBlue();

        // JavaFX Lighting effect default values when surfaceScale is 0:
        // Diffuse = 1.0, Specular = 0.3
        // Result = LightColor * (Diffuse * PixelColor + Specular)
        double diffuseConstant = 1.0;
        double specularConstant = 0.3;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                // Normalize pixel components to 0.0-1.0
                double normR = r / 255.0;
                double normG = g / 255.0;
                double normB = b / 255.0;

                double resR = targetR * (diffuseConstant * normR + specularConstant);
                double resG = targetG * (diffuseConstant * normG + specularConstant);
                double resB = targetB * (diffuseConstant * normB + specularConstant);

                int finalR = (int) Math.min(255, resR * 255);
                int finalG = (int) Math.min(255, resG * 255);
                int finalB = (int) Math.min(255, resB * 255);

                result.setRGB(x, y, (a << 24) | (finalR << 16) | (finalG << 8) | finalB);
            }
        }
        return result;
    }

    public static Optional<BufferedImage> blackAndWhiteImage(BufferedImage image) {
        if (image == null) {
            return Optional.empty();
        }

        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage gray = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgba = image.getRGB(x, y);

                int a = (rgba >> 24) & 0xFF;
                int r = (rgba >> 16) & 0xFF;
                int g = (rgba >> 8) & 0xFF;
                int b = rgba & 0xFF;

                int grayValue = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                int grayRgba = (a << 24) | (grayValue << 16) | (grayValue << 8) | grayValue;
                gray.setRGB(x, y, grayRgba);
            }
        }

        return Optional.of(gray);
    }

    public static Optional<BufferedImage> blurryImage(BufferedImage image, int radius) {
        if (image == null) {
            return Optional.empty();
        }

        if (radius <= 0) {
            BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), image.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : image.getType());
            Graphics2D g2d = copy.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
            return Optional.of(copy);
        }

        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage dest = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                long sumR = 0, sumG = 0, sumB = 0, sumA = 0;
                int count = 0;

                for (int ky = -radius; ky <= radius; ky++) {
                    for (int kx = -radius; kx <= radius; kx++) {
                        int pixelX = Math.clamp(x + kx, 0, width - 1);
                        int pixelY = Math.clamp(y + ky, 0, height - 1);
                        
                        int rgb = image.getRGB(pixelX, pixelY);

                        sumA += (rgb >> 24) & 0xFF;
                        sumR += (rgb >> 16) & 0xFF;
                        sumG += (rgb >> 8) & 0xFF;
                        sumB += rgb & 0xFF;
                        count++;
                    }
                }

                int avgA = (int) (sumA / count);
                int avgR = (int) (sumR / count);
                int avgG = (int) (sumG / count);
                int avgB = (int) (sumB / count);

                int blurredRGB = (avgA << 24) | (avgR << 16) | (avgG << 8) | avgB;
                dest.setRGB(x, y, blurredRGB);
            }
        }

        return Optional.of(dest);
    }
}
