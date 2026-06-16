package media_multitool.converters;

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
import model.helper.pdfWorker.ConverterPdfHelper;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import viewHelp.Alerts;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static model.utility.Util.getSavedPath;
import static viewHelp.Message.*;

public class ConverterImageToPdfController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML private ImageView imageViewPdf;
    @FXML private StackPane dropZone, previewContainer;
    @FXML private Button btnSelectFile, btnChoiceDirForSaveFile, btnSubmit;
    @FXML private Label labelSelectFileName, textDragZone, labelPreviewPlaceholder;
    
    @FXML private ComboBox<String> comboMargin, comboOrientation, comboPageSize;

    private PDDocument currentDoc;
    private List<Control> listControls;
    @FXML
    public void initialize() {
        listControls = List.of(comboMargin, comboOrientation, comboPageSize, btnSubmit);

        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);

        initComboBoxes();
        setupDragAndDrop(dropZone, textDragZone, Global.getAllSupportedImageFormats(), this::loadFile);

        isPressedReset();
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
        if (imageProperties.getImage() != null) {
            loadFile(imageProperties.getImage());
        }
    }

    @Override
    protected void lockUI() {
        btnSelectFile.setDisable(true);
        btnChoiceDirForSaveFile.setDisable(true);
        btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSelectFile.setDisable(false);
        btnChoiceDirForSaveFile.setDisable(false);
        btnReset.setDisable(false);
    }

    @Override
    protected void disableControls() {
        listControls.forEach(c -> c.setDisable(true));
    }

    @Override
    protected void enableControls() {
        listControls.forEach(c -> c.setDisable(false));
    }

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Images", "*.png", "*.tiff", "*.jpg", "*.jpeg", "*.svg"),
                "Choice image"
        ).ifPresent(this::loadFile);
    }

    @FXML
    public void onActionChoiceDirForSaveFile() {
        selectOutputDirectory(btnChoiceDirForSaveFile, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save image");
    }

    @FXML
    public void submitAndDownload() {
        if (Checking.checkImageAndOutputOnNull(imageProperties)) {
            return;
        }

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);

                File outputFile = Util.createOutputFile(
                        imageProperties.getImage(),
                        imageProperties.getOutput(),
                        "pdf"
                );

                updateProgress(50, 100);

                if (currentDoc != null) {
                    currentDoc.save(outputFile);
                }

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
        Util.reset(imageProperties, ctx, "Selected image file: none");

        disableControls();

        closeCurrentDoc();
        if (imageViewPdf != null) {
            imageViewPdf.setImage(null);
        }

         labelPreviewPlaceholder.setVisible(true);

         comboMargin.setValue("No margin");
         comboOrientation.setValue("Portrait");
         comboPageSize.setValue("Fix (image size)");
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
                "Image to PDF",
                """
                        How to use:
                        1. Select an image file using 'Select image' or drag and drop.
                        2. Choose Margin, Orientation and Page Size.
                        3. Click 'Submit and Download' to save the PDF.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    private void loadFile(File selectedFile) {
        enableControls();

        closeCurrentDoc();
        imageProperties.setImage(selectedFile);
        imageProperties.setTypeImage(DetermineType.determineFormat(selectedFile).orElse(null));
        labelSelectFileName.setText("Select image: " + selectedFile.getName());
        textDragZone.setText("Selected: " + selectedFile.getName());

        dropZone.getStyleClass().add("drop-zone-filled");

        Util.bindingImageViewToPreviewContainer(imageViewPdf, previewContainer);

        String margin = comboMargin.getValue();
        String orientation = comboOrientation.getValue();
        String pageSize = comboPageSize.getValue();
        
        String marginVal = margin.toLowerCase();
        String orientationVal = orientation.toLowerCase();
        String pageSizeVal = "fix";
        if (pageSize.contains("A4")) pageSizeVal = "a4";
        else if (pageSize.contains("US Letter")) pageSizeVal = "us letter";

        ConverterPdfHelper helper = new ConverterPdfHelper();
        helper.getDocumentFromImage(imageProperties.getPathToImage(), marginVal, pageSizeVal, orientationVal).ifPresent(doc -> {
            currentDoc = doc;
            updatePreview();
        });
    }
}
