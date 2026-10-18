package model.helper.pdf;

import model.enums.CompressionLevel;
import model.logger.ErrorLogger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class CompressPdfHelper {
    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(double workDone, double max);
    }

    public static void compressPdf(File inputFile, File outputFile,
                                   CompressionLevel level, ProgressCallback progressCallback) throws IOException {
        if (nonNull(progressCallback)) {
            progressCallback.onProgress(0, 100);
        }

        try (PDDocument document = PdfHelper.loadUnencryptedPdf(inputFile)) {
            if (nonNull(progressCallback)) {
                progressCallback.onProgress(5, 100);
            }

            int totalPages = document.getNumberOfPages();

            if (totalPages > 0) {
                for (int i = 0; i < totalPages; i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new IOException("Compression cancelled");
                    }
                    PDPage page = document.getPage(i);
                    optimizeResources(page.getResources(), document, level, progressCallback, i, totalPages);

                    if (nonNull(progressCallback)) {
                        double pageProgress = 5.0 + 85.0 * (i + 1) / totalPages;
                        progressCallback.onProgress(pageProgress, 100);
                    }
                }
            } else if (nonNull(progressCallback)) {
                progressCallback.onProgress(85, 100);
            }

            if (Thread.currentThread().isInterrupted()) {
                throw new IOException("Compression cancelled");
            }

            if (nonNull(progressCallback)) {
                progressCallback.onProgress(90, 100);
            }

            document.save(outputFile);

            if (nonNull(progressCallback)) {
                progressCallback.onProgress(100, 100);
            }

            ErrorLogger.info("PDF compressed successfully: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            if (Thread.currentThread().isInterrupted()
                    || "Compression cancelled".equalsIgnoreCase(e.getMessage())) {
                ErrorLogger.info("PDF compression cancelled.");
            } else {
                ErrorLogger.error("Error compressing PDF: " + e.getMessage());
            }
            throw e;
        }
    }

    private static void optimizeResources(PDResources resources, PDDocument document, CompressionLevel level,
                                          ProgressCallback progressCallback, int pageIndex, int totalPages) throws IOException {
        if (isNull(resources)) return;

        List<COSName> xObjectNames = new ArrayList<>();
        for (COSName name : resources.getXObjectNames()) {
            xObjectNames.add(name);
        }
        int totalXObjects = xObjectNames.size();

        for (int j = 0; j < totalXObjects; j++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new IOException("Compression cancelled");
            }

            COSName name = xObjectNames.get(j);
            PDXObject xobject = resources.getXObject(name);
            if (xobject instanceof PDImageXObject image) {
                try {
                    BufferedImage bufferedImage = image.getImage();
                    if (nonNull(bufferedImage)) {
                        byte[] compressedBytes = bufferedImage.getColorModel().hasAlpha()
                                ? encodeImage(bufferedImage, "png", 1.0f)
                                : encodeImage(bufferedImage, "jpg", level.getImageQuality());
                        long originalLength;
                        try (InputStream rawStream = image.getCOSObject().createRawInputStream()) {
                            originalLength = rawStream.transferTo(java.io.OutputStream.nullOutputStream());
                        }

                        if (compressedBytes.length < originalLength) {
                            PDImageXObject compressedImage = PDImageXObject.createFromByteArray(
                                    document, compressedBytes, name.getName());
                            resources.put(name, compressedImage);
                        }
                    }
                } catch (Exception e) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new IOException("Compression cancelled");
                    }
                    ErrorLogger.warn("Skipping image optimization for " + name.getName() + ": " + e.getMessage());
                }
            } else if (xobject instanceof PDFormXObject form) {
                optimizeResources(form.getResources(), document, level, progressCallback, pageIndex, totalPages);
            }

            if (nonNull(progressCallback) && totalPages > 0) {
                double pageBase = 5.0 + 85.0 * pageIndex / totalPages;
                double pageSlice = 85.0 / totalPages;
                double currentProgress = pageBase + pageSlice * (j + 1) / totalXObjects;
                progressCallback.onProgress(currentProgress, 100);
            }
        }
    }

    private static byte[] encodeImage(BufferedImage image, String format, float quality) throws IOException {
        BufferedImage encodedImage = image;
        if ("jpg".equals(format) && image.getType() != BufferedImage.TYPE_INT_RGB) {
            encodedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = encodedImage.createGraphics();
            try {
                graphics.drawImage(image, 0, 0, null);
            } finally {
                graphics.dispose();
            }
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) throw new IllegalStateException("No writers for " + format);

        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionType(param.getCompressionTypes()[0]);
                param.setCompressionQuality(quality);
            }

            writer.write(null, new IIOImage(encodedImage, null, null), param);
            return baos.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
