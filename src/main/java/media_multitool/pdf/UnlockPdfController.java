package media_multitool.pdf;

import app.Launcher;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import media_multitool.AbstractMediaController;
import model.helper.pdf.PdfHelper;
import model.helper.pdf.UnlockPdfHelper;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.ResetContext;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import viewHelp.Alerts;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class UnlockPdfController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML private PasswordField typePasswordField;
    @FXML private TextField typePasswordTextField;
    @FXML private ImageView imageViewPdf;
    @FXML private ProgressBar progressBar;
    @FXML private StackPane dropZone, previewContainer;
    @FXML private Button btnSelectFile, btnChoiceDirForSaveFile, btnSubmit;
    @FXML private ToggleButton btnShowTypePassword;
    @FXML private Label labelSelectFileName, textDragZone, labelPreviewPlaceholder, labelFileEncrypted;

    private PDDocument currentDoc;
    private List<Control> listControls;

    private String typePassword;
    private boolean isFileEncrypted = false;

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

        setupDragAndDrop(dropZone, List.of("pdf"), this::loadFile);

        isPressedReset();
    }

    private void setupListener() {
        typePasswordField.textProperty().addListener((_, _, newValue) -> {
            typePassword = newValue;
            if (typePasswordTextField != null) {
                typePasswordTextField.setText(newValue);
            }
        });


        if (typePasswordTextField != null) {
            typePasswordTextField.textProperty().addListener((_, _, newValue) -> {
                typePassword = newValue;
                typePasswordField.setText(newValue);
            });
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

        if (typePasswordTextField != null) {
            typePasswordTextField.setDisable(true);
        }

        btnShowTypePassword.setDisable(true);
    }

    @Override
    protected void enableControls() {
        listControls.forEach(c -> c.setDisable(false));
        typePasswordField.setDisable(false);
        if (typePasswordTextField != null) {
            typePasswordTextField.setDisable(false);
        }

        btnShowTypePassword.setDisable(false);
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
    public void onActionBtnSelectFile() {
        SelectFile selectPdfFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectPdfFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                "Select PDF"
        ).ifPresent(this::loadFile);
    }

    @FXML
    public void onActionChoiceDirForSaveFile() {
        selectOutputDirectory(btnChoiceDirForSaveFile, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save PDF");
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

        if (isFileEncrypted && (typePassword == null || typePassword.isEmpty())) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Please enter password", imageProperties.getHideSuccessMessageTimer());
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

        executeMediaTask(taskUnlockPdf());
        labelSuccess.setManaged(true);
    }

    private Task<File> taskUnlockPdf() {
        return new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);

                if (imageProperties.getImage() == null) {
                    throw new IOException("PDF file not selected");
                }

                updateProgress(30, 100);

                String baseName = imageProperties.getImage().getName().replaceFirst("[.][^.]+$", "");
                String shortId = UUID.randomUUID().toString().substring(0, 8);
                File outputFile = new File(imageProperties.getOutput(), baseName + "_unlocked_" + shortId + ".pdf");

                updateProgress(50, 100);

                File result;
                if (isFileEncrypted) {
                    result = UnlockPdfHelper.unlockPdf(
                            imageProperties.getImage(),
                            outputFile,
                            typePassword
                    );
                } else {
                    java.nio.file.Files.copy(
                            imageProperties.getImage().toPath(),
                            outputFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );
                    result = outputFile;
                }

                updateProgress(100, 100);

                return result;
            }
        };
    }

    private void updatePreview(boolean isEncrypted) {
        if(isEncrypted) {
            Image image = new Image(Objects.requireNonNull(Launcher.class.getResourceAsStream("/img/encrypted.webp")));
            imageViewPdf.setImage(image);
            labelPreviewPlaceholder.setVisible(false);
        }
        else if(currentDoc != null) {
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
        
        ErrorLogger.info("PDF unlocked successfully! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            String message = isFileEncrypted ? "PDF unlocked successfully!" : "PDF copied successfully!";
            showSuccessText(labelSuccess, message, imageProperties.getHideSuccessMessageTimer());
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

        typePassword = "";
        typePasswordField.setText("");
        if (typePasswordTextField != null) {
            typePasswordTextField.setText("");
        }

        isFileEncrypted = false;

        if (btnShowTypePassword.isSelected()) {
            btnShowTypePassword.setSelected(false);
            typePasswordField.setVisible(true);
            typePasswordField.setManaged(true);
            typePasswordTextField.setVisible(false);
            typePasswordTextField.setManaged(false);
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
                        2. If the PDF file is password protected, enter the password.
                        3. If the file is not password protected, you can simply proceed to the next step.
                        4. Click 'Unlock and Download' to unlock your PDF.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    private void loadFile(File selectedFile) {
        if (imageViewPdf != null) {
            imageViewPdf.setImage(null);
        }

        try {
            currentDoc = PdfHelper.closeDocument(currentDoc);
        } catch (IOException e) {
            ErrorLogger.error("Error closing previous document: " + e.getMessage());
        }

        imageProperties.setImage(selectedFile);
        labelSelectFileName.setText("Selected PDF: " + selectedFile.getName());
        textDragZone.setText("Selected: " + selectedFile.getName());

        if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        bindingImageViewToPreviewContainer(imageViewPdf, previewContainer);

        try {
            currentDoc = Loader.loadPDF(selectedFile);

            if(currentDoc.isEncrypted()){
                isFileEncrypted = true;
                labelFileEncrypted.setText("PDF file is encrypted!");
                updatePreview(true);
                typePasswordTextField.setDisable(false);
                typePasswordField.setDisable(false);
                btnShowTypePassword.setDisable(false);
                btnSubmit.setDisable(false);
            }
            else {
                isFileEncrypted = false;
                labelFileEncrypted.setText("PDF file is not encrypted");
                typePasswordTextField.setDisable(true);
                typePasswordField.setDisable(true);
                btnShowTypePassword.setDisable(true);
                btnSubmit.setDisable(false);
                updatePreview(false);
            }

        } catch (IOException e) {
            String errorMsg = e.getMessage().toLowerCase();
            if (errorMsg.contains("password") || errorMsg.contains("encrypted") || errorMsg.contains("owner") || errorMsg.contains("user")) {
                isFileEncrypted = true;
                labelFileEncrypted.setText("PDF file is encrypted! Please enter password.");
                labelFileEncrypted.setVisible(true);
                updatePreview(true);
                typePasswordTextField.setDisable(false);
                typePasswordField.setDisable(false);
                btnShowTypePassword.setDisable(false);
                btnSubmit.setDisable(false);
                ErrorLogger.info("Encrypted PDF detected: " + selectedFile.getName());
            } else {
                ErrorLogger.error("Error loading PDF: " + e.getMessage());
                Platform.runLater(() -> {
                    showErrorMessage(labelSuccess, progressBar,"Error loading PDF: " + e.getMessage(), imageProperties.getHideSuccessMessageTimer());
                    labelSuccess.setManaged(true);
                });
            }
        }
    }
}
