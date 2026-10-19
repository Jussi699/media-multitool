package controllers.pdfTools;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import controllers.abstractControllers.AbstractPdfToolController;
import model.utility.checks.Checking;
import model.helper.pdf.PdfHelper;
import model.logger.ErrorLogger;
import model.utility.context.ResetContext;
import model.utility.format.SupportedFormat;
import model.utility.io.FileUtils;
import model.utility.media.ImageFormatDetector;
import org.apache.pdfbox.pdmodel.PDDocument;
import visual.dialogs.InfoAlert;

import java.io.File;
import java.io.IOException;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.helper.pdf.ConverterPdfHelper.getDocumentFromImage;
import static model.utility.io.PathWorker.getSavedPath;
import static visual.controls.Message.*;

public class ConverterImageToPdfController extends AbstractPdfToolController {
    @FXML private ComboBox<String> comboMargin, comboOrientation, comboPageSize;

    @FXML
    public void initialize() {
        properties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, properties.getHideSuccessMessageTimer(), true);

        initComboBoxes();
        setupDragAndDrop(dropZone, SupportedFormat.getAllSupportedImageFormats(), this::loadFile);

        isPressedReset();
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "Image to PDF",
                "1. Select an image file using \"Select image\" or drag and drop it into the dash-bordered zone;",
                """
                        3. (Optional) Drag image thumbnails to reorder them, or click the remove button to delete an image;

                        4. Select the desired margin, page orientation, and page size;
                        
                        5. Click "Submit and Download".
                        
                        You can cancel the conversion at any time using the "Cancel Conversion" button.
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        cancelProcessing();

        ResetContext ctx = new ResetContext(
                labelSelectFileName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPdf, progressBar, true, "image"
        );
        reset(properties, ctx, "Selected image file: none");

        disableControls();

        try {
            currentDoc = closeDocument(currentDoc);
        } catch (IOException e) {
            ErrorLogger.error("Error closing PDF document: " + e.getMessage());
        }

        imageViewPdf.setImage(null);
        labelPreviewPlaceholder.setVisible(true);

        comboMargin.setValue("No margin");
        comboOrientation.setValue("Portrait");
        comboPageSize.setValue("Fix (image size)");
    }

    @FXML
    public void submitAndDownload() {
        if (Checking.checkImageAndOutputOnNull(properties)) {
            return;
        }

        PDDocument documentToSave = currentDoc;
        if (isNull(documentToSave)) {
            showErrorMessage(labelSuccess, progressBar,
                    "Could not create a PDF from the selected image.",
                    properties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            return;
        }

        cancelFlag.set(false);

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);
                if (isCancelled() || cancelFlag.get()) {
                    throw new InterruptedException("Conversion cancelled");
                }

                File outputFile = getOutputFilePdf("converted");

                try {
                    updateProgress(50, 100);
                    if (isCancelled() || cancelFlag.get()) {
                        throw new InterruptedException("Conversion cancelled");
                    }

                    documentToSave.save(outputFile);

                    if (isCancelled() || cancelFlag.get()) {
                        throw new InterruptedException("Conversion cancelled");
                    }
                    updateProgress(100, 100);
                    return outputFile;
                } catch (Exception exception) {
                    FileUtils.cleanupFile(outputFile);
                    throw exception;
                }
            }
        };

        executeMediaTask(task);
        labelSuccess.setManaged(true);
    }

    @Override
    protected void loadFile(File selectedFile) {
        if (!validateSelectedFile(selectedFile)) {
            return;
        }

        enableControls();

        try {
            currentDoc = PdfHelper.closeDocument(currentDoc);
        } catch (IOException e) {
            ErrorLogger.error("Error closing previous document: " + e.getMessage());
        }

        properties.setImage(selectedFile);
        properties.setTypeImage(ImageFormatDetector.determineFormat(selectedFile).orElse(null));
        labelSelectFileName.setText("Select image: " + selectedFile.getName());

        markDropZoneLoaded(dropZone, textDragZone, selectedFile.getName());
        bindingImageViewToPreviewContainer(imageViewPdf, previewContainer);

        String pageSize = comboPageSize.getValue();

        String marginVal =  comboMargin.getValue().toLowerCase();
        String orientationVal = comboOrientation.getValue().toLowerCase();
        String pageSizeVal = "fix";
        if (pageSize.contains("A4")) pageSizeVal = "a4";
        else if (pageSize.contains("US Letter")) pageSizeVal = "us letter";

        currentDoc = getDocumentFromImage(
                properties.getPathToImage(), marginVal, pageSizeVal, orientationVal
        ).orElse(null);
        if (nonNull(currentDoc)) {
            updatePreview();
        } else {
            labelPreviewPlaceholder.setText("Unable to create PDF preview");
            labelPreviewPlaceholder.setVisible(true);
            showErrorMessage(labelSuccess, progressBar,
                    "Could not create a PDF from the selected image.",
                    properties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        }
    }

    @Override
    protected void toggleAdditionalControls(boolean disabled) {
        setControlsDisabled(disabled, comboMargin, comboOrientation, comboPageSize);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (isNull(result) || Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info(getClass(), "Image to PDF conversion successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "PDF saved!", properties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    private void initComboBoxes() {
        comboMargin.getItems().addAll("No margin", "Small", "Big");
        comboMargin.setValue("No margin");
        comboMargin.setOnAction(_ -> updatePdfAndPreview());

        comboOrientation.getItems().addAll("Portrait", "Landscape");
        comboOrientation.setValue("Portrait");
        comboOrientation.setOnAction(_ -> updatePdfAndPreview());

        comboPageSize.getItems().addAll("A4 297x210 mm", "US Letter 215x279,4 mm", "Fix (image size)");
        comboPageSize.setValue("Fix (image size)");
        comboPageSize.setOnAction(_ -> updatePdfAndPreview());
    }

    private void updatePdfAndPreview() {
        if (nonNull(properties.getImage())) {
            loadFile(properties.getImage());
        }
    }
}
