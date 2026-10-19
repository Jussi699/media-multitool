package media_multitool.pdf;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Modality;
import media_multitool.AbstractPdfToolController;
import model.helper.pdf.PdfHelper;
import model.helper.pdf.ProtectPdfHelper;
import model.logger.ErrorLogger;
import model.utility.ResetContext;
import viewHelp.Alerts;
import viewHelp.InfoAlert;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.helper.pdf.PdfHelper.notNullInputAndOutput;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class ProtectPdfController extends AbstractPdfToolController {
    @FXML private ToggleButton btnShowTypePassword, btnShowRepeatPassword;
    @FXML private TextField typePasswordTextField, repeatPasswordTextField;
    @FXML private Label labelPasswordMatch;
    @FXML private PasswordField typePasswordField, repeatPasswordField;

    private String typePassword, repeatPassword;

    @FXML
    public void initialize() {
        properties.setOutput(getSavedPath());

        progressBar.setVisible(true);
        progressBar.setManaged(true);
        progressBar.setProgress(0);

        setupListenerField();

        setupClearMessageTimer(labelSuccess, progressBar, properties.getHideSuccessMessageTimer(), true);
        setupDragAndDrop(dropZone, "pdf", this::loadFile);

        isPressedReset();
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "Protect PDF",
                "1. Select a PDF file using \"Select PDF\" or drag and drop it into the dash-bordered zone;",
                """
                        3. Enter a password and repeat it in the fields to confirm;

                        4. Click "Protect and Download".
                       
                        Security settings:
                        - 256-bit AES encryption
                        - Printing allowed
                        - Editing, copying, and form filling disabled.
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

        cleanTextField();

        labelPreviewPlaceholder.setVisible(true);
        labelPasswordMatch.setVisible(false);

        btnShowTypePassword.setSelected(false);
        togglePasswordVisibility(false, typePasswordField, typePasswordTextField, typePassword);

        btnShowRepeatPassword.setSelected(false);
        togglePasswordVisibility(false, repeatPasswordField, repeatPasswordTextField, repeatPassword);
    }

    @FXML
    public void submitAndDownload() {
        if(!checks()) {
            return;
        }

        executeMediaTask(getTaskProtectPdf());
        labelSuccess.setManaged(true);
    }

    @FXML
    private void turnShowPassword(ActionEvent actionEvent) {
        ToggleButton source = (ToggleButton) actionEvent.getSource();

        switch (source.getId()) {
            case "btnShowTypePassword" ->
                    togglePasswordVisibility(source.isSelected(), typePasswordField, typePasswordTextField, typePassword);
            case "btnShowRepeatPassword" ->
                    togglePasswordVisibility(source.isSelected(), repeatPasswordField, repeatPasswordTextField, repeatPassword);
            default -> throw new IllegalArgumentException("Unexpected id: " + source.getId());
        }
    }

    @Override
    protected void toggleAdditionalControls(boolean disabled) {
        setControlsDisabled(disabled, typePasswordField, repeatPasswordField, btnShowTypePassword,
                btnShowRepeatPassword, typePasswordTextField, repeatPasswordTextField);
    }

    @Override
    protected void loadFile(File selectedFile) {
        if (!validateSelectedFile(selectedFile)) {
            return;
        }

        try {
            currentDoc = PdfHelper.closeDocument(currentDoc);
        } catch (IOException e) {
            ErrorLogger.error("Error closing previous document: " + e.getMessage());
        }

        properties.setImage(selectedFile);
        labelSelectFileName.setText("Selected PDF: " + selectedFile.getName());

        markDropZoneLoaded(dropZone, textDragZone, selectedFile.getName());
        bindingImageViewToPreviewContainer(imageViewPdf, previewContainer);

        try {
            currentDoc = PdfHelper.loadUnencryptedPdf(selectedFile);
            updatePreview();
            enableControls();

        } catch (IOException e) {
            final boolean encrypted = PdfHelper.isEncryptedException(e);
            if (encrypted) {
                setProtectImage();
                ErrorLogger.error("Encrypted PDF rejected: " + e.getMessage());
                disableControls();
                Platform.runLater(() -> {
                    Alerts.alertDialog(Alert.AlertType.ERROR,
                            "Error",
                            "Failed to load PDF",
                            e.getMessage(),
                            Modality.APPLICATION_MODAL);

                    showErrorMessage(labelSuccess,
                            progressBar,
                            PdfHelper.getEncryptedPdfMessage(),
                            properties.getHideSuccessMessageTimer());
                    labelSuccess.setManaged(true);
                });
            } else {
                handlePdfLoadError(e);
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

        ErrorLogger.info(getClass(), "PDF protection successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "PDF protected successfully!", properties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            progressBar.setProgress(0);
        });
    }

    private boolean checks() {
        if(notNullInputAndOutput(properties, labelSuccess, progressBar)) {
            return false;
        }

        if (isNull(typePassword) || typePassword.isEmpty()) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Please enter password", properties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
            return false;
        }

        if (isNull(repeatPassword) || repeatPassword.isEmpty()) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Please repeat password", properties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
            return false;
        }

        if (!typePassword.equals(repeatPassword)) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Passwords do not match!", properties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
            return false;
        }

        return true;
    }

    private Task<File> getTaskProtectPdf() {
        return new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);

                if (isNull(properties.getImage())) {
                    throw new IOException("PDF file not selected");
                }

                updateProgress(30, 100);

                String baseName = properties.getImage().getName().replaceFirst("[.][^.]+$", "");
                String shortId = UUID.randomUUID().toString().substring(0, 8);
                File outputFile = new File(properties.getOutput(), baseName + "_protected_" + shortId + ".pdf");

                updateProgress(50, 100);

                File result = ProtectPdfHelper.protectPdf(
                        properties.getImage(),
                        outputFile,
                        typePassword,
                        typePassword
                );

                updateProgress(100, 100);

                return result;
            }
        };
    }

    private void setupListenerField() {
        typePasswordField.textProperty().addListener((_, _, newValue) -> {
            typePassword = newValue;
            typePasswordTextField.setText(newValue);
            checkPasswordMatch();
        });

        repeatPasswordField.textProperty().addListener((_, _, newValue) -> {
            repeatPassword = newValue;
            repeatPasswordTextField.setText(newValue);
            checkPasswordMatch();
        });

        if (nonNull(typePasswordTextField)) {
            typePasswordTextField.textProperty().addListener((_, _, newValue) -> {
                typePassword = newValue;
                typePasswordField.setText(newValue);
                checkPasswordMatch();
            });
        }

        if (nonNull(repeatPasswordTextField)) {
            repeatPasswordTextField.textProperty().addListener((_, _, newValue) -> {
                repeatPassword = newValue;
                repeatPasswordField.setText(newValue);
                checkPasswordMatch();
            });
        }
    }

    private void checkPasswordMatch() {
        final String trueStyle  = "-fx-text-fill: #32CD32; -fx-font-size: 16px;";
        final String falseStyle = "-fx-text-fill: #ef2b2b; -fx-font-size: 16px;";

        final boolean hasPasswords = nonNull(typePassword) && !typePassword.isEmpty() &&
                nonNull(repeatPassword) && !repeatPassword.isEmpty();

        if (hasPasswords) {
            final boolean isMatch = typePassword.equals(repeatPassword);
            labelPasswordMatch.setText(isMatch ? "✓" : "✗");
            labelPasswordMatch.setStyle(isMatch ? trueStyle : falseStyle);
            labelPasswordMatch.setVisible(true);
        } else {
            labelPasswordMatch.setVisible(false);
        }
    }

    private void cleanTextField() {
        typePassword = "";
        repeatPassword = "";
        typePasswordField.setText("");
        repeatPasswordField.setText("");
        typePasswordTextField.setText("");
        repeatPasswordTextField.setText("");
    }

    private void togglePasswordVisibility(boolean show, PasswordField passwordField, TextField textField, String password) {
        TextInputControl visibleField = show ? textField : passwordField;
        TextInputControl hiddenField = show ? passwordField : textField;

        visibleField.setText(password);
        visibleField.setVisible(true);
        visibleField.setManaged(true);
        hiddenField.setVisible(false);
        hiddenField.setManaged(false);
    }
}
