package model.helper.pdf;

import model.logger.ErrorLogger;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.IntConsumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class ConvertImagesToPdfHelper {

    private static final String[] SUPPORTED_IMAGE_EXTENSIONS = {
        ".png", ".jpg", ".jpeg", ".tiff", ".svg", ".bmp"
    };

    public boolean isImageFile(File file) {
        if (isNull(file) || !file.exists()) {
            return false;
        }
        
        String name = file.getName().toLowerCase();
        for (String extension : SUPPORTED_IMAGE_EXTENSIONS) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parses margin value from ComboBox selection
     * 
     * @param comboValue value from ComboBox (e.g., "No margin", "Small", "Big")
     * @return lowercase margin value for processing
     */
    public String parseMarginValue(String comboValue) {
        if (isNull(comboValue)) {
            return "no margin";
        }
        return comboValue.toLowerCase();
    }

    /**
     * Parses orientation value from ComboBox selection
     * 
     * @param comboValue value from ComboBox (e.g., "Portrait", "Landscape")
     * @return lowercase orientation value for processing
     */
    public String parseOrientationValue(String comboValue) {
        if (isNull(comboValue)) {
            return "portrait";
        }
        return comboValue.toLowerCase();
    }

    /**
     * Parses page size value from ComboBox selection
     * 
     * @param comboValue value from ComboBox (e.g., "A4 297x210 mm", "US Letter 215x279,4 mm", "Fix (image size)")
     * @return parsed page size value ("a4", "us letter", or "fix")
     */
    public String parsePageSizeValue(String comboValue) {
        if (isNull(comboValue)) {
            return "fix";
        }
        
        if (comboValue.contains("A4")) {
            return "a4";
        } else if (comboValue.contains("US Letter")) {
            return "us letter";
        }
        
        return "fix";
    }

    /**
     * Converts a list of image files to a PDF document
     * 
     * @param imageFiles list of image files
     * @param margin margin value ("no margin", "small", "big")
     * @param pageSize page size ("fix", "a4", "us letter")
     * @param orientation orientation ("portrait", "landscape")
     * @param progressCallback callback for progress updates (value from 0 to 100)
     * @return PDDocument with added image pages
     */
    public PDDocument convertImagesToPdf(
            List<File> imageFiles,
            String margin,
            String pageSize,
            String orientation,
            IntConsumer progressCallback
    ) throws IOException {
        
        if (isNull(imageFiles) || imageFiles.isEmpty()) {
            throw new IllegalArgumentException("Image files list is empty");
        }

        PDDocument finalDoc = new PDDocument();
        ConverterPdfHelper pdfHelper = new ConverterPdfHelper();
        PDFMergerUtility merger = new PDFMergerUtility();
        
        int totalImages = imageFiles.size();
        try {
            for (int i = 0; i < totalImages; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new RuntimeException(new InterruptedException("Conversion cancelled"));
                }

                File imageFile = imageFiles.get(i);
                
                if (!isImageFile(imageFile)) {
                    ErrorLogger.warn("Skipping non-image file: " + imageFile.getName());
                    continue;
                }
                
                PDDocument imageDocument = pdfHelper.getDocumentFromImage(
                        imageFile.getAbsolutePath(),
                        margin,
                        pageSize,
                        orientation
                ).orElseThrow(() -> new IOException("Unable to convert image: " + imageFile.getName()));
                try (imageDocument) {
                    merger.appendDocument(finalDoc, imageDocument);
                }
                
                if (nonNull(progressCallback)) {
                    int progress = 30 + (60 * (i + 1) / totalImages);
                    progressCallback.accept(progress);
                }
            }
            if (finalDoc.getNumberOfPages() == 0) {
                throw new IOException("No images could be converted to PDF.");
            }
        } catch (IOException | RuntimeException e) {
            try {
                finalDoc.close();
            } catch (IOException closeException) {
                e.addSuppressed(closeException);
            }
            throw e;
        }
        
        return finalDoc;
    }

    /**
     * Saves a PDF document to a file
     * 
     * @param document PDF document
     * @param outputFile file to save to
     * @throws IOException if an error occurred while saving
     */
    public void savePdfDocument(PDDocument document, File outputFile) throws IOException {
        if (isNull(document)) {
            throw new IllegalArgumentException("PDF document is null");
        }
        
        if (isNull(outputFile)) {
            throw new IllegalArgumentException("Output file is null");
        }

        try (document) {
            document.save(outputFile);
        }
    }
}
