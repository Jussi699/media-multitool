package controllers.pdfTools;

import controllers.abstractControllers.AbstractPdfToolController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import model.enums.PythonScript;
import model.helper.pdf.PdfHelper;
import model.logger.ErrorLogger;
import model.utility.context.ResetContext;
import visual.controls.Message;
import visual.dialogs.Alerts;
import visual.dialogs.InfoAlert;

import java.io.File;
import java.io.IOException;

import static java.util.Objects.isNull;
import static model.helper.pdf.PdfWordHelper.createConversionTask;
import static model.utility.io.PathWorker.getSavedPath;
import static visual.controls.Message.setupClearMessageTimer;

public class ConverterPdfToWordController extends AbstractPdfToolController {
    @FXML
    private void initialize() {
        properties.setOutput(getSavedPath());
        bindingImageViewToPreviewContainer(imageViewPdf, previewContainer);
        setupClearMessageTimer(labelSuccess, progressBar, properties.getHideSuccessMessageTimer(), true);
        setupDragAndDrop(dropZone, "pdf", this::loadFile);
        isPressedReset();
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "PDF to Word",
                "1. Select a PDF file using \"Select PDF\" or drag it into the drop zone;",
                """
                        2. Choose an output directory if needed;

                        3. Click "Convert and Download".

                        Text is extracted from every PDF page and saved as a DOCX document. Scanned pages without selectable text cannot be converted to editable text.
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        cancelProcessing();
        closeCurrentDoc();
        ResetContext context = new ResetContext(
                labelSelectFileName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPdf, progressBar, true, "PDF"
        );
        reset(properties, context, "Selected PDF file: none");
        disableControls();
    }

    @FXML
    public void submitAndDownload() {
        if (isNull(properties.getImage())) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "No PDF selected",
                    "Please select a PDF file to convert to Word.");
            return;
        }

        if (isNull(properties.getOutput())) {
            properties.setOutput(getSavedPath());
        }
        cancelFlag.set(false);
        executeMediaTask(createConversionTask(
                PythonScript.PDF_TO_WORD,
                properties.getImage(),
                properties.getOutput(),
                "docx"));

        labelSuccess.setManaged(true);
    }

    @Override
    protected void loadFile(File file) {
        if (!validateSelectedFile(file)) {
            return;
        }
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            Message.showErrorMessage(labelSuccess, "Please select a PDF file.", properties.getHideSuccessMessageTimer());
            return;
        }

        closeCurrentDoc();
        try {
            currentDoc = PdfHelper.loadUnencryptedPdf(file);
            properties.setImage(file);
            labelSelectFileName.setText("Selected PDF: " + file.getName());
            markDropZoneLoaded(dropZone, textDragZone, file.getName());
            bindingImageViewToPreviewContainer(imageViewPdf, previewContainer);
            updatePreview();
            enableControls();
        } catch (IOException exception) {
            properties.setImage(null);
            if (PdfHelper.isEncryptedException(exception)) {
                ErrorLogger.error(getClass(), "Encrypted PDF rejected: " + exception.getMessage());
                handleEncryptedPdf(exception);
            } else {
                handlePdfLoadError(exception);
            }
        }
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (isNull(result) || Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info(getClass(), "PDF to Word conversion successful: " + outputFile.getAbsolutePath());
        Platform.runLater(() -> {
            Message.showSuccessText(labelSuccess, "Word document saved!", properties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }
}
