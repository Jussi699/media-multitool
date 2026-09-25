package model.helper.pdf;

import lombok.Getter;
import model.logger.ErrorLogger;
import org.apache.pdfbox.Loader;
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
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CompressPdfHelper {
    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(double workDone, double max);
    }

    @Getter
    public enum CompressionLevel {
        LOW(1.0f),
        MEDIUM(0.6f),
        HIGH(0.3f);

        private final float imageQuality;

        CompressionLevel(float imageQuality) {
            this.imageQuality = imageQuality;
        }
    }

    public static void compressPdf(File inputFile, File outputFile, CompressionLevel level, ProgressCallback progressCallback) throws IOException {
        if (progressCallback != null) {
            progressCallback.onProgress(0, 100);
        }

        try (PDDocument document = Loader.loadPDF(inputFile)) {
            if (progressCallback != null) {
                progressCallback.onProgress(5, 100);
            }

            int totalPages = document.getNumberOfPages();

            if (level != CompressionLevel.LOW && totalPages > 0) {
                for (int i = 0; i < totalPages; i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new IOException("Compression cancelled");
                    }
                    PDPage page = document.getPage(i);
                    optimizeResources(page.getResources(), document, level, progressCallback, i, totalPages);

                    if (progressCallback != null) {
                        double pageProgress = 5.0 + 85.0 * (i + 1) / totalPages;
                        progressCallback.onProgress(pageProgress, 100);
                    }
                }
            } else if (progressCallback != null) {
                progressCallback.onProgress(85, 100);
            }

            if (Thread.currentThread().isInterrupted()) {
                throw new IOException("Compression cancelled");
            }

            if (progressCallback != null) {
                progressCallback.onProgress(90, 100);
            }

            document.setAllSecurityToBeRemoved(true);
            document.save(outputFile);

            if (progressCallback != null) {
                progressCallback.onProgress(100, 100);
            }

            ErrorLogger.info("PDF compressed successfully: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            ErrorLogger.error("Error compressing PDF: " + e.getMessage());
            throw e;
        }
    }

    private static void optimizeResources(PDResources resources, PDDocument document, CompressionLevel level,
                                          ProgressCallback progressCallback, int pageIndex, int totalPages) throws IOException {
        if (resources == null) return;

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
                    if (bufferedImage != null) {
                        byte[] compressedBytes = compressImage(bufferedImage, level.getImageQuality());
                        PDImageXObject compressedImage = PDImageXObject.createFromByteArray(document, compressedBytes, name.getName());
                        resources.put(name, compressedImage);
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

            if (progressCallback != null && totalPages > 0 && totalXObjects > 0) {
                double pageBase = 5.0 + 85.0 * pageIndex / totalPages;
                double pageSlice = 85.0 / totalPages;
                double currentProgress = pageBase + pageSlice * (j + 1) / totalXObjects;
                progressCallback.onProgress(currentProgress, 100);
            }
        }
    }

    private static byte[] compressImage(BufferedImage image, float quality) throws IOException {
        BufferedImage rgbImage = image;
        if (image.getType() != BufferedImage.TYPE_INT_RGB) {
            rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgbImage.getGraphics().drawImage(image, 0, 0, null);
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) throw new IllegalStateException("No writers for jpg");

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

            writer.write(null, new IIOImage(rgbImage, null, null), param);
            return baos.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
