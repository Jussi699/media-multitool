package controllers.pdfTools;

import controllers.abstractControllers.AbstractPdfToolController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import model.enums.PythonScript;
import model.logger.ErrorLogger;
import model.utility.context.ResetContext;
import visual.controls.Message;
import visual.dialogs.Alerts;
import visual.dialogs.InfoAlert;

import java.io.File;

import static java.util.Objects.isNull;
import static model.helper.pdf.PdfWordHelper.createConversionTask;
import static model.utility.io.PathWorker.getSavedPath;
import static visual.controls.Message.setupClearMessageTimer;

public class ConverterWordToPdfController extends AbstractPdfToolController {
    @FXML
    private void initialize() {
        properties.setOutput(getSavedPath());
        setupClearMessageTimer(labelSuccess, progressBar, properties.getHideSuccessMessageTimer(), true);
        setupDragAndDrop(dropZone, "docx", this::loadFile);
        isPressedReset();
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "Word to PDF",
                "1. Select a DOCX document using \"Select Word\" or drag it into the drop zone;",
                """
                        2. Choose an output directory if needed;

                        3. Click "Convert and Download".

                        Paragraph text and table contents are included in the PDF. DOC files and embedded images are not supported.
                        """
        );
    }

    @FXML
    private void selectWordFile() {
        selectInputFile(
                btnSelectFile,
                new FileChooser.ExtensionFilter("Word documents", "*.docx"),
                this::loadFile
        );
    }

    @FXML
    public void isPressedReset() {
        cancelProcessing();
        ResetContext context = new ResetContext(
                labelSelectFileName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPdf, progressBar, true, "Word document"
        );
        reset(properties, context, "Selected Word file: none");
        disableControls();
    }

    @FXML
    public void submitAndDownload() {
        if (isNull(properties.getImage())) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "No Word document selected",
                    "Please select a DOCX document to convert to PDF.");
            return;
        }

        if (isNull(properties.getOutput())) {
            properties.setOutput(getSavedPath());
        }
        cancelFlag.set(false);

        executeMediaTask(createConversionTask(
                PythonScript.WORD_TO_PDF,
                properties.getImage(),
                properties.getOutput(),
                "pdf"));

        labelSuccess.setManaged(true);
    }

    @Override
    protected void loadFile(File file) {
        if (!validateSelectedFile(file)) {
            return;
        }
        if (!file.getName().toLowerCase().endsWith(".docx")) {
            Message.showErrorMessage(labelSuccess, "Only DOCX Word documents are supported.",
                    properties.getHideSuccessMessageTimer());
            return;
        }

        properties.setImage(file);
        labelSelectFileName.setText("Selected Word document: " + file.getName());
        markDropZoneLoaded(dropZone, textDragZone, file.getName());
        enableControls();
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (isNull(result) || Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info(getClass(), "Word to PDF conversion successful: " + outputFile.getAbsolutePath());
        Platform.runLater(() -> {
            Message.showSuccessText(labelSuccess, "PDF saved!", properties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }
}
