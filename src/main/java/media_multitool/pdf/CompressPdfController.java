package media_multitool.pdf;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import media_multitool.AbstractMediaController;
import model.checks.Checking;
import model.helper.pdf.CompressPdfHelper;
import model.helper.pdf.PdfHelper;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import viewHelp.Alerts;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static model.utility.PathWorker.*;
import static viewHelp.Message.*;
import static viewHelp.Utility.formatFileSize;

public class CompressPdfController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML private ImageView imageViewPdf;
    @FXML private StackPane dropZone, previewContainer;
    @FXML private Button btnSelectFile, btnChoiceDirForSaveFile, btnSubmit, btnReset;
    @FXML private Label labelSelectFileName, textDragZone, labelPreviewPlaceholder;

    @FXML private ToggleButton btnLowCompression, btnMediumCompression, btnHighCompression;
    @FXML private Label labelEstimatedSize;

    private PDDocument currentDoc;
    private List<Control> listControls;

    @FXML
    public void initialize() {
        listControls = List.of(btnLowCompression, btnMediumCompression, btnHighCompression, btnReset, btnSubmit, btnChoiceDirForSaveFile);

        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupDragAndDrop(dropZone, List.of("pdf"), this::loadFile);

        isPressedReset();
    }

    @Override
    protected void lockUI() {
        listControls.forEach(lc -> lc.setDisable(true));
    }

    @Override
    protected void unlockUI() {
        listControls.forEach(lc -> lc.setDisable(false));
    }

    @Override
    protected void disableControls() {
        listControls.forEach(lc -> lc.setDisable(true));
    }

    @Override
    protected void enableControls() {
        listControls.forEach(lc -> lc.setDisable(false));
    }

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                "Select PDF"
        ).ifPresent(this::loadFile);
    }

    @FXML
    public void onActionChoiceDirForSaveFile() {
        selectOutputDirectory(btnChoiceDirForSaveFile, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save PDF");
    }

    @FXML
    public void submitAndDownload() {
        if (Checking.checkImageAndOutputOnNull(imageProperties)) {
            return;
        }

        CompressPdfHelper.CompressionLevel selectedLevel = getSelectedCompressionLevel();

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);

                File outputFile = createOutputFile(
                        imageProperties.getImage(),
                        imageProperties.getOutput(),
                        "pdf"
                );

                updateProgress(30, 100);

                CompressPdfHelper.compressPdf(imageProperties.getImage(), outputFile, selectedLevel);

                updateProgress(100, 100);

                return outputFile;
            }
        };

        executeMediaTask(task);
        labelSuccess.setManaged(true);
    }

    private void updatePreview() {
        if (currentDoc != null) {
            try {
                PDFRenderer renderer = new PDFRenderer(currentDoc);
                BufferedImage bim = renderer.renderImageWithDPI(0, 72);
                imageViewPdf.setImage(SwingFXUtils.toFXImage(bim, null));
                labelPreviewPlaceholder.setVisible(false);
            } catch (IOException e) {
                ErrorLogger.error("Error rendering PDF preview: " + e.getMessage());
            }
        }
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info("Conversion to PDF successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "PDF saved!", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @Override
    protected void handleTaskFailure(Throwable exception) {
        super.handleTaskFailure(exception);
        Platform.runLater(() -> {
            showErrorMessage(labelSuccess, "Error: " + exception.getMessage(), imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFileName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPdf, progressBar, true
        );
        reset(imageProperties, ctx, "Selected PDF file: none");

        disableControls();

        try {
            currentDoc = PdfHelper.closeDocument(currentDoc);
        } catch (IOException e) {
            ErrorLogger.error("Error closing document during reset: " + e.getMessage());
        }
        
        if (imageViewPdf != null) {
            imageViewPdf.setImage(null);
        }

        labelPreviewPlaceholder.setVisible(true);
        labelEstimatedSize.setText("Estimated size: --");
        btnLowCompression.setSelected(true);
    }

    @FXML
    public void onCompressionLevelChanged() {
        updateEstimatedSize();
    }

    private void updateEstimatedSize() {
        if (imageProperties.getImage() == null) {
            labelEstimatedSize.setText("Estimated size: --");
            return;
        }

        long originalSize = imageProperties.getImage().length();
        CompressPdfHelper.CompressionLevel level = getSelectedCompressionLevel();

        double factor = switch (level) {
            case LOW    -> 0.9;
            case MEDIUM -> 0.6;
            case HIGH   -> 0.3;
        };

        long estimatedSize = (long) (originalSize * factor);
        labelEstimatedSize.setText("Estimated size: ~" + formatFileSize(estimatedSize));
    }

    private CompressPdfHelper.CompressionLevel getSelectedCompressionLevel() {
        if (btnHighCompression.isSelected()) return CompressPdfHelper.CompressionLevel.HIGH;
        if (btnMediumCompression.isSelected()) return CompressPdfHelper.CompressionLevel.MEDIUM;
        return CompressPdfHelper.CompressionLevel.LOW;
    }



    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Compress PDF",
                """
                        How to use:
                        1. Select a PDF file using 'Select PDF' or drag and drop.
                        2. Select the desired compression preset.
                        3. Click 'Submit and Download' to save the compressed PDF.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    private void isEncrypted(PDDocument doc) {
        if (doc.isEncrypted()) {
            showErrorMessage(labelSuccess, "PDF file is encrypted! Please unlock it first.", imageProperties.getHideSuccessMessageTimer());
            disableControls();
            labelPreviewPlaceholder.setText("Encrypted PDF - Preview unavailable");
            labelPreviewPlaceholder.setVisible(true);
            imageViewPdf.setImage(null);
        }
    }

    private void loadFile(File selectedFile) {
        enableControls();
        hideSuccessMessage(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);

        try {
            currentDoc = PdfHelper.closeDocument(currentDoc);
            currentDoc = Loader.loadPDF(selectedFile);

            isEncrypted(currentDoc);
        } catch (IOException e) {
            String errorMsg = e.getMessage().toLowerCase();
            if (errorMsg.contains("password") || errorMsg.contains("encrypted") || errorMsg.contains("owner") || errorMsg.contains("user")) {
                showErrorMessage(labelSuccess, "PDF file is encrypted! Please unlock it first.", imageProperties.getHideSuccessMessageTimer());
            } else {
                ErrorLogger.error("Error loading PDF document: " + e.getMessage());
            }
            disableControls();
            labelPreviewPlaceholder.setText("Error loading PDF");
            labelPreviewPlaceholder.setVisible(true);
            imageViewPdf.setImage(null);
        }

        imageProperties.setImage(selectedFile);
        imageProperties.setTypeImage(DetermineType.determineFormat(selectedFile).orElse(null));
        labelSelectFileName.setText("Select PDF: " + selectedFile.getName());
        textDragZone.setText("Selected: " + selectedFile.getName());

        if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        bindingImageViewToPreviewContainer(imageViewPdf, previewContainer);
        if (currentDoc != null && !currentDoc.isEncrypted()) {
            updatePreview();
        }
        updateEstimatedSize();
    }
}
