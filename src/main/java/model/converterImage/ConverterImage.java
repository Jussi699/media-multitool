package model.converterImage;

import com.luciad.imageio.webp.WebPWriteParam;
import model.logger.ErrorLogger;
import net.ifok.image.image4j.codec.ico.ICOEncoder;
import net.ifok.image.image4j.codec.ico.ICODecoder;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ConverterImage {
    public static File convert(File image, File pathForSave, String typeFile) throws IOException {
        validateSourceImage(image);
        validateOutputDirectory(pathForSave);

        ErrorLogger.info("Starting conversion for image: " + image.getName() + " to " + typeFile);

        BufferedImage bufImage = readImage(image);
        String outputFormat = normalizeOutputFormat(typeFile);
        BufferedImage preparedImage = prepareImageForFormat(bufImage, outputFormat);
        File outputImage = createOutputFile(image, pathForSave, typeFile);

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

        return "jpeg".equalsIgnoreCase(typeFile) ? "jpg" : typeFile.toLowerCase(Locale.ROOT);
    }

    public static BufferedImage prepareImageForFormat(BufferedImage source, String format) {
        if (!"jpg".equals(format) && !"jpeg".equals(format)) {
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
        BufferedImage bufferedImage = ImageIO.read(image);
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

    private static void writeWebp(BufferedImage image, File outputFile) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) {
            writers = ImageIO.getImageWritersByFormatName("webp");
        }

        if (!writers.hasNext()) {
            throw new IOException("WebP writer is not available. Add org.sejda.imageio:webp-imageio dependency.");
        }

        ImageWriter writer = writers.next();

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(ios);

            WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());
            writeParam.setCompressionMode(WebPWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionType(writeParam.getCompressionTypes()[WebPWriteParam.LOSSY_COMPRESSION]);
            writeParam.setCompressionQuality(0.90f);

            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }
    }
}

