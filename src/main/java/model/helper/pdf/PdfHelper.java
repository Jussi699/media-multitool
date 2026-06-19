package model.helper.pdf;

import model.logger.ErrorLogger;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;

public class PdfHelper {
    public static PDDocument closeDocument(PDDocument document) throws IOException {
        if (document != null) {
            try {
                document.close();
            } catch (IOException e) {
                ErrorLogger.error("Error closing PDF document: " + e.getMessage());
                throw e;
            }
            document = null;
        }
        return document;
    }
}
