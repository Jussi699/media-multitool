package media_multitool.pdf;

import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.util.Duration;
import media_multitool.AbstractPdfToolController;
import model.helper.pdf.PdfHelper;
import model.utility.ResetContext;
import model.utility.Utility;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import viewHelp.Alerts;
import viewHelp.InfoAlert;
import viewHelp.Message;
import viewHelp.PdfPagePreviewCard;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.helper.pdf.SplitPdfHelper.*;
import static model.utility.PathWorker.getSavedPath;

public class SplitPdfController extends AbstractPdfToolController {
    private final List<PdfHelper.PageEntry> allPages = new ArrayList<>();
    private final List<PdfPagePreviewCard> pageCards = new ArrayList<>();
    private final Set<Integer> selectedPageIndices = new HashSet<>();

    @FXML private ToggleGroup rangeTypeGroup, splitModeGroup;
    @FXML private RadioButton rbCustomRange, rbFixedRange, rbRange, rbPages;
    @FXML private TextField tfFromPage, tfToPage;
    @FXML private ScrollPane scrollPanePreview;
    @FXML private FlowPane imagesFlowPane;
    @FXML private VBox vboxRangeOptions, vboxPagesOptions, vboxToPage;

    @FXML
    public void initialize() {
        properties.setOutput(getSavedPath());

        setupDragAndDrop();
        disableControls();
        updateUIState();
        setupListener();
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "Split PDF",
                "Select a PDF file using \"Select PDF\" or drag and drop it into the dash-bordered zone;",
                """
                        3.Choose split mode:
                            - "Range": Select "Custom Range" to specify From/To pages, or "Fixed Range (1 to N)" to extract pages 1 to N
                            (selected range is highlighted with a green border);
                        
                            - "Pages": Select pages in the preview area below (use Shift + click to select a range);
                            (If multiple pages are selected, they will be packed into a ZIP archive);

                        4. Click "Submit and Download".
                        
                        You can cancel the compression at any time using the "Cancel Split" button.
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        allPages.clear();
        pageCards.clear();
        selectedPageIndices.clear();
        imagesFlowPane.getChildren().clear();

        ResetContext ctx = new ResetContext(
                labelSelectFileName, labelSuccess, textDragZone, null,
                dropZone, null, progressBar, true, "PDF"
        );
        reset(properties, ctx, "Selected PDF file: none");

        disableControls();
    }

    @FXML
    public void submitAndDownload() {
        if (isNull(properties.getImage())){
            Alerts.alertDialog(Alert.AlertType.WARNING,
                    "Warning",
                    "No PDF selected",
                    "Please select a PDF file to split.");
            return;
        }

        int fromPage = 0;
        int toPage = 0;
        if (rbRange.isSelected()) {
            try {
                fromPage = rbFixedRange.isSelected() ? 1 : parsePageField(tfFromPage.getText());
                toPage = parsePageField(tfToPage.getText());
            } catch (NumberFormatException _) {
                showInvalidRange("Enter a valid page range.");
                return;
            }

            if (fromPage < 1 || toPage < 1 || fromPage > toPage || toPage > allPages.size()) {
                showInvalidRange("Enter a range from page 1 to " + allPages.size()
                        + " with the start page no greater than the end page.");
                return;
            }
        } else if (selectedPageIndices.isEmpty()) {
            Alerts.alertDialog(Alert.AlertType.WARNING,
                    "Warning",
                    "No pages selected",
                    "Please select at least one page to split.");
            return;
        }

        cancelFlag.set(false);
        executeMediaTask(buildSplitTask(properties.getOutput(), fromPage, toPage));
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
        setControlsDisabled(disabled, rbRange, rbPages, rbCustomRange, rbFixedRange,
                tfFromPage, tfToPage, btnSubmit, btnReset);
        btnCancel.setDisable(true);
    }

    @Override
    protected void loadFile(File file) {
        if (!validateSelectedFile(file)) {
            return;
        }

        isPressedReset();
        properties.setImage(file);

        if (dropZone.getStyleClass().contains("drop-zone")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        Task<List<BufferedImage>> loadTask = buildLoadTask(file);

        setOnSucceededLoadTask(loadTask);
        setOnFailedLoadTask(loadTask);

        progressBar.progressProperty().bind(loadTask.progressProperty());
        startDaemonTask(loadTask);
    }

    private void setupListener() {
        splitModeGroup.selectedToggleProperty().addListener((_, _, newValue) -> {
            boolean isRange = newValue == rbRange;
            vboxRangeOptions.setVisible(isRange);
            vboxPagesOptions.setVisible(!isRange);
            refreshPreviewSelection();
            if (isRange) {
                updateRangeHighlight();
            }
        });

        rangeTypeGroup.selectedToggleProperty().addListener((_, _, newValue) -> {
            boolean isFixed = newValue == rbFixedRange;
            tfFromPage.setDisable(isFixed);
            if (isFixed) {
                tfFromPage.setText("1");
            }
            updateRangeHighlight();
        });

        tfFromPage.textProperty().addListener((_, _, newValue) -> {
            if (!newValue.matches("\\d*")) {
                tfFromPage.setText(newValue.replaceAll("\\D", ""));
            }
            if (rbRange.isSelected()) {
                updateRangeHighlight();
            }
        });

        tfToPage.textProperty().addListener((_, _, newValue) -> {
            if (!newValue.matches("\\d*")) {
                tfToPage.setText(newValue.replaceAll("\\D", ""));
            }
            if (rbRange.isSelected()) {
                updateRangeHighlight();
            }
        });
    }

    private void setupDragAndDrop() {
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
            Dragboard dragboard = event.getDragboard();
            File pdfFile = dragboard.hasFiles()
                    ? dragboard.getFiles().stream()
                    .filter(file -> file.getName().toLowerCase().endsWith(".pdf"))
                    .findFirst()
                    .orElse(null)
                    : null;
            if (nonNull(pdfFile)) {
                loadFile(pdfFile);
            }
            event.setDropCompleted(nonNull(pdfFile));
            event.consume();
        });
    }

    private Task<List<BufferedImage>> buildLoadTask(File file) {
        return new Task<>() {
            @Override
            protected List<BufferedImage> call() throws Exception {
                List<BufferedImage> images = new ArrayList<>();
                try (PDDocument document = PdfHelper.loadUnencryptedPdf(file)) {
                    PDFRenderer renderer = new PDFRenderer(document);
                    int pageCount = document.getNumberOfPages();
                    for (int i = 0; i < pageCount; i++) {
                        BufferedImage image = renderer.renderImageWithDPI(i, 72);
                        images.add(image);
                        updateProgress(i + 1L, pageCount);
                    }
                }
                return images;
            }
        };
    }

    private void addPageToList(int index, BufferedImage image) {
        PdfHelper.PageEntry entry = new PdfHelper.PageEntry(index);
        allPages.add(entry);
        createPagePreviewCard(entry, image);
    }

    private PdfPagePreviewCard getPdfPreviewCard(PdfHelper.PageEntry entry, BufferedImage image) {
        return new PdfPagePreviewCard(
                image,
                properties.getImage().getName(),
                entry.originalPageIndex,
                entry.id,
                () -> {},
                null,
                240, 270, 230, 260
        );
    }

    private void createPagePreviewCard(PdfHelper.PageEntry entry, BufferedImage image) {
        PdfPagePreviewCard card = getPdfPreviewCard(entry, image);

        card.getContainer().setOnMouseClicked(event -> {
            try {
                if (rbPages.isSelected()) {
                    if (event.isShiftDown() && !selectedPageIndices.isEmpty()) {
                        int lastIdx = -1;
                        for (int idx : selectedPageIndices) {
                            if (lastIdx == -1 || idx > lastIdx) lastIdx = idx;
                        }
                        int start = Math.min(lastIdx, entry.originalPageIndex);
                        int end = Math.max(lastIdx, entry.originalPageIndex);
                        for (int i = start; i <= end; i++) {
                            if (i >= 0 && i < pageCards.size() && !selectedPageIndices.contains(i)) {
                                togglePageSelection(i, pageCards.get(i));
                            }
                        }
                    } else {
                        togglePageSelection(entry.originalPageIndex, card);
                    }
                }
            } catch (IndexOutOfBoundsException _) {
                // Silently ignore bounds exceptions during page selection
            }
        });

        pageCards.add(card);
        imagesFlowPane.getChildren().add(card.getContainer());
    }

    private void togglePageSelection(int index, PdfPagePreviewCard card) {
        if (selectedPageIndices.contains(index)) {
            selectedPageIndices.remove(index);
            card.getContainer().setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #555;");
        } else {
            selectedPageIndices.add(index);
            card.getContainer().setStyle("-fx-background-color: #3a3a3a; -fx-border-color: #32CD32; -fx-border-width: 2px;");
        }
    }

    private void refreshPreviewSelection() {
        selectedPageIndices.clear();
        for (PdfPagePreviewCard card : pageCards) {
            card.getContainer().setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #555;");
        }
    }

    private void updateRangeHighlight() {
        if (!rbRange.isSelected() || allPages.isEmpty()) return;

        int from = 1;
        int to = 1;
        try {
            if (!tfFromPage.getText().isEmpty()) from = Integer.parseInt(tfFromPage.getText());
            if (!tfToPage.getText().isEmpty()) to = Integer.parseInt(tfToPage.getText());
        } catch (NumberFormatException _) {
            // Ignored
        }

        if (rbFixedRange.isSelected()) {
            from = 1;
        }

        for (int i = 0; i < pageCards.size(); i++) {
            int pageNum = i + 1;
            PdfPagePreviewCard card = pageCards.get(i);
            if (pageNum >= from && pageNum <= to) {
                card.getContainer().setStyle("-fx-background-color: #3a3a3a; -fx-border-color: #32CD32; -fx-border-width: 2px;");
            } else {
                card.getContainer().setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #555;");
            }
        }
    }

    private void updateUIState() {
        labelSelectFileName.setText(isNull(properties.getImage()) ? "Selected PDF file: none" : "Selected PDF: " +
                properties.getImage().getName() + " (Pages: " + allPages.size() + ")");
    }

    private Task<Boolean> buildSplitTask(File outputDir, int fromPage, int toPage) {
        boolean isRange    = rbRange.isSelected();
        boolean fixedRange = isRange && rbFixedRange.isSelected();
        File inputFile = properties.getImage();
        Set<Integer> pagesToSplit = Set.copyOf(selectedPageIndices);

        return new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                updateProgress(0, 100);
                BiConsumer<Long, Long> progress = this::updateProgress;
                checkSplitCancelled();
                File outputFile = null;
                try {
                    try (PDDocument sourceDoc = PdfHelper.loadUnencryptedPdf(inputFile)) {
                        PDFMergerUtility merger = new PDFMergerUtility();
                        if (isRange) {
                            int from = fixedRange ? 1 : fromPage;
                            outputFile = splitByRange(sourceDoc, merger, outputDir, progress, from, toPage, inputFile);
                        } else {
                            outputFile = splitByPages(sourceDoc, merger, outputDir, progress, pagesToSplit, inputFile);
                        }
                    }
                    checkSplitCancelled();
                    updateProgress(100, 100);
                    return true;
                } catch (Exception exception) {
                    if (nonNull(outputFile)) {
                        Utility.cleanupFile(outputFile);
                    }
                    throw exception;
                }
            }

            private void checkSplitCancelled() throws InterruptedException {
                if (isCancelled() || cancelFlag.get() || Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Split cancelled");
                }
            }
        };
    }

    private int parsePageField(String text) {
        if (isNull(text) || text.isBlank()) {
            throw new NumberFormatException("Page number is empty");
        }
        return Integer.parseInt(text);
    }

    private void showInvalidRange(String message) {
        Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "Invalid page range", message);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.TRUE.equals(result)) {
            Message.showSuccessText(labelSuccess, "Split successful!", properties.getHideSuccessMessageTimer());
        }
    }

    private void setOnSucceededLoadTask(Task<List<BufferedImage>> loadTask) {
        loadTask.setOnSucceeded(_ -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(1.0);
            List<BufferedImage> pageImages = loadTask.getValue();
            for (int i = 0; i < pageImages.size(); i++) {
                addPageToList(i, pageImages.get(i));
            }
            if (rbRange.isSelected()) {
                updateRangeHighlight();
            }
            enableControls();
            updateUIState();

            PauseTransition hideBar = new PauseTransition(Duration.seconds(5));
            hideBar.setOnFinished(_ -> progressBar.setProgress(0));
            hideBar.playFromStart();
        });
    }

    private void setOnFailedLoadTask(Task<List<BufferedImage>> loadTask) {
        loadTask.setOnFailed(_ -> {
            Platform.runLater(() -> Alerts.alertDialog(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Failed to load PDF",
                    loadTask.getException().getMessage(),
                    Modality.APPLICATION_MODAL
            ));
            progressBar.setProgress(0);
        });
    }
}
