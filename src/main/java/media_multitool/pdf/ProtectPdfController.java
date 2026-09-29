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
import model.helper.pdf.PdfHelper;
import model.helper.pdf.ProtectPdfHelper;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.ResetContext;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jspecify.annotations.NonNull;
import viewHelp.Alerts;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.PathWorker.getSavedPath;
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
    @FXML private StackPane dropZone, previewContainer;
    @FXML private Button btnSelectFile, btnChoiceDirForSaveFile, btnSubmit;
    @FXML private ToggleButton btnShowTypePassword, btnShowRepeatPassword;
    @FXML private Label labelSelectFileName, textDragZone, labelPreviewPlaceholder, labelPasswordMatch;

    private PDDocument currentDoc;
    private String typePassword, repeatPassword;
    private List<Control> listControls;

    @FXML
    public void initialize() {
        listControls = List.of(btnSubmit, btnReset);
        imageProperties.setOutput(getSavedPath());

        if (nonNull(progressBar)) {
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
            if (nonNull(typePasswordTextField)) {
                typePasswordTextField.setText(newValue);
            }
            checkPasswordMatch();
        });

        repeatPasswordField.textProperty().addListener((_, _, newValue) -> {
            repeatPassword = newValue;
            if (nonNull(repeatPasswordTextField)) {
                repeatPasswordTextField.setText(newValue);
            }
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
        if (nonNull(typePassword) && !typePassword.isEmpty() &&
            nonNull(repeatPassword) && !repeatPassword.isEmpty()) {
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
        listControls.forEach(c -> c.setDisable(true));
    }

    @Override
    protected void unlockUI() {
        btnSelectFile.setDisable(false);
        btnChoiceDirForSaveFile.setDisable(false);
        listControls.forEach(c -> c.setDisable(false));
    }

    @Override
    protected void disableControls() {
        listControls.forEach(c -> c.setDisable(true));

        typePasswordField.setDisable(true);
        repeatPasswordField.setDisable(true);
        btnShowTypePassword.setDisable(true);
        btnShowRepeatPassword.setDisable(true);
        typePasswordTextField.setDisable(true);
        repeatPasswordTextField.setDisable(true);
    }

    @Override
    protected void enableControls() {
        listControls.forEach(c -> c.setDisable(false));

        typePasswordField.setDisable(false);
        repeatPasswordField.setDisable(false);
        btnShowTypePassword.setDisable(false);
        btnShowRepeatPassword.setDisable(false);
        typePasswordTextField.setDisable(false);
        repeatPasswordTextField.setDisable(false);
    }

    private void turnTypePassword(boolean isSelected) {
        if (isNull(typePasswordTextField)){
            return;
        }

       if(isSelected) {
           typePasswordTextField.setText(typePassword);
           typePasswordTextField.setVisible(true);
           typePasswordTextField.setManaged(true);
           typePasswordField.setVisible(false);
           typePasswordField.setManaged(false);
       }
       else {
           typePasswordField.setText(typePassword);
           typePasswordField.setVisible(true);
           typePasswordField.setManaged(true);
           typePasswordTextField.setVisible(false);
           typePasswordTextField.setManaged(false);
       }
    }

    private void turnRepeatPassword(boolean isSelected) {
        if (isNull(repeatPasswordTextField)) {
            return;
        }

        if(isSelected) {
            repeatPasswordTextField.setText(repeatPassword);
            repeatPasswordTextField.setVisible(true);
            repeatPasswordTextField.setManaged(true);
            repeatPasswordField.setVisible(false);
            repeatPasswordField.setManaged(false);
        }
        else {
            repeatPasswordField.setText(repeatPassword);
            repeatPasswordField.setVisible(true);
            repeatPasswordField.setManaged(true);
            repeatPasswordTextField.setVisible(false);
            repeatPasswordTextField.setManaged(false);
        }
    }


    @FXML
    private void turnShowPassword(ActionEvent actionEvent) {
        ToggleButton source = (ToggleButton) actionEvent.getSource();

        switch (source.getId()) {
            case "btnShowTypePassword"   -> turnTypePassword(source.isSelected());
            case "btnShowRepeatPassword" -> turnRepeatPassword(source.isSelected());
        }
    }

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectPdfFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectPdfFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")).ifPresent(this::loadFile);
    }

    @FXML
    public void onActionChoiceDirForSaveFile() {
        selectOutputDirectory(btnChoiceDirForSaveFile, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save PDF");
    }

    private boolean checks() {
        if (isNull(imageProperties.getImage())) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Please select a PDF file", imageProperties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
            return false;
        }

        if (isNull(imageProperties.getOutput())) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Please select output directory", imageProperties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
            return false;
        }

        if (isNull(typePassword) || typePassword.isEmpty()) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Please enter password", imageProperties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
            return false;
        }

        if (isNull(repeatPassword) || repeatPassword.isEmpty()) {
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

                if (isNull(imageProperties.getImage())) {
                    throw new IOException("PDF file not selected");
                }

                updateProgress(30, 100);

                String baseName = imageProperties.getImage().getName().replaceFirst("[.][^.]+$", "");
                String shortId = UUID.randomUUID().toString().substring(0, 8);
                File outputFile = new File(imageProperties.getOutput(), baseName + "_protected_" + shortId + ".pdf");

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
        if (nonNull(currentDoc)) {
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
    protected void handleTaskFailure(@NonNull Throwable exception) {
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
                dropZone, imageViewPdf, progressBar, true, "PDF"
        );
        reset(imageProperties, ctx, "Selected PDF file: none");

        disableControls();

        try {
            currentDoc = PdfHelper.closeDocument(currentDoc);
        } catch (IOException e) {
            ErrorLogger.error("Error closing document during reset: " + e.getMessage());
        }
        
        if (nonNull(imageViewPdf)) {
            imageViewPdf.setImage(null);
        }

        labelPreviewPlaceholder.setVisible(true);

        typePassword = "";
        repeatPassword = "";
        typePasswordField.setText("");
        repeatPasswordField.setText("");
        if (nonNull(typePasswordTextField)) {
            typePasswordTextField.setText("");
        }
        if (nonNull(repeatPasswordTextField)) {
            repeatPasswordTextField.setText("");
        }
        
        labelPasswordMatch.setVisible(false);

        if (btnShowTypePassword.isSelected()) {
            btnShowTypePassword.setSelected(false);
            typePasswordField.setVisible(true);
            typePasswordField.setManaged(true);
            if (nonNull(typePasswordTextField)) {
                typePasswordTextField.setVisible(false);
                typePasswordTextField.setManaged(false);
            }
        }

        if (btnShowRepeatPassword.isSelected()) {
            btnShowRepeatPassword.setSelected(false);
            repeatPasswordField.setVisible(true);
            repeatPasswordField.setManaged(true);
            if (nonNull(repeatPasswordTextField)) {
                repeatPasswordTextField.setVisible(false);
                repeatPasswordTextField.setManaged(false);
            }
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
                        1. Select a PDF file using "Select PDF" or drag and drop it into the dash-bordered zone;

                        2. (Optional) Select where you want to save the result by clicking on "Directory for save".
                            (Default directory: Desktop);

                        3. Enter a password and repeat it in the fields to confirm;

                        4. Click "Protect and Download".

                        Security settings:
                        - 256-bit AES encryption
                        - Printing allowed
                        - Editing, copying, and form filling disabled.

                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    private void loadFile(File selectedFile) {
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
