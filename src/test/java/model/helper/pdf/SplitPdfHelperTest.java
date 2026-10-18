package model.helper.pdf;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SplitPdfHelperTest {
    @TempDir
    Path tempDir;

    @Test
    void splitsRequestedPageRange() throws IOException {
        File inputFile = tempDir.resolve("source.pdf").toFile();
        try (PDDocument source = new PDDocument()) {
            source.addPage(new PDPage());
            source.addPage(new PDPage());
            source.addPage(new PDPage());

            File outputFile = SplitPdfHelper.splitByRange(
                    source, new PDFMergerUtility(), tempDir.toFile(), (done, total) -> assertTrue(done <= total),
                    2, 3, inputFile);

            assertTrue(outputFile.isFile());
            try (PDDocument result = org.apache.pdfbox.Loader.loadPDF(outputFile)) {
                assertEquals(2, result.getNumberOfPages());
            }
        }
    }

    @Test
    void rejectsInvalidPageRangeInsteadOfSplittingWholeDocument() throws IOException {
        try (PDDocument source = new PDDocument()) {
            source.addPage(new PDPage());
            source.addPage(new PDPage());

            assertThrows(IllegalArgumentException.class, () -> SplitPdfHelper.splitByRange(
                    source, new PDFMergerUtility(), tempDir.toFile(), (done, total) -> assertTrue(done <= total),
                    3, 4, tempDir.resolve("source.pdf").toFile()));
        }
    }
}
