package media_multitool.pdf;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import media_multitool.AbstractMediaController;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.ResetContext;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import viewHelp.Alerts;
import viewHelp.Message;
import viewHelp.PdfPagePreviewCard;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;

import static model.helper.pdf.SplitPdfHelper.*;

public class SplitPdfController extends AbstractMediaController {
    @FXML private Button btnSelectFiles, btnChoiceDirForSaveFile, btnSubmit, btnReset;
    @FXML private ProgressBar progressBar;
    @FXML private Label labelSuccess, textDragZone, labelSelectFileName;
    @FXML private FlowPane imagesFlowPane;
    @FXML private ScrollPane scrollPanePreview;
    @FXML private StackPane dropZone;

    @FXML private VBox vboxRangeOptions, vboxPagesOptions, vboxToPage;

    @FXML private RadioButton rbCustomRange, rbFixedRange, rbRange, rbPages;
    @FXML private ToggleGroup rangeTypeGroup, splitModeGroup;
    @FXML private TextField tfFromPage, tfToPage;

    private final ImageProperties imageProperties = new ImageProperties();
    private final List<PageEntry> allPages = new ArrayList<>();
    private final List<PdfPagePreviewCard> pageCards = new ArrayList<>();
    private final Set<Integer> selectedPageIndices = new HashSet<>();

    private List<Control> controls;

    private static class PageEntry {
        final int originalPageIndex;
        final String id;

