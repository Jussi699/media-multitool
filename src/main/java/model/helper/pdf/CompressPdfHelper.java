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
import java.util.Iterator;

public class CompressPdfHelper {
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

    public static File compressPdf(File inputFile, File outputFile, CompressionLevel level) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputFile)) {
            
            if (level != CompressionLevel.LOW) {
                for (PDPage page : document.getPages()) {
                    optimizeResources(page.getResources(), document, level);
                }
            }

            document.setAllSecurityToBeRemoved(true);
            document.save(outputFile);

            ErrorLogger.info("PDF compressed successfully: " + outputFile.getAbsolutePath());
            return outputFile;
        } catch (IOException e) {
            ErrorLogger.error("Error compressing PDF: " + e.getMessage());
            throw e;
        }
    }

    private static void optimizeResources(PDResources resources, PDDocument document, CompressionLevel level) throws IOException {
        if (resources == null) return;

        for (COSName name : resources.getXObjectNames()) {
            PDXObject xobject = resources.getXObject(name);
            if (xobject instanceof PDImageXObject image) {
                BufferedImage bufferedImage = image.getImage();
                if (bufferedImage != null) {
                    byte[] compressedBytes = compressImage(bufferedImage, level.getImageQuality());
                    PDImageXObject compressedImage = PDImageXObject.createFromByteArray(document, compressedBytes, name.getName());
                    resources.put(name, compressedImage);
                }
            } else if (xobject instanceof PDFormXObject form) {
                optimizeResources(form.getResources(), document, level);
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
