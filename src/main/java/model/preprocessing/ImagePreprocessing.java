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
import java.util.Arrays;
import java.util.Optional;
import java.util.function.DoubleConsumer;

public class ImagePreprocessing {
    public enum RotateSide {
        RIGHT,
        LEFT,
        HORIZONTALLY,
        VERTICALLY
    }

    public static Optional<BufferedImage> toNegative(BufferedImage image) {
        if (image == null) {
            return Optional.empty();
        }

        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage negative = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = negative.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        int[] pixels = ((DataBufferInt) negative.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] ^= 0x00FFFFFF;
        }

        return Optional.of(negative);
    }

    /**
     * Rotates or flips the image based on the specified side.
     *
     * @param image the image to be processed
     * @param side the transformation type: "RIGHT", "LEFT", "HORIZONTALLY", or "VERTICALLY"
     * @return an Optional containing the processed BufferedImage, or empty if the input image is null
     */
    public static Optional<BufferedImage> rotateImage(BufferedImage image, RotateSide side) {
        if (image == null) {
            return Optional.empty();
        }

        int width = image.getWidth();
        int height = image.getHeight();

        int targetWidth  = (side == RotateSide.RIGHT || side == RotateSide.LEFT) ? height : width;
        int targetHeight = (side == RotateSide.RIGHT || side == RotateSide.LEFT) ? width : height;

        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight,
                image.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : image.getType());
        
        java.awt.Graphics2D g2d = outputImage.createGraphics();

        switch (side) {
            // Right (90 degrees clockwise)
            case RotateSide.RIGHT -> {
                g2d.translate(targetWidth, 0);
                g2d.rotate(Math.toRadians(90));
            }

            // Left (90 degrees counter-clockwise)
            case RotateSide.LEFT -> {
                g2d.translate(0, targetHeight);
                g2d.rotate(Math.toRadians(-90));
            }

            case RotateSide.HORIZONTALLY -> {
                g2d.translate(width, 0);
                g2d.scale(-1, 1);
            }

            case RotateSide.VERTICALLY -> {
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

        int[] lut = new int[256];
        for (int v = 0; v < 256; v++) {
            lut[v] = Math.clamp((long) v + offset, 0, 255);
        }

        int[] pixels = ((DataBufferInt) result.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            int a = argb & 0xFF000000;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;

            pixels[i] = a | (lut[r] << 16) | (lut[g] << 8) | lut[b];
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
        Graphics2D g2d = result.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        double targetR = fxColor.getRed();
        double targetG = fxColor.getGreen();
        double targetB = fxColor.getBlue();

        // JavaFX Lighting effect default values when surfaceScale is 0:
        // Diffuse = 1.0, Specular = 0.3
        // Result = LightColor * (Diffuse * PixelColor + Specular)
        double diffuseConstant = 1.0;
        double specularConstant = 0.3;

        int[] lutR = new int[256];
        int[] lutG = new int[256];
        int[] lutB = new int[256];
        for (int v = 0; v < 256; v++) {
            double norm = v / 255.0;
            lutR[v] = Math.min(255, (int) Math.round(targetR * (diffuseConstant * norm + specularConstant) * 255.0));
            lutG[v] = Math.min(255, (int) Math.round(targetG * (diffuseConstant * norm + specularConstant) * 255.0));
            lutB[v] = Math.min(255, (int) Math.round(targetB * (diffuseConstant * norm + specularConstant) * 255.0));
        }

        int[] pixels = ((DataBufferInt) result.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            int a = argb & 0xFF000000;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;

            pixels[i] = a | (lutR[r] << 16) | (lutG[g] << 8) | lutB[b];
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
        Graphics2D g2d = gray.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        int[] pixels = ((DataBufferInt) gray.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < pixels.length; i++) {
            int rgba = pixels[i];

            int a = rgba & 0xFF000000;
            int r = (rgba >> 16) & 0xFF;
            int g = (rgba >> 8) & 0xFF;
            int b = rgba & 0xFF;

            int grayValue = (299 * r + 587 * g + 114 * b) / 1000;

            pixels[i] = a | (grayValue << 16) | (grayValue << 8) | grayValue;
        }

        return Optional.of(gray);
    }

    /**
     * Applies a Gaussian-approximated blur using three consecutive box-blur passes via SAT.
     * By the Central Limit Theorem, three box blurs converge to a Gaussian with
     * sigma ≈ radius * sqrt(1/3), producing a soft photographic-quality blur comparable
     * to CSS filter:blur() or Photoshop Gaussian blur.
     * <p>
     * Complexity: O(W*H) — identical to a single SAT box blur pass, radius-independent.
     */
    public static Optional<BufferedImage> blurryImage(BufferedImage image, int radius, DoubleConsumer progressConsumer) {
        if (image == null) {
            return Optional.empty();
        }

        if (radius <= 0) {
            BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(),
                    image.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : image.getType());
            Graphics2D g2d = copy.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
            return Optional.of(copy);
        }

        int width  = image.getWidth();
        int height = image.getHeight();

        // Normalize to TYPE_INT_ARGB so DataBufferInt is always available
        BufferedImage src = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        {
            Graphics2D g = src.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
        }
        int[] pixels = ((DataBufferInt) src.getRaster().getDataBuffer()).getData();

        // Three box-blur passes (SAT-based, O(W*H) each) → Gaussian approximation
        int stride = width + 1;
        for (int pass = 0; pass < 3; pass++) {
            if (Thread.interrupted()) return Optional.empty();
            if (progressConsumer != null) progressConsumer.accept((double) pass / 3.0);

            // Build SAT for this pass
            long[] satA = new long[stride * (height + 1)];
            long[] satR = new long[stride * (height + 1)];
            long[] satG = new long[stride * (height + 1)];
            long[] satB = new long[stride * (height + 1)];

            for (int y = 0; y < height; y++) {
                long rowA = 0, rowR = 0, rowG = 0, rowB = 0;
                for (int x = 0; x < width; x++) {
                    int argb = pixels[y * width + x];
                    rowA += (argb >> 24) & 0xFF;
                    rowR += (argb >> 16) & 0xFF;
                    rowG += (argb >>  8) & 0xFF;
                    rowB +=  argb        & 0xFF;
                    int idx = (y + 1) * stride + (x + 1);
                    int above = y * stride + (x + 1);
                    satA[idx] = rowA + satA[above];
                    satR[idx] = rowR + satR[above];
                    satG[idx] = rowG + satG[above];
                    satB[idx] = rowB + satB[above];
                }
            }

            // Write blurred pixels back into the same array
            int[] tmp = Arrays.copyOf(pixels, pixels.length);
            for (int y = 0; y < height; y++) {
                int y1 = Math.max(0, y - radius);
                int y2 = Math.min(height, y + radius + 1);
                for (int x = 0; x < width; x++) {
                    int x1 = Math.max(0, x - radius);
                    int x2 = Math.min(width, x + radius + 1);
                    long area = (long)(x2 - x1) * (y2 - y1);
                    long sumA = satA[y2 * stride + x2] - satA[y1 * stride + x2] - satA[y2 * stride + x1] + satA[y1 * stride + x1];
                    long sumR = satR[y2 * stride + x2] - satR[y1 * stride + x2] - satR[y2 * stride + x1] + satR[y1 * stride + x1];
                    long sumG = satG[y2 * stride + x2] - satG[y1 * stride + x2] - satG[y2 * stride + x1] + satG[y1 * stride + x1];
                    long sumB = satB[y2 * stride + x2] - satB[y1 * stride + x2] - satB[y2 * stride + x1] + satB[y1 * stride + x1];
                    tmp[y * width + x] = ((int)(sumA / area) << 24) | ((int)(sumR / area) << 16) | ((int)(sumG / area) << 8) | (int)(sumB / area);
                }
            }
            System.arraycopy(tmp, 0, pixels, 0, pixels.length);
        }

        if (progressConsumer != null) progressConsumer.accept(1.0);

        return Optional.of(src);
    }
}
