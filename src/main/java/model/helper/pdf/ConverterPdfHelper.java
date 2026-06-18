package model.helper.pdf;

import model.logger.ErrorLogger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.util.Optional;

public class ConverterPdfHelper {

    public Optional<PDDocument> getDocumentFromImage(String imagePath, String marginType, String pageSizeType, String orientation) {
        PDDocument doc = new PDDocument();
        try {
            PDImageXObject pdImage = PDImageXObject.createFromFile(imagePath, doc);
            
            PDRectangle pageSize = determinePageSize(pageSizeType, pdImage, orientation);
            float margin = determineMargin(marginType);
            
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            float printableWidth = pageSize.getWidth() - (margin * 2);
            float printableHeight = pageSize.getHeight() - (margin * 2);

            float imageWidth = pdImage.getWidth();
            float imageHeight = pdImage.getHeight();

            float scale = Math.min(printableWidth / imageWidth, printableHeight / imageHeight);
            
            if (pageSizeType.equals("fix") && margin == 0) {
                scale = 1.0f;
            }

            float finalWidth = imageWidth * scale;
            float finalHeight = imageHeight * scale;

            float x = (pageSize.getWidth() - finalWidth) / 2;
            float y = (pageSize.getHeight() - finalHeight) / 2;

            try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                contentStream.drawImage(pdImage, x, y, finalWidth, finalHeight);
            }

            return Optional.of(doc);
        } catch (IOException e) {
            ErrorLogger.error("Error processing image conversion: " + e.getMessage());
            try {
                doc.close();
            } catch (IOException ex) {
                ErrorLogger.error("Error occurred while closing: " + ex.getMessage());
            }
            return Optional.empty();
        }
    }

    private PDRectangle determinePageSize(String pageSizeType, PDImageXObject image, String orientation) {
        PDRectangle rect;
        switch (pageSizeType.toLowerCase()) {
            case "a4":
                rect = PDRectangle.A4;
                break;
            case "us letter":
                rect = PDRectangle.LETTER;
                break;
            case "fix":
            default:
                rect = new PDRectangle(image.getWidth(), image.getHeight());
                return rect;
        }

        if ("landscape".equalsIgnoreCase(orientation)) {
            rect = new PDRectangle(rect.getHeight(), rect.getWidth());
        }
        return rect;
    }

    private float determineMargin(String marginType) {
        return switch (marginType.toLowerCase()) {
            case "small" -> 20f;
            case "big" -> 50f;
            default -> 0f;
        };
    }
}