        PageEntry(int index) {
            this.originalPageIndex = index;
            this.id = UUID.randomUUID().toString();
        }
    }

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML
    public void initialize() {
        controls = List.of(btnSubmit, btnReset, rbRange, rbPages, rbCustomRange, rbFixedRange, tfFromPage, tfToPage);

        setupDragAndDropMultiple();
        disableControls();
        updateUIState();
        setupListener();
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

    private void setupDragAndDropMultiple() {
        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            var db = event.getDragboard();
            if (db.hasFiles()) {
                File file = db.getFiles().getFirst();
                if (file.getName().toLowerCase().endsWith(".pdf")) {
                    loadPdfFile(file);
                }
            }
            event.setDropCompleted(true);
            event.consume();
        });
    }

    @Override
    protected void lockUI() {
        vboxRangeOptions.getScene().getRoot().setDisable(true);
    }

    @Override
    protected void unlockUI() {
        vboxRangeOptions.getScene().getRoot().setDisable(false);
    }

    @Override
    protected void disableControls() {
        controls.forEach(control -> control.setDisable(true));
    }

    @Override
    protected void enableControls() {
        controls.forEach(control -> control.setDisable(false));
    }

    @FXML
    public void onActionBtnSelectFiles() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectFiles.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                "Select PDF"
        ).ifPresent(this::loadPdfFile);
    }

    private void loadPdfFile(File file) {
        isPressedReset();
        imageProperties.setImage(file);

        if (dropZone.getStyleClass().contains("drop-zone")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        Task<List<BufferedImage>> loadTask = new Task<>() {
            @Override
            protected List<BufferedImage> call() throws Exception {
                List<BufferedImage> images = new ArrayList<>();
                try (PDDocument document = Loader.loadPDF(file)) {
                    PDFRenderer renderer = new PDFRenderer(document);
                    int pageCount = document.getNumberOfPages();
                    for (int i = 0; i < pageCount; i++) {
                        BufferedImage image = renderer.renderImageWithDPI(i, 72);
                        images.add(image);
                        updateProgress(i + 1, pageCount);
                    }
                }
                return images;
            }
        };

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

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> progressBar.setProgress(0));
                }
            }, 5000);
        });

        loadTask.setOnFailed(_ -> {
            Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Failed to load PDF", loadTask.getException().getMessage());
            progressBar.setProgress(0);
        });

        progressBar.progressProperty().bind(loadTask.progressProperty());
        new Thread(loadTask).start();
    }

    private void addPageToList(int index, BufferedImage image) {
        PageEntry entry = new PageEntry(index);
        allPages.add(entry);
        createPagePreviewCard(entry, image);
    }

    private void createPagePreviewCard(PageEntry entry, BufferedImage image) {
        PdfPagePreviewCard card = new PdfPagePreviewCard(
                image,
                imageProperties.getImage().getName(),
                entry.originalPageIndex,
                entry.id,
                () -> {},
                null,
                240, 270, 230, 260
        );

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
            } catch (IndexOutOfBoundsException e) {
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
        } catch (NumberFormatException ignored) {}

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
        labelSelectFileName.setText(imageProperties.getImage() == null ? "Selected PDF file: none" : "Selected PDF: " +
                imageProperties.getImage().getName() + " (Pages: " + allPages.size() + ")");
    }

    @FXML
    public void onActionChoiceDirForSaveFile() {
        selectOutputDirectory(btnChoiceDirForSaveFile, imageProperties.getOutput(),
                imageProperties::setOutput, "Select directory for save PDF");
    }

    @FXML
    public void submitAndDownload() {
        if (imageProperties.getImage() == null) return;

        File outputDir = imageProperties.getOutput() != null
                ? imageProperties.getOutput()
                : new File(System.getProperty("user.home"), "Desktop");

        Task<Void> splitTask = buildSplitTask(outputDir);

        splitTask.setOnSucceeded(_ -> {
            labelSuccess.setVisible(true);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> labelSuccess.setVisible(false));
                }
            }, 5000);
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            Message.showSuccessText(labelSuccess, "Split successful!", imageProperties.getHideSuccessMessageTimer());
        });

        splitTask.setOnFailed(_ -> {
            Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Split failed", splitTask.getException().getMessage());
            progressBar.setProgress(0);
        });

        progressBar.progressProperty().bind(splitTask.progressProperty());
        new Thread(splitTask).start();
    }

    private Task<Void> buildSplitTask(File outputDir) {
        boolean isRange = rbRange.isSelected();
        int fromPage = isRange ? parsePageField(tfFromPage.getText()) : 0;
        int toPage   = isRange ? parsePageField(tfToPage.getText())   : 0;
        boolean fixedRange = isRange && rbFixedRange.isSelected();

        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0, 100);
                BiConsumer<Long, Long> progress = this::updateProgress;
                try (PDDocument sourceDoc = Loader.loadPDF(imageProperties.getImage())) {
                    PDFMergerUtility merger = new PDFMergerUtility();
                    if (isRange) {
                        int from = fixedRange ? 1 : fromPage;
                        splitByRange(sourceDoc, merger, outputDir, progress, from, toPage, imageProperties.getImage());
                    } else {
                        splitByPages(sourceDoc, merger, outputDir, progress, selectedPageIndices, imageProperties.getImage());
                    }
                }
                updateProgress(100, 100);
                return null;
            }
        };
    }

    private int parsePageField(String text) {
        try {
            return text.isEmpty() ? 1 : Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    @FXML
    public void isPressedReset() {
        allPages.clear();
        pageCards.clear();
        selectedPageIndices.clear();
        imagesFlowPane.getChildren().clear();

        ResetContext ctx = new ResetContext(
                labelSelectFileName, labelSuccess, textDragZone, null,
                dropZone, null, progressBar, true
        );
        reset(imageProperties, ctx, "Selected PDF file: none");

        disableControls();
    }

    @FXML
    public void showInfo() {
        Alerts.alertDialog(Alert.AlertType.INFORMATION, "Information", "How to use Split PDF",
                """
                    How to use:
                    1. Select a PDF file.
                    2. Choose split mode:
                    Range:
                    a) Custom Range: Enter From and To page numbers.
                    b) Fixed Range: Enter N, will extract pages 1 to N.
                    Selected range will be highlighted with a green border.
                    \s
                    Pages:
                    Select pages in the preview area below. Each selected page will be saved as a separate file.
                    Use Shift + click to select a range of pages.
                    If multiple pages are selected, they will be packed into a ZIP archive.
                    \s
                    3. Click 'Submit and Download' to save.
                    """);
    }
}
