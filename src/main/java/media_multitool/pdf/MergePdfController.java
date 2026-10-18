package media_multitool.pdf;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.util.Duration;
import media_multitool.AbstractPdfToolController;
import model.helper.pdf.PdfHelper;
import model.logger.ErrorLogger;
import model.utility.ResetContext;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import viewHelp.Alerts;
import viewHelp.InfoAlert;
import viewHelp.Message;
import viewHelp.PdfPagePreviewCard;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.PathWorker.generateUniquePdfOutputFile;
import static model.utility.PathWorker.getSavedPath;
import static model.utility.Utility.cleanupFile;

public class MergePdfController extends AbstractPdfToolController {
    private final List<PageEntry> selectedPages = new ArrayList<>();
    private final List<PdfPagePreviewCard> pageCards = new ArrayList<>();

    private static class PageEntry {
        final File sourceFile;
        final int originalPageIndex;
        final String id;

        PageEntry(File sourceFile, int index) {
            this.sourceFile = sourceFile;
            this.originalPageIndex = index;
            this.id = UUID.randomUUID().toString();
        }
    }

    @FXML private FlowPane imagesFlowPane;
    @FXML private ScrollPane scrollPanePreview;

    @FXML
    public void initialize() {
        properties.setOutput(getSavedPath());
        setupDragAndDropMultiple();
        isPressedReset();
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "Merge PDF",
                "1. Select one or more PDF files using \"Select PDF\" or drag and drop them into the dash-bordered zone;",
                """
                        3. (Optional) Drag and drop thumbnails to reorder the PDF files, or click the remove button to remove a file;

                        4. Click "Submit and Download".
                        
                        You can cancel the conversion at any time using the "Cancel Merge" button.
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        selectedPages.clear();
        pageCards.clear();
        imagesFlowPane.getChildren().clear();
        properties.setImage(null);

        ResetContext ctx = new ResetContext(
                labelSelectFileName, labelSuccess, textDragZone, null,
                dropZone, null, progressBar, true, "PDF(s)"
        );
        if (nonNull(progressBar)) {
            progressBar.progressProperty().unbind();
        }
        reset(properties, ctx, "Last uploaded PDF: none");

        disableControls();
        textDragZone.setText("Drag PDF here");
        if (nonNull(dropZone)) {
            dropZone.getStyleClass().remove("drop-zone-filled");
        }
    }

    @FXML
    public void submitAndDownload() {
        if (selectedPages.isEmpty()) {
            Alerts.alertDialog(
                    Alert.AlertType.WARNING,
                    "Warning",
                    "No pages selected",
                    "Please add at least one PDF file."
            );
            return;
        }

        if (isNull(properties.getOutput())) {
            properties.setOutput(getSavedPath());
        }

        Task<File> task = createTaskMergePdf();
        executeMediaTask(task);
    }

    @FXML
    public void selectMultiplePdfFile() {
        selectMultiplePdfFile(this::loadFile);
    }

    @Override
    protected void lockUI() {
        toggleUI(true);
    }

    @Override
    protected void unlockUI() {
        toggleUI(false);
        cancelFlag.set(false);
    }

    @Override
    protected void disableControls() {
        toggleControls(true);
    }

    @Override
    protected void enableControls() {
        toggleControls(false);
    }

    @Override
    protected void toggleUI(boolean disabled) {
        toggleControls(disabled);
        setControlsDisabled(disabled, btnSelectFile, btnChoiceDirForSave);
        dropZone.setDisable(disabled);
        scrollPanePreview.setDisable(disabled);
        btnCancel.setDisable(!disabled);
    }

    @Override
    protected void toggleControls(boolean disabled) {
        setControlsDisabled(disabled, btnSubmit, btnReset);
    }

    @Override
    protected void loadFile(File file) {
        if (!validateSelectedFile(file)) {
            return;
        }

        if (isNull(properties.getImage())) {
            properties.setImage(file);
        }

        if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        Task<BufferedImage> loadTask = new Task<>() {
            @Override
            protected BufferedImage call() throws Exception {
                try (PDDocument document = PdfHelper.loadUnencryptedPdf(file)) {
                    PDFRenderer renderer = new PDFRenderer(document);
                    if (document.getNumberOfPages() > 0) {
                        return renderer.renderImageWithDPI(0, 72);
                    }
                }
                return null;
            }
        };

        setOnSucceededLoadTask(loadTask, file);
        setOnFailedLoadTask(loadTask);

        progressBar.progressProperty().bind(loadTask.progressProperty());

        startDaemonTask(loadTask);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (result instanceof File outputFile) {
            ErrorLogger.info(getClass(), "PDF merged successfully! Saved to: " + outputFile.getAbsolutePath());

            Platform.runLater(() -> {
                Message.showSuccessText(labelSuccess, "PDF saved!", properties.getHideSuccessMessageTimer());

                PauseTransition hideBar = new PauseTransition(Duration.seconds(5));
                hideBar.setOnFinished(_ -> {
                    labelSuccess.setVisible(false);
                    progressBar.progressProperty().unbind();
                    progressBar.setProgress(0);
                });
                hideBar.play();
            });
        }
    }

    private void setOnFailedLoadTask(Task<BufferedImage> loadTask) {
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
            progressBar.setProgress(0);
        });
    }

    private void setOnSucceededLoadTask(Task<BufferedImage> loadTask, File file) {
        loadTask.setOnSucceeded(_ -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(1.0);
            BufferedImage firstPage = loadTask.getValue();
            if (nonNull(firstPage)) {
                PageEntry entry = new PageEntry(file, 0);
                selectedPages.add(entry);
                createPagePreviewCard(entry, firstPage);
            }
            enableControls();
            updateUIState();

            PauseTransition hideBar = new PauseTransition(Duration.seconds(5));
            hideBar.setOnFinished(_ -> {
                progressBar.progressProperty().unbind();
                progressBar.setProgress(0);
            });
            hideBar.playFromStart();
        });
    }

    private void createPagePreviewCard(PageEntry entry, BufferedImage image) {
        PdfPagePreviewCard card = new PdfPagePreviewCard(
            image,
            entry.sourceFile.getName(),
            entry.originalPageIndex,
            entry.id,
            () -> mergePage(entry.id),
                draggedId -> reorderPages(draggedId, entry.id),
                240, 270, 230, 260
        );

        pageCards.add(card);
        imagesFlowPane.getChildren().add(card.getContainer());
    }

    private void reorderPages(String draggedId, String targetId) {
        int draggedIdx = -1;
        int targetIdx = -1;
        for (int i = 0; i < selectedPages.size(); i++) {
            if (selectedPages.get(i).id.equals(draggedId)) draggedIdx = i;
            if (selectedPages.get(i).id.equals(targetId)) targetIdx = i;
        }

        if (draggedIdx != -1 && targetIdx != -1 && draggedIdx != targetIdx) {
            PageEntry draggedEntry = selectedPages.remove(draggedIdx);
            PdfPagePreviewCard draggedCard = pageCards.remove(draggedIdx);

            selectedPages.add(targetIdx, draggedEntry);
            pageCards.add(targetIdx, draggedCard);

            refreshPreview();
        }
    }

    private void updateUIState() {
        labelSelectFileName.setText(selectedPages.isEmpty()
                ? "Selected PDF file: none" : "Last uploaded PDF: " + selectedPages.getLast().sourceFile.getName());

        textDragZone.setText("PDF selected: " + selectedPages.size());

        if (!selectedPages.isEmpty()) {
            enableControls();
        } else {
            disableControls();
            textDragZone.setText("Drag PDF here");
            dropZone.getStyleClass().remove("drop-zone-filled");
        }
    }

    private Task<File> createTaskMergePdf() {
        List<PageEntry> pagesToMerge = List.copyOf(selectedPages);
        return new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(0, 100);

                File outputDirFile = properties.getOutput();
                String baseName = "merged_document";
                if (!pagesToMerge.isEmpty()) {
                    baseName = pagesToMerge.getFirst().sourceFile.getName().replace(".pdf", "") + "_merged";
                }
                File outputFile = generateUniquePdfOutputFile(outputDirFile.getAbsolutePath(), baseName);

                try {
                    try (PDDocument targetDoc = new PDDocument()) {
                        int totalFiles = pagesToMerge.size();
                        PDFMergerUtility merger = new PDFMergerUtility();

                        for (int i = 0; i < totalFiles; i++) {
                            if (isCancelled() || cancelFlag.get() || Thread.currentThread().isInterrupted()) {
                                throw new InterruptedException("Merge cancelled");
                            }
                            PageEntry entry = pagesToMerge.get(i);
                            try (PDDocument sourceDoc = PdfHelper.loadUnencryptedPdf(entry.sourceFile)) {
                                merger.appendDocument(targetDoc, sourceDoc);
                            }
                            updateProgress(i + 1L, totalFiles);
                        }
                        if (isCancelled() || cancelFlag.get() || Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException("Merge cancelled");
                        }
                        targetDoc.save(outputFile);
                    }
                    return outputFile;
                } catch (Exception exception) {
                    cleanupFile(outputFile);
                    throw exception;
                }
            }
        };
    }

    private void mergePage(String cardId) {
        int index = -1;
        for (int i = 0; i < pageCards.size(); i++) {
            if (pageCards.get(i).getCardId().equals(cardId)) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            final int finalIndex = index;
            PdfPagePreviewCard card = pageCards.get(finalIndex);

            card.animateRemoval(() -> {
                selectedPages.remove(finalIndex);
                pageCards.remove(finalIndex);
                refreshPreview();
                updateUIState();
            });
        }
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
            boolean success = false;
            if (db.hasFiles()) {
                List<File> pdfFiles = db.getFiles().stream()
                        .filter(file -> file.getName().toLowerCase().endsWith(".pdf"))
                        .toList();
                pdfFiles.forEach(this::loadFile);
                success = !pdfFiles.isEmpty();
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void refreshPreview() {
        imagesFlowPane.getChildren().clear();
        for (PdfPagePreviewCard card : pageCards) {
            imagesFlowPane.getChildren().add(card.getContainer());
        }
    }
}
