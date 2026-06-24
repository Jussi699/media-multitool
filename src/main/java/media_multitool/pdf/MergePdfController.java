package media_multitool.pdf;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import media_multitool.AbstractMediaController;
import model.logger.ErrorLogger;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static model.utility.PathWorker.generateUniquePdfOutputFile;
import static model.utility.PathWorker.getSavedPath;

public class MergePdfController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();
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

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML private FlowPane imagesFlowPane;
    @FXML private ScrollPane scrollPanePreview;
    @FXML private StackPane dropZone;
    @FXML private Button btnSelectFiles, btnChoiceDirForSaveFile, btnSubmit;
    @FXML private Label labelSelectFileName, textDragZone;

    @FXML
    public void initialize() {
        imageProperties.setOutput(getSavedPath());
        setupDragAndDropMultiple();
        isPressedReset();
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
                if (!files.isEmpty()) {
                    files.stream()
                         .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
                         .forEach(this::loadPdfFile);
                }
            }
            event.consume();
        });
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
        btnSubmit.setDisable(true);
    }

    @Override
    protected void enableControls() {
        btnSubmit.setDisable(false);
    }

    @FXML
    public void onActionBtnSelectFiles() {
        SelectFile selectPdf = new SelectFile();
        Stage stage = (Stage) btnSelectFiles.getScene().getWindow();

        selectPdf.showOpenMultipleDialog(stage,
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                "Select PDF"
        ).ifPresent(page -> page.forEach(this::loadPdfFile));
    }

    private void loadPdfFile(File file) {
        if (imageProperties.getImage() == null) {
            imageProperties.setImage(file);
        }
        labelSelectFileName.setText("Selected PDFs: " + (!selectedPages.isEmpty() ? "Multiple files" : file.getName()));
        textDragZone.setText("Files added");
        
        if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        Task<BufferedImage> loadTask = new Task<>() {
            @Override
            protected BufferedImage call() throws Exception {
                try (PDDocument document = Loader.loadPDF(file)) {
                    PDFRenderer renderer = new PDFRenderer(document);
                    if (document.getNumberOfPages() > 0) {
                        return renderer.renderImageWithDPI(0, 72);
                    }
                }
                return null;
            }
        };

        loadTask.setOnSucceeded(_ -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(1.0);
            BufferedImage firstPage = loadTask.getValue();
            if (firstPage != null) {
                PageEntry entry = new PageEntry(file, 0);
                selectedPages.add(entry);
                createPagePreviewCard(entry, firstPage);
            }
            enableControls();
            updateUIState();
            
            PauseTransition hideBar = new PauseTransition(Duration.seconds(5));
            hideBar.setOnFinished(_ -> progressBar.setProgress(0));
            hideBar.playFromStart();
        });

        loadTask.setOnFailed(_ -> {
            progressBar.progressProperty().unbind();
            ErrorLogger.error("Failed to load PDF: " + loadTask.getException().getMessage());
            Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Failed to load PDF", loadTask.getException().getMessage());
            
            progressBar.setProgress(0);
        });

        progressBar.progressProperty().bind(loadTask.progressProperty());
        
        new Thread(loadTask).start();
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
        labelSelectFileName.setText(imageProperties.getImage() == null
                ? "Selected PDF file: none" : "Selected PDF: " + imageProperties.getImage().getName() + " (Pages: " + selectedPages.size() + ")");
        
        if (!selectedPages.isEmpty()) {
            enableControls();
        } else {
            disableControls();
        }
    }

    @FXML
    public void onActionChoiceDirForSaveFile() {
        selectOutputDirectory(btnChoiceDirForSaveFile, imageProperties.getOutput(), 
            imageProperties::setOutput, "Select directory for save PDF");
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

        if (imageProperties.getOutput() == null) {
            imageProperties.setOutput(getSavedPath());
        }

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(0, 100);

                File outputDirFile = imageProperties.getOutput();
                String baseName = "merged_document";
                if (!selectedPages.isEmpty()) {
                    baseName = selectedPages.getFirst().sourceFile.getName().replace(".pdf", "") + "_merged";
                }
                File outputFile = generateUniquePdfOutputFile(outputDirFile.getAbsolutePath(), baseName);

                try (PDDocument targetDoc = new PDDocument()) {
                    int totalFiles = selectedPages.size();
                    PDFMergerUtility merger = new PDFMergerUtility();
                    
                    for (int i = 0; i < totalFiles; i++) {
                        PageEntry entry = selectedPages.get(i);
                        try (PDDocument sourceDoc = Loader.loadPDF(entry.sourceFile)) {
                            merger.appendDocument(targetDoc, sourceDoc);
                        }
                        updateProgress(i + 1, totalFiles);
                    }
                    targetDoc.save(outputFile);
                }

                updateProgress(100, 100);
                return outputFile;
            }
        };

        executeMediaTask(task);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (result instanceof File outputFile) {
            ErrorLogger.info("PDF merged successfully! Saved to: " + outputFile.getAbsolutePath());

            Platform.runLater(() -> {
                Message.showSuccessText(labelSuccess, "PDF saved!", imageProperties.getHideSuccessMessageTimer());
                
                PauseTransition hideBar = new PauseTransition(Duration.seconds(5));
                hideBar.setOnFinished(_ -> {
                    labelSuccess.setVisible(false);
                    progressBar.setProgress(0);
                });
                hideBar.play();
            });
        }
    }

    @FXML
    public void isPressedReset() {
        selectedPages.clear();
        pageCards.clear();
        imagesFlowPane.getChildren().clear();
        imageProperties.setImage(null);
        
        ResetContext ctx = new ResetContext(
            labelSelectFileName, labelSuccess, textDragZone, null,
            dropZone, null, progressBar, true
        );
        if (progressBar != null) {
            progressBar.progressProperty().unbind();
        }
        reset(imageProperties, ctx, "Selected PDF file: none");

        disableControls();
        textDragZone.setText("Drag PDF here");
        if (dropZone != null) {
            dropZone.getStyleClass().remove("drop-zone-filled");
        }
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
            Alert.AlertType.INFORMATION,
            "Information",
            "Merge PDF",
            """
                How to use:
                1. Select PDF files using 'Select PDF files' or drag and drop.
                2. Each file will be represented by a thumbnail of its first page.
                3. Drag and drop thumbnails to reorder the PDF files.
                4. Click the X button on any thumbnail to remove that file from the merge.
                5. Click 'Submit and Download' to save the merged PDF.
                
                If you have any questions or problems, please go to Info and write to me on Discord."""
        );
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

    private void refreshPreview() {
        imagesFlowPane.getChildren().clear();
        for (PdfPagePreviewCard card : pageCards) {
            imagesFlowPane.getChildren().add(card.getContainer());
        }
    }
}
