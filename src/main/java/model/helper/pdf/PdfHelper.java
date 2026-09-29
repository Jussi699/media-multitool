package model.helper.pdf;

import model.logger.ErrorLogger;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;

import static java.util.Objects.nonNull;

public class PdfHelper {
    private PdfHelper() {
        /* This utility class should not be instantiated */
    }

    public static PDDocument closeDocument(PDDocument document) throws IOException {
        if (nonNull(document)) {
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
