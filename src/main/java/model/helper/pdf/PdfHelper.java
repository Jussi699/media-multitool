package model.helper.pdf;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static visual.Message.showErrorMessage;

public class PdfHelper {
    private static final String ENCRYPTED_PDF_MESSAGE = "PDF file is encrypted! Please unlock it first.";

    private PdfHelper() {
        /* This utility class should not be instantiated */
    }

    public static PDDocument loadUnencryptedPdf(File file) throws IOException {
        PDDocument document;
        try {
            document = Loader.loadPDF(file);
        } catch (InvalidPasswordException exception) {
            throw new EncryptedPdfException(exception);
        }

        if (document.isEncrypted()) {
            EncryptedPdfException exception = new EncryptedPdfException();
            try {
                document.close();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
        return document;
    }

    public static boolean isEncryptedException(Throwable exception) {
        for (Throwable cause = exception; nonNull(cause); cause = cause.getCause()) {
            if (cause instanceof EncryptedPdfException || cause instanceof InvalidPasswordException) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEncryptedDocument(PDDocument document) {
        return nonNull(document) && document.isEncrypted();
    }

    public static String getEncryptedPdfMessage() {
        return ENCRYPTED_PDF_MESSAGE;
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

    public static final class EncryptedPdfException extends IOException {
        private EncryptedPdfException() {
            super(ENCRYPTED_PDF_MESSAGE);
        }

        private EncryptedPdfException(InvalidPasswordException cause) {
            super(ENCRYPTED_PDF_MESSAGE, cause);
        }
    }

    public static class PageEntry {
        public final int originalPageIndex;
        public final String id;

        public PageEntry(int index) {
            this.originalPageIndex = index;
            this.id = UUID.randomUUID().toString();
        }
    }

    public static boolean notNullInputAndOutput(ImageProperties properties, Label labelSuccess, ProgressBar progressBar) {
        if (isNull(properties.getImage())) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Please select a PDF file", properties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
            return true;
        }

        if (isNull(properties.getOutput())) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Please select output directory", properties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
            return true;
        }

        return false;
    }
}
