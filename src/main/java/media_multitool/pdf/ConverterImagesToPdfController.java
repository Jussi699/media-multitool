package media_multitool.pdf;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import media_multitool.AbstractMediaController;
import model.helper.pdf.ConvertImagesToPdfHelper;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import viewHelp.Alerts;
import viewHelp.ImagePreviewCard;
import viewHelp.Message;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static model.utility.Util.getSavedPath;

public class ConverterImagesToPdfController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();
    private final List<ImageEntry> selectedImages = new ArrayList<>();
    private final List<ImagePreviewCard> imageCards = new ArrayList<>();
    private final ConvertImagesToPdfHelper helper = new ConvertImagesToPdfHelper();
    
    private static class ImageEntry {
        final File file;
        final String id;
        
        ImageEntry(File file) {
            this.file = file;
            this.id = UUID.randomUUID().toString();
        }
    }

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML private FlowPane imagesFlowPane;
    @FXML private ScrollPane scrollPanePreview;
    @FXML private StackPane dropZone;
    @FXML private Button btnSelectFiles, btnChoiceDirForSaveFile, btnSubmit;
    @FXML private Label labelSelectFileName, textDragZone;
    
    @FXML private ComboBox<String> comboMargin, comboOrientation, comboPageSize;

    private List<Control> listControls;

    @FXML
    public void initialize() {
        listControls = List.of(comboMargin, comboOrientation, comboPageSize, btnSubmit);

        imageProperties.setOutput(getSavedPath());

        initComboBoxes();
        setupDragAndDropMultiple();

        isPressedReset();
    }

    private void initComboBoxes() {
        comboMargin.getItems().addAll("No margin", "Small", "Big");
        comboMargin.setValue("No margin");

        comboOrientation.getItems().addAll("Portrait", "Landscape");
        comboOrientation.setValue("Portrait");

        comboPageSize.getItems().addAll("A4 297x210 mm", "US Letter 215x279,4 mm", "Fix (image size)");
        comboPageSize.setValue("Fix (image size)");
    }

    private void setupDragAndDropMultiple() {
        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                List<File> files = db.getFiles();
                for (File file : files) {
                    if (isImageFile(file)) {
                        addImageToList(file);
                    }
                }
            }
            event.consume();
        });
    }

    private boolean isImageFile(File file) {
        return helper.isImageFile(file);
    }

    @Override
    protected void lockUI() {
        btnSelectFiles.setDisable(true);
        btnChoiceDirForSaveFile.setDisable(true);
        btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSelectFiles.setDisable(false);
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
    public void onActionBtnSelectFiles() {
        SelectFile selectPdf = new SelectFile();
        Stage stage = (Stage) btnSelectFiles.getScene().getWindow();

        selectPdf.showOpenMultipleDialog(stage,
                new FileChooser.ExtensionFilter("Images", "*.png", "*.tiff", "*.jpg", "*.jpeg", "*.svg", "*.bmp"),
                "Select images"
        ).ifPresent(f -> f.forEach(this::addImageToList));
    }

    private void addImageToList(File file) {
        ImageEntry entry = new ImageEntry(file);
        selectedImages.add(entry);
        createImagePreviewCard(entry);
        updateUIState();
    }

    private void createImagePreviewCard(ImageEntry entry) {
        ImagePreviewCard card = new ImagePreviewCard(
            entry.file,
            entry.id,
            () -> removeImage(entry.id),
            draggedId -> handleDragDrop(draggedId, entry.id)
        );
        imageCards.add(card);
        imagesFlowPane.getChildren().add(card.getContainer());
    }

    private void updateUIState() {
        labelSelectFileName.setText("Selected images: " + selectedImages.size());
        textDragZone.setText(selectedImages.isEmpty() ? 
            "Drag images here or click 'Select images'" : 
            "Selected: " + selectedImages.size() + " image(s)");
        
        if (!selectedImages.isEmpty()) {
            enableControls();
            if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
                dropZone.getStyleClass().add("drop-zone-filled");
            }
        } else {
            disableControls();
            dropZone.getStyleClass().remove("drop-zone-filled");
        }
    }

    @FXML
    public void onActionChoiceDirForSaveFile() {
        selectOutputDirectory(btnChoiceDirForSaveFile, imageProperties.getOutput(), 
            imageProperties::setOutput, "Select directory for save PDF");
    }

    @FXML
    public void submitAndDownload() {
        if (selectedImages.isEmpty()) {
            Alerts.alertDialog(
                Alert.AlertType.WARNING,
                "Warning",
                "No images selected",
                "Please select at least one image to convert to PDF."
            );
            return;
        }

        if (imageProperties.getOutput() == null) {
            imageProperties.setOutput(getSavedPath());
        }

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);

                File outputDirFile = imageProperties.getOutput() != null ?
                    imageProperties.getOutput() : 
                    getSavedPath();
                String outputDir = outputDirFile.getAbsolutePath();
                File outputFile = helper.generateUniqueOutputFile(
                    outputDir, 
                    "merged_images"
                );

                updateProgress(30, 100);

                String marginVal = helper.parseMarginValue(comboMargin.getValue());
                String orientationVal = helper.parseOrientationValue(comboOrientation.getValue());
                String pageSizeVal = helper.parsePageSizeValue(comboPageSize.getValue());

                List<File> imageFiles = new ArrayList<>();
                for (ImageEntry entry : selectedImages) {
                    imageFiles.add(entry.file);
                }

                PDDocument finalDoc = helper.convertImagesToPdf(
                    imageFiles, marginVal,
                    pageSizeVal, orientationVal,
                    progress -> updateProgress(progress, 100)
                );

                helper.savePdfDocument(finalDoc, outputFile);

                updateProgress(100, 100);

                return outputFile;
            }
        };

        executeMediaTask(task);
        labelSuccess.setManaged(true);
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
            Message.showSuccessText(labelSuccess, "PDF saved!", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            labelSuccess.setVisible(true);
            
            PauseTransition hideTransition = new PauseTransition(Duration.seconds(5));
            hideTransition.setOnFinished(_ -> {
                labelSuccess.setVisible(false);
                progressBar.setProgress(0);
            });
            hideTransition.play();
        });
    }

    @Override
    protected void handleTaskFailure(Throwable exception) {
        super.handleTaskFailure(exception);
        Platform.runLater(() -> {
            Message.showErrorMessage(labelSuccess, "Error: " + exception.getMessage(), 
                imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            labelSuccess.setVisible(true);
            
            PauseTransition hideTransition = new PauseTransition(Duration.seconds(5));
            hideTransition.setOnFinished(_ -> {
                labelSuccess.setVisible(false);
                progressBar.setProgress(0);
            });
            hideTransition.play();
        });
    }

    @FXML
    public void isPressedReset() {
        selectedImages.clear();
        imageCards.clear();
        imagesFlowPane.getChildren().clear();
        
        ResetContext ctx = new ResetContext(
            labelSelectFileName, labelSuccess, textDragZone, null,
            dropZone, null, progressBar, true
        );
        Util.reset(imageProperties, ctx, "Selected images: 0");

        disableControls();

        comboMargin.setValue("No margin");
        comboOrientation.setValue("Portrait");
        comboPageSize.setValue("Fix (image size)");
        
        textDragZone.setText("Drag images here or click 'Select images'");
        updateUIState();
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
            Alert.AlertType.INFORMATION,
            "Information",
            "Images to PDF",
            """
                How to use:
                1. Select multiple images using 'Select images' or drag and drop.
                2. Drag image thumbnails to reorder them.
                3. Click the X button on any thumbnail to remove it.
                4. Choose Margin, Orientation and Page Size.
                5. Click 'Submit and Download' to save the PDF.
                
                If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    private void removeImage(String imageId) {
        int index = findImageIndexById(imageId);
        
        if (index != -1) {
            final int finalIndex = index;
            ImagePreviewCard card = imageCards.get(finalIndex);
            
            card.animateRemoval(() -> {
                selectedImages.remove(finalIndex);
                imageCards.remove(finalIndex);
                refreshPreview();
                updateUIState();
            });
        }
    }

    private void handleDragDrop(String draggedId, String targetId) {
        int sourceIndex = findImageIndexById(draggedId);
        int targetIndex = findImageIndexById(targetId);
        
        if (sourceIndex != -1 && targetIndex != -1 && sourceIndex != targetIndex) {
            ImageEntry temp = selectedImages.get(sourceIndex);
            selectedImages.set(sourceIndex, selectedImages.get(targetIndex));
            selectedImages.set(targetIndex, temp);
            
            ImagePreviewCard tempCard = imageCards.get(sourceIndex);
            imageCards.set(sourceIndex, imageCards.get(targetIndex));
            imageCards.set(targetIndex, tempCard);
            
            refreshPreview();
        }
    }

    private int findImageIndexById(String imageId) {
        for (int i = 0; i < selectedImages.size(); i++) {
            if (selectedImages.get(i).id.equals(imageId)) {
                return i;
            }
        }
        return -1;
    }

    private void refreshPreview() {
        imagesFlowPane.getChildren().clear();
        for (ImagePreviewCard card : imageCards) {
            imagesFlowPane.getChildren().add(card.getContainer());
        }
    }
}
