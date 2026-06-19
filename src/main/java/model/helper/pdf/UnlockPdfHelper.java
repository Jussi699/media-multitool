package model.helper.pdf;

import model.logger.ErrorLogger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;

public class UnlockPdfHelper {
    public static File unlockPdf(File inputFile, File outputFile, String password) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputFile, password)) {
            document.setAllSecurityToBeRemoved(true);
            document.save(outputFile);
            ErrorLogger.info("PDF protected successfully: " + outputFile.getAbsolutePath());

            return outputFile;
        }
        catch (IOException e) {
            ErrorLogger.error("Error unlock PDF: " + e.getMessage());
            throw e;
        }
    }
}
