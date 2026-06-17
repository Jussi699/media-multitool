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
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import viewHelp.Alerts;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static model.utility.Util.getSavedPath;
import static viewHelp.Message.*;

public class ConverterPdfToImageController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML private ImageView imageViewPdf;
    @FXML private StackPane dropZone, previewContainer;
    @FXML private Button btnSelectFile, btnChoiceDirForSaveFile, btnSubmit;
    @FXML private Label labelSelectFileName, textDragZone, labelPreviewPlaceholder;
    
    @FXML private ToggleButton btnToPNG, btnToJPEG, btnToWEBP, btnToTIFF, btnToBMP;
    @FXML private ToggleButton btnToPPM, btnToPGM, btnToPAM;

    private PDDocument currentDoc;
    private List<Control> listControls;
    private ToggleGroup toggleGroup;
    @FXML
    public void initialize() {
        toggleGroup = new ToggleGroup();
        btnToPNG.setToggleGroup(toggleGroup);
        btnToJPEG.setToggleGroup(toggleGroup);
        btnToWEBP.setToggleGroup(toggleGroup);
        btnToTIFF.setToggleGroup(toggleGroup);
        btnToBMP.setToggleGroup(toggleGroup);
        btnToPPM.setToggleGroup(toggleGroup);
        btnToPGM.setToggleGroup(toggleGroup);
        btnToPAM.setToggleGroup(toggleGroup);

        listControls = List.of(btnToPNG, btnToJPEG, btnToWEBP, btnToTIFF, btnToBMP, 
                               btnToPPM, btnToPGM, btnToPAM, btnSubmit);

        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);

        setupDragAndDrop(dropZone, List.of("*.pdf"), this::loadFile);

        isPressedReset();
    }

    @FXML
    public void onActionClickToggleBtnFormat() {
        ToggleButton selectedBtn = (ToggleButton) toggleGroup.getSelectedToggle();
        if (selectedBtn != null) {
            String format = selectedBtn.getText().replace("to ", "").toLowerCase();
            imageProperties.setTypeImage(format);
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

    @FXML
    public void submitAndDownload() {
        if (Checking.checkImageAndOutputOnNull(imageProperties)) {
            return;
        }

        if (imageProperties.getTypeImage() == null || imageProperties.getTypeImage().isEmpty()) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, "Please select output format", imageProperties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
            return;
        }

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);

                if (currentDoc == null) {
                    throw new IOException("PDF document not loaded");
                }

                PDFRenderer renderer = new PDFRenderer(currentDoc);

                updateProgress(30, 100);

                BufferedImage image = renderer.renderImageWithDPI(0, 300);

                updateProgress(60, 100);

                File outputFile = Util.createOutputFile(
                        imageProperties.getImage(),
                        imageProperties.getOutput(),
                        imageProperties.getTypeImage()
                );

                String format = imageProperties.getTypeImage().toUpperCase();
                if (format.equals("JPEG") || format.equals("JPG")) {
                    format = "jpg";
                }

                ImageIO.write(image, format, outputFile);

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
        ErrorLogger.info("Conversion to image successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Image saved!", imageProperties.getHideSuccessMessageTimer());
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
        Util.reset(imageProperties, ctx, "Selected PDF file: none");

        disableControls();

        closeCurrentDoc();
        if (imageViewPdf != null) {
            imageViewPdf.setImage(null);
        }

        labelPreviewPlaceholder.setVisible(true);

        toggleGroup.selectToggle(null);
        imageProperties.setTypeImage(null);
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
                "PDF to Image",
                """
                        How to use:
                        1. Select a PDF file using 'Select PDF file' or drag and drop.
                        2. Choose output format (PNG, JPEG, WEBP, TIFF, BMP, PPM, PGM, PAM).
                        3. Click 'Convert and Download' to save the image.
                        
                        Note: Only the first page of the PDF will be converted.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    private void loadFile(File selectedFile) {
        enableControls();

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
        } catch (IOException e) {
            ErrorLogger.error("Error loading PDF: " + e.getMessage());
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, "Error loading PDF: " + e.getMessage(), imageProperties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
        }
    }
}
