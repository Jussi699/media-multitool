package controllers.pdfTools;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import controllers.abstractControllers.AbstractPdfToolController;
import model.helper.pdf.PdfHelper;
import model.logger.ErrorLogger;
import model.utility.context.ResetContext;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import visual.dialogs.Alerts;
import visual.dialogs.InfoAlert;
import visual.controls.Message;
import visual.components.PdfPagePreviewCard;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.io.PathWorker.generateUniquePdfOutputFile;
import static model.utility.io.PathWorker.getSavedPath;

public class RemovePagesPdfController extends AbstractPdfToolController {
    private final List<PdfHelper.PageEntry> allPages = new ArrayList<>();
    private final List<PdfPagePreviewCard> pageCards = new ArrayList<>();
    private final Set<Integer> removalPageIndices = new HashSet<>();

    @FXML private ScrollPane scrollPanePreview;
    @FXML private FlowPane imagesFlowPane;

    @FXML
    public void initialize() {
        properties.setOutput(getSavedPath());
        setupDragAndDropMultiple();
        isPressedReset();
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "Remove Pages PDF",
                "1. Select a PDF file using \"Select PDF\" or drag and drop it into the dash-bordered zone;",
                """
                        3. Click page thumbnails to mark the pages to remove (marked pages have a red border; use Shift + click to mark a range);

                        4. Click "Submit and Download".
                       
                        You can cancel the conversion at any time using the "Cancel Remove" button.
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        allPages.clear();
        pageCards.clear();
        removalPageIndices.clear();
        imagesFlowPane.getChildren().clear();
        properties.setImage(null);

        ResetContext ctx = new ResetContext(
                labelSelectFileName, labelSuccess, textDragZone, null,
                dropZone, null, progressBar, true, "PDF"
        );
        if (nonNull(progressBar)) {
            progressBar.progressProperty().unbind();
        }
        reset(properties, ctx, "Selected PDF file: none");

        disableControls();
        textDragZone.setText("Drag PDF here");
        dropZone.getStyleClass().remove("drop-zone-filled");
    }

    @FXML
    public void submitAndDownload() {
        if (removalPageIndices.isEmpty()) {
            Alerts.alertDialog(
                    Alert.AlertType.WARNING,
                    "Warning",
                    "No pages selected",
                    "Select at least one page to remove."
            );
            return;
        }
        if (removalPageIndices.size() == allPages.size()) {
            Alerts.alertDialog(
                    Alert.AlertType.WARNING,
                    "Warning",
                    "All pages selected",
                    "Please keep at least one page in the PDF."
            );
            return;
        }

        if (isNull(properties.getOutput())) {
            properties.setOutput(getSavedPath());
        }

        Task<File> task = getProcessingTask();
        executeMediaTask(task);
    }

    @Override
    protected void toggleAdditionalUI(boolean disabled) {
        scrollPanePreview.setDisable(disabled);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (result instanceof File outputFile) {
            ErrorLogger.info(getClass(), "PDF pages removed successfully! Saved to: " + outputFile.getAbsolutePath());

            Platform.runLater(() -> {
                Message.showSuccessText(labelSuccess, "PDF saved!", properties.getHideSuccessMessageTimer());

                properties.getHideSuccessMessageTimer().setOnFinished(_ -> {
                    labelSuccess.setVisible(false);
                    progressBar.setProgress(0);
                });
            });
        }
    }

    @Override
    protected void loadFile(File file) {
        if (!validateSelectedFile(file)) {
            return;
        }

        isPressedReset();
        properties.setImage(file);
        labelSelectFileName.setText("Selected PDF: " + file.getName());

        markDropZoneLoaded(dropZone, textDragZone, file.getName());

        Task<List<BufferedImage>> loadTask = getLoadTask(file);

        setOnSucceededLoadTask(loadTask);
        setOnFailedLoadTask(loadTask);

        progressBar.progressProperty().bind(loadTask.progressProperty());

        startDaemonTask(loadTask);
    }

    private Task<List<BufferedImage>> getLoadTask(File file) {
        return new Task<>() {
            @Override
            protected List<BufferedImage> call() throws Exception {
                List<BufferedImage> images = new ArrayList<>();
                try (PDDocument document = PdfHelper.loadUnencryptedPdf(file)) {
                    PDFRenderer renderer = new PDFRenderer(document);
                    int pageCount = document.getNumberOfPages();
                    for (int i = 0; i < pageCount; i++) {
                        var pageSize = document.getPage(i).getMediaBox();
                        float width = pageSize.getWidth();
                        float height = pageSize.getHeight();
                        int rotation = Math.floorMod(document.getPage(i).getRotation(), 360);
                        if (rotation == 90 || rotation == 270) {
                            float temp = width;
                            width = height;
                            height = temp;
                        }

                        final float PREVIEW_MAX_WIDTH = 230;
                        final float PREVIEW_MAX_HEIGHT = 260;
                        float scale = Math.min(PREVIEW_MAX_WIDTH / width, PREVIEW_MAX_HEIGHT / height);
                        BufferedImage image = renderer.renderImage(i, scale);
                        images.add(image);
                        updateProgress(i + 1L, pageCount);
                    }
                }
                return images;
            }
        };
    }

    private Task<File> getProcessingTask() {
        return new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);

                File inputFile = properties.getImage();
                File outputDirFile = properties.getOutput();

                String baseName = inputFile.getName().replace(".pdf", "") + "_modified";
                File outputFile = generateUniquePdfOutputFile(outputDirFile.getAbsolutePath(), baseName);

                updateProgress(30, 100);

                try (PDDocument sourceDoc = PdfHelper.loadUnencryptedPdf(inputFile);
                     PDDocument targetDoc = new PDDocument()) {

                    PDFMergerUtility merger = new PDFMergerUtility();
                    int total = allPages.size() - removalPageIndices.size();
                    int processed = 0;
                    for (PdfHelper.PageEntry page : allPages) {
                        if (removalPageIndices.contains(page.originalPageIndex)) {
                            continue;
                        }
                        try (PDDocument tempDoc = new PDDocument()) {
                            tempDoc.addPage(sourceDoc.getPage(page.originalPageIndex));
                            merger.appendDocument(targetDoc, tempDoc);
                        }
                        updateProgress(30 + (60L * ++processed / total), 100);
                    }

                    targetDoc.save(outputFile);
                }

                updateProgress(100, 100);
                return outputFile;
            }
        };
    }

    private void setOnFailedLoadTask(Task<List<BufferedImage>> loadTask) {
        loadTask.setOnFailed(_ -> {
            progressBar.progressProperty().unbind();
            String textEx = loadTask.getException().getMessage();
            ErrorLogger.error("Failed to load PDF: " + textEx);
            Platform.runLater(() -> Alerts.alertDialog(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Failed to load PDF",
                    textEx,
                    Modality.APPLICATION_MODAL
            ));
            isPressedReset();

            progressBar.setProgress(0);
        });
    }

    private void setOnSucceededLoadTask(Task<List<BufferedImage>> loadTask) {
        loadTask.setOnSucceeded(_ -> {
            progressBar.progressProperty().unbind();
            List<BufferedImage> pageImages = loadTask.getValue();
            for (int i = 0; i < pageImages.size(); i++) {
                addPageToList(i, pageImages.get(i));
                pageImages.set(i, null);
            }
            updateUIState();

            PauseTransition hideBar = properties.getHideSuccessMessageTimer();
            hideBar.setOnFinished(_ -> progressBar.setProgress(0));
            hideBar.playFromStart();
        });
    }

    private void setupDragAndDropMultiple() {
        dropZone.setOnDragOver(event -> {
            Dragboard dragboard = event.getDragboard();
            if (event.getGestureSource() != dropZone
                    && dragboard.hasFiles()
                    && dragboard.getFiles().stream()
                    .anyMatch(file -> file.getName().toLowerCase().endsWith(".pdf"))) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            File pdfFile = null;
            if (db.hasFiles()) {
                pdfFile = db.getFiles().stream()
                        .filter(file -> file.getName().toLowerCase().endsWith(".pdf"))
                        .findFirst()
                        .orElse(null);
            }
            if (nonNull(pdfFile)) {
                loadFile(pdfFile);
            }
            event.setDropCompleted(nonNull(pdfFile));
            event.consume();
        });
    }

    private void addPageToList(int index, BufferedImage image) {
        PdfHelper.PageEntry entry = new PdfHelper.PageEntry(index);
        allPages.add(entry);
        createPagePreviewCard(entry, image);
    }

    private void createPagePreviewCard(PdfHelper.PageEntry entry, BufferedImage image) {
        PdfPagePreviewCard card = new PdfPagePreviewCard(
                image,
                properties.getImage() != null ? properties.getImage().getName() : "PDF",
                entry.originalPageIndex,
                entry.id,
                null,
                null,
                240, 270, 230, 260,
                false
        );
        pageCards.add(card);
        imagesFlowPane.getChildren().add(card.getContainer());

        card.getContainer().setOnMouseClicked(event -> {
            int pageIndex = entry.originalPageIndex;
            if (event.isShiftDown() && !removalPageIndices.isEmpty()) {
                int anchor = removalPageIndices.stream().mapToInt(Integer::intValue).max().orElse(pageIndex);
                int start = Math.min(anchor, pageIndex);
                int end = Math.max(anchor, pageIndex);
                for (int i = start; i <= end; i++) {
                    if (i < pageCards.size()) {
                        markPageForRemoval(i, pageCards.get(i));
                    }
                }
            } else {
                togglePageSelection(pageIndex, card);
            }
            updateUIState();
        });
    }

    private void updateUIState() {
        labelSelectFileName.setText(isNull(properties.getImage())
                ? "Selected PDF file: none"
                : "Selected PDF: " + properties.getImage().getName() + " (Pages: " + allPages.size()
                + ", marked for removal: " + removalPageIndices.size() + ")");

        if (!allPages.isEmpty()) {
            enableControls();
        } else {
            disableControls();
        }
    }

    private void togglePageSelection(int index, PdfPagePreviewCard card) {
        if (removalPageIndices.remove(index)) {
            card.getContainer().setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 8; -fx-padding: 5; "
                    + "-fx-border-color: #555; -fx-border-radius: 8; -fx-border-width: 1;");
        } else {
            markPageForRemoval(index, card);
        }
    }

    private void markPageForRemoval(int index, PdfPagePreviewCard card) {
        if (removalPageIndices.add(index)) {
            card.getContainer().setStyle("-fx-background-color: #3a2a2a; -fx-background-radius: 8; -fx-padding: 5; "
                    + "-fx-border-color: #ff4444; -fx-border-radius: 8; -fx-border-width: 2;");
        }
    }
}
