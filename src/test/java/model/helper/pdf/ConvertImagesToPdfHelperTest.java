package model.helper.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import services.ImageTestService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvertImagesToPdfHelperTest {
    @TempDir
    Path tempDir;

    @Test
    void mergesConvertedImagesAndRetainsPageResources() throws IOException {
        File firstImage = ImageTestService.createPng(tempDir, "first.png", 40, 30);
        File secondImage = ImageTestService.createPng(tempDir, "second.png", 20, 50);
        File output = tempDir.resolve("merged.pdf").toFile();
        ConvertImagesToPdfHelper helper = new ConvertImagesToPdfHelper();

        PDDocument document = helper.convertImagesToPdf(
                List.of(firstImage, secondImage), "no margin", "fix", "portrait", null);
        helper.savePdfDocument(document, output);

        try (PDDocument savedDocument = Loader.loadPDF(output)) {
            assertEquals(2, savedDocument.getNumberOfPages());
            assertTrue(savedDocument.getPage(0).getResources().getXObjectNames().iterator().hasNext());
            assertTrue(savedDocument.getPage(1).getResources().getXObjectNames().iterator().hasNext());
        }
    }

    @Test
    void failsInsteadOfReturningAnEmptyPdfWhenAnImageCannotBeConverted() throws IOException {
        File invalidImage = tempDir.resolve("invalid.png").toFile();
        Files.writeString(invalidImage.toPath(), "not an image");

        assertThrows(IOException.class, () -> {
            try (PDDocument doc = new ConvertImagesToPdfHelper().convertImagesToPdf(
                    List.of(invalidImage), "no margin", "fix", "portrait", null)) {
                assertNotNull(doc);
            }
        });
    }
}
