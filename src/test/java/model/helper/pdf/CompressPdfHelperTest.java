package model.helper.pdf;

import model.enums.CompressionLevel;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompressPdfHelperTest {
    @TempDir
    Path tempDir;

    @Test
    void lowCompressionOptimizesImageHeavyPdf() throws IOException {
        File inputFile = createImagePdf(createGradientImage());
        File outputFile = tempDir.resolve("compressed.pdf").toFile();

        CompressPdfHelper.compressPdf(inputFile, outputFile, CompressionLevel.LOW, null);

        assertTrue(outputFile.length() < inputFile.length(),
                "Low compression should optimize an image-heavy PDF");
    }

    @Test
    void highCompressionPreservesTransparentImagePixels() throws IOException {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(10, 10, Color.RED.getRGB());
        File inputFile = createTransparentImagePdf(image);
        File outputFile = tempDir.resolve("transparent-compressed.pdf").toFile();

        CompressPdfHelper.compressPdf(inputFile, outputFile, CompressionLevel.HIGH, null);

        try (PDDocument document = Loader.loadPDF(outputFile)) {
            BufferedImage rendered = new PDFRenderer(document).renderImage(0);
            assertEquals(Color.BLUE.getRGB(), rendered.getRGB(2, 2));
            assertEquals(Color.RED.getRGB(), rendered.getRGB(10, 10));
        }
    }

    private File createImagePdf(BufferedImage image) throws IOException {
        File file = tempDir.resolve("image.pdf").toFile();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(100, 100));
            document.addPage(page);
            PDImageXObject pdfImage = LosslessFactory.createFromImage(document, image);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.drawImage(pdfImage, 0, 0, 100, 100);
            }
            document.save(file);
        }
        return file;
    }

    private File createTransparentImagePdf(BufferedImage image) throws IOException {
        File file = tempDir.resolve("transparent-image.pdf").toFile();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(20, 20));
            document.addPage(page);
            PDImageXObject pdfImage = LosslessFactory.createFromImage(document, image);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setNonStrokingColor(Color.BLUE);
                content.addRect(0, 0, 20, 20);
                content.fill();
                content.drawImage(pdfImage, 0, 0, 20, 20);
            }
            document.save(file);
        }
        return file;
    }

    private BufferedImage createGradientImage() {
        BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(1);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, 0xff000000 | random.nextInt(0x1000000));
            }
        }
        return image;
    }
}
