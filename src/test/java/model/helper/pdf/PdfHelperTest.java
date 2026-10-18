package model.helper.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfHelperTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsUnencryptedPdf() throws IOException {
        File file = tempDir.resolve("plain.pdf").toFile();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(file);
        }

        try (PDDocument document = PdfHelper.loadUnencryptedPdf(file)) {
            assertEquals(1, document.getNumberOfPages());
        }
    }

    @Test
    void rejectsEncryptedPdfWithConsistentException() throws IOException {
        File file = tempDir.resolve("encrypted.pdf").toFile();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            StandardProtectionPolicy policy = new StandardProtectionPolicy(
                    "owner-password", "user-password", new AccessPermission());
            policy.setEncryptionKeyLength(256);
            document.protect(policy);
            document.save(file);
        }

        IOException exception = assertThrows(
                IOException.class,
                () -> PdfHelper.loadUnencryptedPdf(file).close());

        assertEquals(PdfHelper.getEncryptedPdfMessage(), exception.getMessage());
        assertTrue(PdfHelper.isEncryptedException(exception));
    }

    @Test
    void rejectsEncryptedPdfThatCanBeOpenedWithEmptyUserPassword() throws IOException {
        File file = tempDir.resolve("empty-user-password.pdf").toFile();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            StandardProtectionPolicy policy = new StandardProtectionPolicy(
                    "owner-password", "", new AccessPermission());
            policy.setEncryptionKeyLength(256);
            document.protect(policy);
            document.save(file);
        }

        IOException exception = assertThrows(
                IOException.class,
                () -> PdfHelper.loadUnencryptedPdf(file).close());

        assertEquals(PdfHelper.getEncryptedPdfMessage(), exception.getMessage());
        assertTrue(PdfHelper.isEncryptedException(exception));
    }
}
