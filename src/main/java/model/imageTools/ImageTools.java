package model.imageTools;

import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.paint.Color;
import model.enums.RotateSide;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Optional;
import java.util.function.DoubleConsumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.imageTools.ImageToolsHelper.*;

public class ImageTools {
    private ImageTools() {
        /* This utility class should not be instantiated */
    }

    /**
     * Converts the specified image into its photographic negative (inverts RGB colors).
     *
     * @param image the input {@link BufferedImage} to be inverted
     * @return an {@link Optional} containing the inverted {@link BufferedImage}, or {@link Optional#empty()} if the input image is null
     */
    public static Optional<BufferedImage> toNegative(BufferedImage image) {
        if (isNull(image)) {
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

        Graphics2D g2d = outputImage.createGraphics();

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


            // Flip horizontally (mirror left-to-right)
            case RotateSide.HORIZONTALLY -> {
                g2d.translate(width, 0);
                g2d.scale(-1, 1);
            }

            // Flip vertically (mirror upside-down)
            case RotateSide.VERTICALLY -> {
                g2d.translate(0, height);
                g2d.scale(1, -1);
            }
        }

        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        return Optional.of(outputImage);
    }

    /**
     * Adjusts the brightness of the specified image by applying an offset to RGB color channels.
     *
     * @param image  the input {@link BufferedImage} to be adjusted
     * @param offset the brightness offset value to add to each channel (-255 to 255)
     * @return an {@link Optional} containing the brightness-adjusted {@link BufferedImage}, or {@link Optional#empty()} if the input image is null
     */
    public static Optional<BufferedImage> brightnessImage(BufferedImage image, int offset) {
        if (isNull(image)) {
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

    /**
     * Creates a JavaFX {@link Lighting} effect configured with a distant light of the specified color.
     *
     * @param image the input {@link BufferedImage} to check for null
     * @param color the target tint {@link Color} for the distant light source
     * @return a configured {@link Lighting} effect, or {@code null} if the input image is null
     */
    public static Lighting colorizeImage(BufferedImage image, Color color) {
        if (isNull(image)) {
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

    /**
     * Applies a color tint/colorize lighting effect to the specified image using diffuse and specular calculations.
     *
     * @param image the input {@link BufferedImage} to be colorized
     * @param color the target tint {@link javafx.scene.paint.Color}
     * @return the colorized {@link BufferedImage}, or null if the input image is null
     */
    public static BufferedImage applyColorizeEffect(BufferedImage image, javafx.scene.paint.Color color) {
        if (isNull(image)) return null;

        if (color.equals(javafx.scene.paint.Color.WHITE)) {
            return image;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        double targetR = color.getRed();
        double targetG = color.getGreen();
        double targetB = color.getBlue();

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

    /**
     * Converts the specified image to grayscale (black and white) using standard luminance weights.
     *
     * @param image the input {@link BufferedImage} to be converted
     * @return an {@link Optional} containing the grayscale {@link BufferedImage}, or {@link Optional#empty()} if the input image is null
     */
    public static Optional<BufferedImage> blackAndWhiteImage(BufferedImage image) {
        if (isNull(image)) {
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
     * Applies a blur effect to the specified image using a fast 3-pass box blur (approximating Gaussian blur)
     * based on integral images (Summed-Area Tables).
     *
     * @param image            the input {@link BufferedImage} to be blurred
     * @param radius           the blur radius in pixels
     * @param progressConsumer an optional callback consumer to report blur progress (from 0.0 to 1.0), or null
     * @return an {@link Optional} containing the blurred {@link BufferedImage}, or {@link Optional#empty()} if the input image is null or interrupted
     */
    public static Optional<BufferedImage> blurryImage(BufferedImage image, int radius, DoubleConsumer progressConsumer) {
        if (isNull(image)) {
            return Optional.empty();
        }

        if (radius <= 0) {
            return Optional.of(copyImage(image));
        }

        int width  = image.getWidth();
        int height = image.getHeight();
        BufferedImage src = normalizeToArgb(image);
        int[] pixels = getPixels(src);

        // Three box-blur passes (SAT-based, O(W*H) each) → Gaussian approximation
        for (int pass = 0; pass < 3; pass++) {
            if (Thread.interrupted()) return Optional.empty();
            if (nonNull(progressConsumer)) progressConsumer.accept((double) pass / 3.0);

            applyBoxBlurPass(pixels, width, height, radius);
        }

        if (nonNull(progressConsumer)) progressConsumer.accept(1.0);

        return Optional.of(src);
    }
}
