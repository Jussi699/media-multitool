package controllers.pdf;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.FlowPane;
import controllers.abstractControllers.AbstractPdfToolController;
import model.helper.pdf.ConvertImagesToPdfHelper;
import model.logger.ErrorLogger;
import model.utility.context.ResetContext;
import model.utility.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import visual.dialogs.Alerts;
import visual.components.ImagePreviewCard;
import visual.dialogs.InfoAlert;
import visual.controls.Message;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.io.PathWorker.*;
import static visual.controls.Message.setupClearMessageTimer;

public class ConverterImagesToPdfController extends AbstractPdfToolController {
    private final List<ImageEntry> selectedImages = new ArrayList<>();
    private final List<ImagePreviewCard> imageCards = new ArrayList<>();
    private final ConvertImagesToPdfHelper helper = new ConvertImagesToPdfHelper();

    @FXML private ScrollPane scrollPanePreview;
    @FXML private ComboBox<String> comboMargin, comboOrientation, comboPageSize;
    @FXML private FlowPane imagesFlowPane;

    private static class ImageEntry {
        final File file;
        final String id;

        ImageEntry(File file) {
            this.file = file;
            this.id = UUID.randomUUID().toString();
        }
    }

    private record PdfConversionRequest(
            List<File> imageFiles,
            File outputDirectory,
            String margin,
            String orientation,
            String pageSize
    ) {}

    @FXML
    private void initialize() {
        properties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, properties.getHideSuccessMessageTimer(), true);

        initComboBoxes();
        setupDragAndDropMultiple();

        isPressedReset();
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "Images to PDF",
                "1. Select one or more image files using \"Select images\" or drag and drop them into the dash-bordered zone;",
                """
                        3. (Optional) Drag image thumbnails to reorder them, or click the remove button to delete an image;

                        4. Select the desired margin, page orientation, and page size;
                        
                        5. Click "Submit and Download".
                        
                        You can cancel the conversion at any time using the "Cancel Conversion" button.
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        cancelProcessing();

        selectedImages.clear();
        imageCards.clear();
        imagesFlowPane.getChildren().clear();

        ResetContext ctx = new ResetContext(
                labelSelectFileName, labelSuccess, textDragZone, null,
                dropZone, null, progressBar, true, "image(s)"
        );
        reset(properties, ctx, "Last uploaded image: none");

        disableControls();

        comboMargin.setValue("No margin");
        comboOrientation.setValue("Portrait");
        comboPageSize.setValue("Fix (image size)");

        textDragZone.setText("Drag image(s) here");
        updateUIState();
    }

    @FXML
    public void submitAndDownload() {
        if (!hasSelectedImages()) {
            return;
        }

        PdfConversionRequest request = createConversionRequest();

        cancelFlag.set(false);
        executeMediaTask(createConversionTask(request));
        labelSuccess.setManaged(true);
    }

    @FXML
    public void selectMultipleImageFile() {
        selectMultipleImageFile(this::loadFile);
    }

    @Override
    protected void toggleAdditionalUI(boolean disabled) {
        scrollPanePreview.setDisable(disabled);
    }

    @Override
    protected void toggleAdditionalControls(boolean disabled) {
        setControlsDisabled(disabled, comboMargin, comboOrientation, comboPageSize);
    }

    @Override
    protected void loadFile(File file) {
        if (!validateSelectedFile(file)) {
            return;
        }
        if (!helper.isImageFile(file)) {
            ErrorLogger.warn(getClass(), "Rejected unsupported image file: " + file.getAbsolutePath());
            return;
        }

        ImageEntry entry = new ImageEntry(file);
        selectedImages.add(entry);
        createImagePreviewCard(entry);
        updateUIState();
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (isNull(result) || Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info(getClass(), "Images to PDF conversion successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            Message.showSuccessText(labelSuccess, "PDF saved!", properties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
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
            Dragboard dragboard = event.getDragboard();
            if (event.getGestureSource() != dropZone
                    && dragboard.hasFiles()
                    && dragboard.getFiles().stream().anyMatch(helper::isImageFile)) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                List<File> files = db.getFiles();
                for (File file : files) {
                    if (helper.isImageFile(file)) {
                        loadFile(file);
                    }
                }
            }
            event.consume();
        });
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
        textDragZone.setText(selectedImages.isEmpty() ? "Drag image(s) here" : "Selected: " + selectedImages.size() + " image(s)");

        if (!selectedImages.isEmpty()) {
            labelSelectFileName.setText("Last uploaded image: " + selectedImages.getLast().file.getName());

            enableControls();
            markDropZoneLoadedAlt(dropZone, textDragZone,
                    selectedImages.isEmpty() ?
                            "Drag image(s) here" :
                            "Selected: " + selectedImages.size() + " image(s)");
        } else {
            disableControls();
            dropZone.getStyleClass().remove("drop-zone-filled");
        }
    }

    private boolean hasSelectedImages() {
        if (!selectedImages.isEmpty()) {
            return true;
        }

        Alerts.alertDialog(
                Alert.AlertType.WARNING,
                "Warning",
                "No images selected",
                "Please select at least one image to convert to PDF."
        );
        return false;
    }

    private PdfConversionRequest createConversionRequest() {
        if (isNull(properties.getOutput())) {
            properties.setOutput(getSavedPath());
        }

        List<File> imageFiles = selectedImages.stream()
                .map(entry -> entry.file)
                .toList();

        return new PdfConversionRequest(
                imageFiles,
                properties.getOutput(),
                helper.parseMarginValue(comboMargin.getValue()),
                helper.parseOrientationValue(comboOrientation.getValue()),
                helper.parsePageSizeValue(comboPageSize.getValue())
        );
    }

    private Task<File> createConversionTask(PdfConversionRequest request) {
        return new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);
                return convertAndSave(
                        request,
                        progress -> updateProgress(progress, 100),
                        () -> isCancelled() || cancelFlag.get()
                );
            }
        };
    }

    private File convertAndSave(
            PdfConversionRequest request,
            IntConsumer progressUpdater,
            BooleanSupplier cancellationRequested
    ) throws IOException {
        File outputFile = null;
        PDDocument document = null;
        try {
            checkConversionCancelled(cancellationRequested);
            outputFile = generateUniquePdfOutputFile(
                    request.outputDirectory().getAbsolutePath(),
                    "merged_images"
            );

            progressUpdater.accept(30);
            checkConversionCancelled(cancellationRequested);

            document = helper.convertImagesToPdf(
                    request.imageFiles(),
                    request.margin(),
                    request.pageSize(),
                    request.orientation(),
                    progressUpdater
            );

            checkConversionCancelled(cancellationRequested);
            PDDocument documentToSave = document;
            document = null;
            helper.savePdfDocument(documentToSave, outputFile);

            checkConversionCancelled(cancellationRequested);
            progressUpdater.accept(100);
            return outputFile;
        } catch (IOException | RuntimeException exception) {
            if (nonNull(document)) {
                try {
                    document.close();
                } catch (IOException closeException) {
                    exception.addSuppressed(closeException);
                }
            }
            FileUtils.cleanupFile(outputFile);
            throw exception;
        }
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

    private void checkConversionCancelled(BooleanSupplier cancellationRequested) throws InterruptedIOException {
        if (cancellationRequested.getAsBoolean() || Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("Conversion cancelled");
        }
    }
}
