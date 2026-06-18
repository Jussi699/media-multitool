package model.helper.pdf;

import model.logger.ErrorLogger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import java.io.File;
import java.io.IOException;

public class ProtectPdfHelper {
    public static File protectPdf(File inputFile, File outputFile, String ownerPassword, String userPassword) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputFile)) {
            AccessPermission ap = new AccessPermission();
            
            ap.setCanPrint(false);
            ap.setCanModify(false);
            ap.setCanExtractContent(false);
            ap.setCanModifyAnnotations(false);
            ap.setCanFillInForm(false);
            ap.setCanExtractForAccessibility(true);
            ap.setCanAssembleDocument(false);

            StandardProtectionPolicy policy = new StandardProtectionPolicy(ownerPassword, userPassword, ap);
            policy.setEncryptionKeyLength(256);
            policy.setPermissions(ap);

            document.protect(policy);
            document.save(outputFile);
            
            ErrorLogger.info("PDF protected successfully: " + outputFile.getAbsolutePath());
            return outputFile;
        } catch (IOException e) {
            ErrorLogger.error("Error protecting PDF: " + e.getMessage());
            throw e;
        }
    }
}
