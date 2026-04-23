package model.converterImage;

import model.logger.ErrorLogger;
import net.ifok.image.image4j.codec.ico.ICOEncoder;
import net.ifok.image.image4j.codec.ico.ICODecoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.Base64;

public class ConverterImage {
    public static File convert(File image, File pathForSave, String typeFile) throws IOException {
        validateSourceImage(image);
        validateOutputDirectory(pathForSave);

        ImageIO.scanForPlugins();

        ErrorLogger.info("Starting conversion for image: " + image.getName() + " to " + typeFile);

        BufferedImage bufImage = readImage(image);
        String outputFormat = normalizeOutputFormat(typeFile);
        File outputImage = createOutputFile(image, pathForSave, typeFile);

        if ("svg".equalsIgnoreCase(outputFormat)) {
            saveAsSvg(bufImage, outputImage);
            ErrorLogger.info("Conversion to SVG successful. Saved to: " + outputImage.getAbsolutePath());
            return outputImage;
        }

        BufferedImage preparedImage = prepareImageForFormat(bufImage, outputFormat);
        boolean written = ImageIO.write(preparedImage, outputFormat, outputImage);
        if (!written) {
            throw new IOException("Unsupported output format: " + typeFile);
        }

        ErrorLogger.info("Conversion successful. Saved to: " + outputImage.getAbsolutePath());
        return outputImage;
    }

    public static File convertToIco(File image, File pathForSave, int size) throws IOException {
        validateSourceImage(image);
        validateOutputDirectory(pathForSave);

        if (size <= 0) {
            throw new IllegalArgumentException("Size must be greater than 0");
        }

        ErrorLogger.info("Converting image: " + image.getName() + " to ICO (size: " + size + ")");

        BufferedImage bufImage = readImage(image);
        BufferedImage resized = resizeImage(bufImage, size, size);
        File outputImage = createOutputFile(image, pathForSave, "ico");

        ICOEncoder.write(resized, outputImage);
        ErrorLogger.info("ICO conversion successful. Saved to: " + outputImage.getAbsolutePath());
        return outputImage;
    }

    public static File convertFromIco(File image, File pathForSave, String typeFile) throws IOException {
        validateSourceImage(image);
        validateOutputDirectory(pathForSave);

        ErrorLogger.info("Converting from ICO: " + image.getName() + " to " + typeFile);

        List<BufferedImage> images = ICODecoder.read(image);
        if (images.isEmpty()) {
            throw new IOException("ICO file does not contain images!");
        }

        String outputFormat = normalizeOutputFormat(typeFile);
        BufferedImage bestImage = prepareImageForFormat(UsefulMethods.getLargestImage(images), outputFormat);
        File outputImage = createOutputFile(image, pathForSave, typeFile);

        boolean written = ImageIO.write(bestImage, outputFormat, outputImage);
        if (!written) {
            throw new IOException("Unsupported output format: " + typeFile);
        }

        ErrorLogger.info("Conversion from ICO successful. Saved to: " + outputImage.getAbsolutePath());
        return outputImage;
    }

    public static String normalizeOutputFormat(String typeFile) {
        if (typeFile == null) {
            throw new IllegalArgumentException("Output format is null");
        }

        switch (typeFile){
            case "tiff", "tif" -> {
                return "tif";
            }
            case "jpeg", "jpg", "jpe" -> {
                if(typeFile.equalsIgnoreCase("jpe")){
                    return "jpe";
                }
                return "jpg";
            }
            case "bmp" -> {
                return "bmp";
            }
            default -> {
                return typeFile.toLowerCase(Locale.ROOT);
            }
        }
    }

    public static BufferedImage prepareImageForFormat(BufferedImage source, String format) {
        if (!"jpg".equals(format) && !"jpeg".equals(format) && !"bmp".equals(format)) {
            return source;
        }

        BufferedImage rgbImage = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return rgbImage;
    }

    private static BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();

        return resized;
    }

    private static void validateSourceImage(File image) {
        if (image == null || !image.exists() || !image.isFile()) {
            throw new IllegalArgumentException("Source image is invalid");
        }
    }

    private static void validateOutputDirectory(File pathForSave) {
        if (pathForSave == null || !pathForSave.exists() || !pathForSave.isDirectory()) {
            throw new IllegalArgumentException("Output directory is invalid");
        }
    }

    private static BufferedImage readImage(File image) throws IOException {
        BufferedImage bufferedImage = UsefulMethods.readPreviewImage(image);
        if (bufferedImage == null) {
            throw new IOException("Unable to read image: " + image.getName());
        }

        return bufferedImage;
    }

    private static File createOutputFile(File image, File pathForSave, String extension) {
        String normalizedExtension = extension.toLowerCase(Locale.ROOT);
        String fileName = getBaseName(image.getName())
                + "_"
                + UUID.randomUUID().toString().replace("-", "")
                + "."
                + normalizedExtension;

        return new File(pathForSave, fileName);
    }

    private static String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return fileName;
        }

        return fileName.substring(0, dotIndex);
    }

    private static void saveAsSvg(BufferedImage image, File outputFile) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
            writer.write(String.format("<svg width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n",
                    image.getWidth(), image.getHeight()));
            writer.write(String.format("  <image width=\"%d\" height=\"%d\" xlink:href=\"data:image/png;base64,%s\"/>\n",
                    image.getWidth(), image.getHeight(), base64));
            writer.write("</svg>");
        }
    }
}

