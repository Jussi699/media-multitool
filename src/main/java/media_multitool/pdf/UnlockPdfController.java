package media_multitool.pdf;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import media_multitool.AbstractPdfToolController;
import model.helper.pdf.PdfHelper;
import model.helper.pdf.UnlockPdfHelper;
import model.logger.ErrorLogger;
import model.utility.ResetContext;
import org.apache.pdfbox.Loader;
import visual.InfoAlert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.helper.pdf.PdfHelper.notNullInputAndOutput;
import static model.utility.PathWorker.getSavedPath;
import static visual.Message.*;

public class UnlockPdfController extends AbstractPdfToolController {
    @FXML private ToggleButton btnShowTypePassword;
    @FXML private TextField typePasswordTextField;
    @FXML private Label labelFileEncrypted;
    @FXML private PasswordField typePasswordField;

    private String typePassword;
    private boolean isFileEncrypted = false;

    @FXML
    public void initialize() {
        properties.setOutput(getSavedPath());

        progressBar.setVisible(true);
        progressBar.setManaged(true);
        progressBar.setProgress(0);

        setupListener();
        setupClearMessageTimer(labelSuccess, progressBar, properties.getHideSuccessMessageTimer(), true);
        setupDragAndDrop(dropZone, "pdf", this::loadFile);

        isPressedReset();
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "Unlock PDF",
                "Select a PDF file using \"Select PDF\" or drag and drop it into the dash-bordered zone;",
                """
                        If the PDF file is password protected, enter the password (if the file is not password protected, you can simply proceed to the next step);

                        4. Click "Unlock and Download".
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFileName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPdf, progressBar, true, "PDF"
        );
        reset(properties, ctx, "Selected PDF file: none");

        disableControls();

        try {
            currentDoc = PdfHelper.closeDocument(currentDoc);
        } catch (IOException e) {
            ErrorLogger.error("Error closing document during reset: " + e.getMessage());
        }

        imageViewPdf.setImage(null);

        labelPreviewPlaceholder.setVisible(true);
        clearField();
        isFileEncrypted = false;
        turnField();
    }

    @FXML
    private void turnShowPassword() {
        if (btnShowTypePassword.isSelected()) {
            typePasswordTextField.setText(typePassword);
            typePasswordTextField.setVisible(true);
            typePasswordTextField.setManaged(true);
            typePasswordField.setVisible(false);
            typePasswordField.setManaged(false);
        } else {
            typePasswordField.setText(typePassword);
            typePasswordField.setVisible(true);
            typePasswordField.setManaged(true);
            typePasswordTextField.setVisible(false);
            typePasswordTextField.setManaged(false);
        }
    }

    @FXML
    public void submitAndDownload() {
        if(!checks()) {
            return;
        }

        executeMediaTask(createTaskUnlockPdf());
        labelSuccess.setManaged(true);
    }

    @Override
    protected void toggleAdditionalControls(boolean disabled) {
        boolean passwordControlsDisabled = disabled || !isFileEncrypted;
        setControlsDisabled(passwordControlsDisabled,
                typePasswordField, typePasswordTextField, btnShowTypePassword);
    }

    @Override
    protected void loadFile(File file) {
        if (!validateSelectedFile(file)) {
            return;
        }

        preparePreview();
        updateSelectedFile(file);

        try {
            currentDoc = Loader.loadPDF(file);
            updateDocumentPreview();
        } catch (IOException e) {
            handleDocumentLoadError(file, e);
        }
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;

        ErrorLogger.info(getClass(), "PDF unlocked successfully! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            String message = isFileEncrypted ? "PDF unlocked successfully!" : "PDF copied successfully!";
            showSuccessText(labelSuccess, message, properties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            progressBar.setProgress(0);
        });
    }

    private boolean checks() {
        if(notNullInputAndOutput(properties, labelSuccess, progressBar)) {
            return false;
        }

        if (isFileEncrypted && (isNull(typePassword) || typePassword.isEmpty())) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Please enter password", properties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
            return false;
        }

        return true;
    }

    private Task<File> createTaskUnlockPdf() {
        return new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);

                if (isNull(properties.getImage())) {
                    throw new IOException("PDF file not selected");
                }

                updateProgress(30, 100);

                updateProgress(50, 100);

                File result;
                File outputFile = getOutputFilePdf("unlocked");
                if (isFileEncrypted) {
                    result = UnlockPdfHelper.unlockPdf(
                            properties.getImage(),
                            outputFile,
                            typePassword
                    );
                } else {
                    Files.copy(
                            properties.getImage().toPath(),
                            outputFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                    result = outputFile;
                }

                updateProgress(100, 100);

                return result;
            }
        };
    }

    private void turnField() {
        if (btnShowTypePassword.isSelected()) {
            btnShowTypePassword.setSelected(false);
            typePasswordField.setVisible(true);
            typePasswordField.setManaged(true);
            typePasswordTextField.setVisible(false);
            typePasswordTextField.setManaged(false);
        }
    }

    private void clearField() {
        typePassword = "";
        typePasswordField.setText("");
        typePasswordTextField.setText("");
    }

    private void setupListener() {
        typePasswordField.textProperty().addListener((_, _, newValue) -> {
            typePassword = newValue;
            if (nonNull(typePasswordTextField)) {
                typePasswordTextField.setText(newValue);
            }
        });


        if (nonNull(typePasswordTextField)) {
            typePasswordTextField.textProperty().addListener((_, _, newValue) -> {
                typePassword = newValue;
                typePasswordField.setText(newValue);
            });
        }
    }

    private void preparePreview() {
        imageViewPdf.setImage(null);

        try {
            currentDoc = PdfHelper.closeDocument(currentDoc);
        } catch (IOException e) {
            ErrorLogger.error("Error closing previous document: " + e.getMessage());
        }
    }

    private void updateSelectedFile(File file) {
        properties.setImage(file);
        labelSelectFileName.setText("Selected PDF: " + file.getName());
        markDropZoneLoaded(dropZone, textDragZone, file.getName());
        bindingImageViewToPreviewContainer(imageViewPdf, previewContainer);
    }

    private void updateDocumentPreview() {
        if (PdfHelper.isEncryptedDocument(currentDoc)) {
            showEncryptedDocument("PDF file is encrypted!", false);
            return;
        }

        isFileEncrypted = false;
        labelFileEncrypted.setText("PDF file is not encrypted");
        typePasswordTextField.setDisable(true);
        typePasswordField.setDisable(true);
        btnShowTypePassword.setDisable(true);
        enableControls();
        updatePreview();
    }

    private void showEncryptedDocument(String status, boolean showStatus) {
        isFileEncrypted = true;
        labelFileEncrypted.setText(status);
        if (showStatus) {
            labelFileEncrypted.setVisible(true);
        }
        setProtectImage();
        typePasswordTextField.setDisable(false);
        typePasswordField.setDisable(false);
        btnShowTypePassword.setDisable(false);
        enableControls();
    }

    private void handleDocumentLoadError(File file, IOException exception) {
        if (!PdfHelper.isEncryptedException(exception)) {
            handlePdfLoadError(exception);
            return;
        }

        showEncryptedDocument("PDF file is encrypted! Please enter password.", true);
        ErrorLogger.info(getClass(), "Encrypted PDF detected: " + file.getName());
    }
}
