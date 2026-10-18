package media_multitool.pdf;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import media_multitool.AbstractPdfToolController;
import model.checks.Checking;
import model.enums.CompressionLevel;
import model.helper.pdf.CompressPdfHelper;
import model.helper.pdf.PdfHelper;
import model.logger.ErrorLogger;
import model.utility.*;
import model.utility.Utility;
import viewHelp.InfoAlert;

import java.io.File;
import java.io.IOException;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.PathWorker.*;
import static viewHelp.Message.*;
import static model.utility.Utility.formatFileSize;

public class CompressPdfController extends AbstractPdfToolController {
    @FXML private ToggleButton btnLowCompression, btnMediumCompression, btnHighCompression;
    @FXML private Label labelEstimatedSize;

    @FXML
    public void initialize() {
        properties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, properties.getHideSuccessMessageTimer(), true);
        setupDragAndDrop(dropZone, "pdf", this::loadFile);

        isPressedReset();
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "Compress PDF",
                "1. Select a PDF file using \"Select PDF\" or drag and drop it into the dash-bordered zone;",
                """
                        3. Select the desired compression level ("Low", "Medium", or "High");

                        4. Click "Compress and Download".
                        
                        You can cancel the compression at any time using the "Cancel Compression" button.
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        cancelProcessing();

        ResetContext ctx = new ResetContext(
                labelSelectFileName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPdf, progressBar, true, "PDF"
        );
        reset(properties, ctx, "Selected PDF file: none");

        disableControls();
        closeCurrentDoc();

        if (nonNull(imageViewPdf)) {
            imageViewPdf.setImage(null);
        }

        labelPreviewPlaceholder.setVisible(true);
        labelEstimatedSize.setText("Estimated size: Waiting load PDF");
        btnLowCompression.setSelected(true);
    }

    @FXML
    private void submitAndDownload() {
        if (Checking.checkImageAndOutputOnNull(properties)) {
            return;
        }

        cancelFlag.set(false);

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(0, 100);
                if (isCancelled() || cancelFlag.get()) {
                    throw new InterruptedException("Compression cancelled");
                }

                File outputFile = getOutputFilePdf("compressed");

                if (isCancelled() || cancelFlag.get()) {
                    Utility.cleanupFile(outputFile);
                    throw new InterruptedException("Compression cancelled");
                }

                try {
                    CompressPdfHelper.compressPdf(
                            properties.getImage(),
                            outputFile,
                            getSelectedCompressionLevel(),
                            this::updateProgress
                    );
                } catch (Exception e) {
                    Utility.cleanupFile(outputFile);
                    throw e;
                }

                if (isCancelled() || cancelFlag.get()) {
                    Utility.cleanupFile(outputFile);
                    throw new InterruptedException("Compression cancelled");
                }

                updateProgress(100, 100);

                return outputFile;
            }
        };

        executeMediaTask(task);
        labelSuccess.setManaged(true);
    }

    @FXML
    public void onCompressionLevelChanged() {
        updateEstimatedSize();
    }

    @Override
    protected void loadFile(File selectedFile) {
        if (!validateSelectedFile(selectedFile)) {
            return;
        }

        enableControls();
        hideSuccessMessage(labelSuccess, progressBar, properties.getHideSuccessMessageTimer(), true);

        boolean encrypted = false;
        try {
            currentDoc = PdfHelper.closeDocument(currentDoc);
            currentDoc = PdfHelper.loadUnencryptedPdf(selectedFile);
        } catch (IOException e) {
            if (PdfHelper.isEncryptedException(e)) {
                handleEncryptedPdf(e);
                encrypted = true;
            } else {
                handlePdfLoadError(e);
                labelPreviewPlaceholder.setText("Error loading PDF");
                labelPreviewPlaceholder.setVisible(true);
                imageViewPdf.setImage(null);
                disableControls();
                if (nonNull(btnReset)) {
                    btnReset.setDisable(false);
                }
            }
        }

        properties.setImage(selectedFile);
        properties.setTypeImage(DetermineType.determineFormat(selectedFile).orElse(null));
        labelSelectFileName.setText("Select PDF: " + selectedFile.getName());

        markDropZoneLoaded(dropZone, textDragZone, selectedFile.getName());
        bindingImageViewToPreviewContainer(imageViewPdf, previewContainer);

        if (nonNull(currentDoc) && !encrypted) {
            updatePreview();
            updateEstimatedSize();
        } else if (encrypted) {
            labelEstimatedSize.setText("Estimated size: Encrypted PDF");
        }
    }

    @Override
    protected void lockUI() {
        toggleUI(true);
    }

    @Override
    protected void unlockUI() {
        toggleUI(false);
        cancelFlag.set(false);
    }

    @Override
    protected void disableControls() {
        toggleControls(true);
    }

    @Override
    protected void enableControls() {
        toggleControls(false);
    }

    @Override
    protected void toggleUI(boolean disabled) {
        toggleControls(disabled);
        setControlsDisabled(disabled, btnSelectFile, btnChoiceDirForSave);
        dropZone.setDisable(disabled);
        btnCancel.setDisable(!disabled);
    }

    @Override
    protected void toggleControls(boolean disabled) {
        setControlsDisabled(disabled, btnLowCompression, btnMediumCompression, btnHighCompression, btnSubmit, btnReset);
        btnCancel.setDisable(true);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (isNull(result) || Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info(getClass(), "PDF compression successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            labelEstimatedSize.setText("Output size: " + formatFileSize(outputFile.length()));
            showSuccessText(labelSuccess, "PDF saved!", properties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    private void updateEstimatedSize() {
        if (isNull(properties.getImage())) {
            labelEstimatedSize.setText("Estimated size: Waiting load PDF");
            return;
        }

        labelEstimatedSize.setText("Estimated size: depends on PDF content");
    }

    private CompressionLevel getSelectedCompressionLevel() {
        if (btnHighCompression.isSelected())   return CompressionLevel.HIGH;
        if (btnMediumCompression.isSelected()) return CompressionLevel.MEDIUM;
        return CompressionLevel.LOW;
    }
}
