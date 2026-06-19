package media_multitool.pdf;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import media_multitool.AbstractMediaController;
import model.helper.pdf.ProtectPdfHelper;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.ResetContext;
import model.utility.Util;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import viewHelp.Alerts;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static model.utility.Util.getSavedPath;
import static viewHelp.Message.*;

public class ProtectPdfController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML private PasswordField typePasswordField, repeatPasswordField;
    @FXML private TextField typePasswordTextField, repeatPasswordTextField;
    @FXML private ImageView imageViewPdf;
    @FXML private ProgressBar progressBar;
    @FXML private StackPane dropZone, previewContainer;
    @FXML private Button btnSelectFile, btnChoiceDirForSaveFile, btnSubmit;
    @FXML private ToggleButton btnShowTypePassword, btnShowRepeatPassword;
    @FXML private Label labelSelectFileName, textDragZone, labelPreviewPlaceholder, labelPasswordMatch;

    private PDDocument currentDoc;
    private List<Control> listControls;

    private String typePassword;
    private String repeatPassword;

    @FXML
    public void initialize() {
        listControls = List.of(btnSubmit);

        imageProperties.setOutput(getSavedPath());

        if (progressBar != null) {
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            progressBar.setProgress(0);
        }

        setupListener();

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);

        setupDragAndDrop(dropZone, List.of(".pdf"), this::loadFile);

        isPressedReset();
    }

    private void setupListener() {
        typePasswordField.textProperty().addListener((_, _, newValue) -> {
            typePassword = newValue;
            if (typePasswordTextField != null) {
                typePasswordTextField.setText(newValue);
            }
            checkPasswordMatch();
        });

        repeatPasswordField.textProperty().addListener((_, _, newValue) -> {
            repeatPassword = newValue;
            if (repeatPasswordTextField != null) {
                repeatPasswordTextField.setText(newValue);
            }
            checkPasswordMatch();
        });

        if (typePasswordTextField != null) {
            typePasswordTextField.textProperty().addListener((_, _, newValue) -> {
                typePassword = newValue;
                typePasswordField.setText(newValue);
                checkPasswordMatch();
            });
        }

        if (repeatPasswordTextField != null) {
            repeatPasswordTextField.textProperty().addListener((_, _, newValue) -> {
                repeatPassword = newValue;
                repeatPasswordField.setText(newValue);
                checkPasswordMatch();
            });
        }
    }

    private void checkPasswordMatch() {
        if (typePassword != null && !typePassword.isEmpty() && 
            repeatPassword != null && !repeatPassword.isEmpty()) {
            if (typePassword.equals(repeatPassword)) {
                labelPasswordMatch.setText("✓");
                labelPasswordMatch.setStyle("-fx-text-fill: #32CD32; -fx-font-size: 16px;");
                labelPasswordMatch.setVisible(true);
            } else {
                labelPasswordMatch.setText("✗");
                labelPasswordMatch.setStyle("-fx-text-fill: #ef2b2b; -fx-font-size: 16px;");
                labelPasswordMatch.setVisible(true);
            }
        } else {
            labelPasswordMatch.setVisible(false);
        }
    }

    @Override
    protected void lockUI() {
        btnSelectFile.setDisable(true);
        btnChoiceDirForSaveFile.setDisable(true);
        btnReset.setDisable(true);
        btnSubmit.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSelectFile.setDisable(false);
        btnChoiceDirForSaveFile.setDisable(false);
        btnReset.setDisable(false);
        btnSubmit.setDisable(false);
    }

    @Override
    protected void disableControls() {
        listControls.forEach(c -> c.setDisable(true));
        typePasswordField.setDisable(true);
        repeatPasswordField.setDisable(true);
        if (typePasswordTextField != null) {
            typePasswordTextField.setDisable(true);
        }
        if (repeatPasswordTextField != null) {
            repeatPasswordTextField.setDisable(true);
        }
        btnShowTypePassword.setDisable(true);
        btnShowRepeatPassword.setDisable(true);
    }

    @Override
    protected void enableControls() {
        listControls.forEach(c -> c.setDisable(false));
        typePasswordField.setDisable(false);
        repeatPasswordField.setDisable(false);
        if (typePasswordTextField != null) {
            typePasswordTextField.setDisable(false);
        }
        if (repeatPasswordTextField != null) {
            repeatPasswordTextField.setDisable(false);
        }
        btnShowTypePassword.setDisable(false);
        btnShowRepeatPassword.setDisable(false);
    }

    @FXML
    private void turnShowPassword(ActionEvent actionEvent) {
        ToggleButton source = (ToggleButton) actionEvent.getSource();

        switch (source.getId()) {
            case "btnShowTypePassword" -> {
                if (source.isSelected()) {
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
            case "btnShowRepeatPassword" -> {
                if (source.isSelected()) {
                    repeatPasswordTextField.setText(repeatPassword);
                    repeatPasswordTextField.setVisible(true);
                    repeatPasswordTextField.setManaged(true);
                    repeatPasswordField.setVisible(false);
                    repeatPasswordField.setManaged(false);
                } else {
                    repeatPasswordField.setText(repeatPassword);
                    repeatPasswordField.setVisible(true);
                    repeatPasswordField.setManaged(true);
                    repeatPasswordTextField.setVisible(false);
                    repeatPasswordTextField.setManaged(false);
                }
            }
        }
    }

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectPdfFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectPdfFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                "Choice PDF file"
        ).ifPresent(this::loadFile);
    }

    @FXML
    public void onActionChoiceDirForSaveFile() {
        selectOutputDirectory(btnChoiceDirForSaveFile, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save images");
    }

    private boolean checks() {
        if (imageProperties.getImage() == null) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Please select a PDF file", imageProperties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
            return false;
        }

        if (imageProperties.getOutput() == null) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Please select output directory", imageProperties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
            return false;
        }

        if (typePassword == null || typePassword.isEmpty()) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Please enter password", imageProperties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
            return false;
        }

        if (repeatPassword == null || repeatPassword.isEmpty()) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Please repeat password", imageProperties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
            return false;
        }

        if (!typePassword.equals(repeatPassword)) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Passwords do not match!", imageProperties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
            return false;
        }

        return true;
    }

    @FXML
    public void submitAndDownload() {
        if(!checks()) {
            return;
        }

        executeMediaTask(taskProtectPdf());
        labelSuccess.setManaged(true);
    }

    private Task<File> taskProtectPdf() {
        return new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);

                if (imageProperties.getImage() == null) {
                    throw new IOException("PDF file not selected");
                }

                updateProgress(30, 100);

                String baseName = imageProperties.getImage().getName().replaceFirst("[.][^.]+$", "");
                File outputFile = new File(imageProperties.getOutput(), baseName + "_protected_" + UUID.randomUUID().toString().substring(0, 8)  + ".pdf");

                updateProgress(50, 100);

                File result = ProtectPdfHelper.protectPdf(
                        imageProperties.getImage(),
                        outputFile,
                        typePassword,
                        typePassword
                );

                updateProgress(100, 100);

                return result;
            }
        };
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
        
        ErrorLogger.info("PDF protection successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "PDF protected successfully!", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            progressBar.setProgress(0);
        });
    }

    @Override
    protected void handleTaskFailure(Throwable exception) {
        super.handleTaskFailure(exception);
        Platform.runLater(() -> {
            showErrorMessage(labelSuccess, progressBar,"Error: " + exception.getMessage(), imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            progressBar.setProgress(0);
        });
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFileName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPdf, progressBar, true
        );
        Util.reset(imageProperties, ctx, "Selected PDF file: none");

        disableControls();

        closeCurrentDoc();
        if (imageViewPdf != null) {
            imageViewPdf.setImage(null);
        }

        labelPreviewPlaceholder.setVisible(true);

        typePassword = "";
        repeatPassword = "";
        typePasswordField.setText("");
        repeatPasswordField.setText("");
        if (typePasswordTextField != null) {
            typePasswordTextField.setText("");
        }
        if (repeatPasswordTextField != null) {
            repeatPasswordTextField.setText("");
        }
        
        labelPasswordMatch.setVisible(false);
        
        if (btnShowTypePassword.isSelected()) {
            btnShowTypePassword.setSelected(false);
            typePasswordField.setVisible(true);
            typePasswordField.setManaged(true);
            typePasswordTextField.setVisible(false);
            typePasswordTextField.setManaged(false);
        }
        
        if (btnShowRepeatPassword.isSelected()) {
            btnShowRepeatPassword.setSelected(false);
            repeatPasswordField.setVisible(true);
            repeatPasswordField.setManaged(true);
            repeatPasswordTextField.setVisible(false);
            repeatPasswordTextField.setManaged(false);
        }
    }

    private void closeCurrentDoc() {
        if (currentDoc != null) {
            try {
                currentDoc.close();
            } catch (IOException e) {
                ErrorLogger.error("Error closing PDF document: " + e.getMessage());
            }
            currentDoc = null;
        }
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Protect PDF",
                """
                        How to use:
                        1. Select a PDF file using 'Select PDF file' or drag and drop.
                        2. Enter a password and repeat it to confirm.
                        3. Click 'Convert and Download' to create a password-protected PDF.
                        
                        The protected PDF will be saved with '_protected' suffix.
                        
                        Security settings:
                        - 256-bit AES encryption
                        - Printing allowed
                        - Editing, copying, and form filling disabled
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    private void loadFile(File selectedFile) {
        closeCurrentDoc();
        imageProperties.setImage(selectedFile);
        labelSelectFileName.setText("Selected PDF: " + selectedFile.getName());
        textDragZone.setText("Selected: " + selectedFile.getName());

        if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        Util.bindingImageViewToPreviewContainer(imageViewPdf, previewContainer);

        try {
            currentDoc = org.apache.pdfbox.Loader.loadPDF(selectedFile);
            updatePreview();
            
            enableControls();

        } catch (IOException e) {
            ErrorLogger.error("Error loading PDF: " + e.getMessage());
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Error loading PDF: " + e.getMessage(), imageProperties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
        }
    }
}
