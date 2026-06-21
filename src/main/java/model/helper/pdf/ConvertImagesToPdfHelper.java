package model.helper.pdf;

import model.logger.ErrorLogger;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class ConvertImagesToPdfHelper {

    private static final String[] SUPPORTED_IMAGE_EXTENSIONS = {
        ".png", ".jpg", ".jpeg", ".tiff", ".svg", ".bmp"
    };

    public boolean isImageFile(File file) {
        if (file == null || !file.exists()) {
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
     * Generates a unique output file path by appending a counter if the file already exists
     * 
     * @param outputDirectory directory where the file will be saved
     * @param baseName base name for the PDF file (without extension)
     * @return unique File object with .pdf extension
     */
    public File generateUniqueOutputFile(String outputDirectory, String baseName) {
        String shortId = UUID.randomUUID().toString().substring(0, 8);
        File outputFile = new File(outputDirectory + File.separator + baseName + "_" + shortId + ".pdf");
        
        int counter = 1;
        while (outputFile.exists()) {
            outputFile = new File(outputDirectory + File.separator + baseName + "_" + shortId + "_" + counter + ".pdf");
            counter++;
        }
        
        return outputFile;
    }

    /**
     * Parses margin value from ComboBox selection
     * 
     * @param comboValue value from ComboBox (e.g., "No margin", "Small", "Big")
     * @return lowercase margin value for processing
     */
    public String parseMarginValue(String comboValue) {
        if (comboValue == null) {
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
        if (comboValue == null) {
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
        if (comboValue == null) {
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
            Consumer<Integer> progressCallback
    ) {
        
        if (imageFiles == null || imageFiles.isEmpty()) {
            throw new IllegalArgumentException("Image files list is empty");
        }

        PDDocument finalDoc = new PDDocument();
        ConverterPdfHelper pdfHelper = new ConverterPdfHelper();
        
        int totalImages = imageFiles.size();
        List<PDDocument> tempDocuments = new ArrayList<>();
        
        for (int i = 0; i < totalImages; i++) {
            File imageFile = imageFiles.get(i);
            
            if (!isImageFile(imageFile)) {
                ErrorLogger.warn("Skipping non-image file: " + imageFile.getName());
                continue;
            }
            
            pdfHelper.getDocumentFromImage(
                imageFile.getAbsolutePath(),
                margin,
                pageSize,
                orientation
            ).ifPresent(tempDocuments::add);
            
            if (progressCallback != null) {
                int progress = 30 + (50 * (i + 1) / totalImages);
                progressCallback.accept(progress);
            }
        }
        
        for (int i = 0; i < tempDocuments.size(); i++) {
            PDDocument tempDoc = tempDocuments.get(i);
            try {
                for (int pageIndex = 0; pageIndex < tempDoc.getNumberOfPages(); pageIndex++) {
                    finalDoc.addPage(tempDoc.getPage(pageIndex));
                }
            } catch (Exception e) {
                ErrorLogger.error("Error adding page from document " + i + ": " + e.getMessage());
            }
            
            if (progressCallback != null) {
                int progress = 80 + (10 * (i + 1) / tempDocuments.size());
                progressCallback.accept(progress);
            }
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
        if (document == null) {
            throw new IllegalArgumentException("PDF document is null");
        }
        
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file is null");
        }
        
        document.save(outputFile);
        document.close();
    }
}
