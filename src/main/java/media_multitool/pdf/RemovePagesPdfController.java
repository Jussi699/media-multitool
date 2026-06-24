package media_multitool.pdf;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
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

public class RemovePagesPdfController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();
    private final List<PageEntry> selectedPages = new ArrayList<>();
    private final List<PdfPagePreviewCard> pageCards = new ArrayList<>();
    
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
                    File file = files.getFirst();
                    if (file.getName().toLowerCase().endsWith(".pdf")) {
                        loadPdfFile(file);
                    }
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

        selectPdf.choiceFile(stage,
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                "Select PDF"
        ).ifPresent(this::loadPdfFile);
    }

    private void loadPdfFile(File file) {
        isPressedReset();
        imageProperties.setImage(file);
        labelSelectFileName.setText("Selected PDF: " + file.getName());
        textDragZone.setText("Selected: " + file.getName());
        
        if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
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
            List<BufferedImage> pageImages = loadTask.getValue();
            for (int i = 0; i < pageImages.size(); i++) {
                addPageToList(i, pageImages.get(i));
            }
            enableControls();
            
            PauseTransition hideBar = imageProperties.getHideSuccessMessageTimer();
            hideBar.setOnFinished(_ -> progressBar.setProgress(0));
            hideBar.playFromStart();
        });

        loadTask.setOnFailed(_ -> {
            progressBar.progressProperty().unbind();
            ErrorLogger.error("Failed to load PDF: " + loadTask.getException().getMessage());
            Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Failed to load PDF", loadTask.getException().getMessage());
            isPressedReset();
            
            progressBar.setProgress(0);
        });

        progressBar.progressProperty().bind(loadTask.progressProperty());
        
        new Thread(loadTask).start();
    }

    private void addPageToList(int index, BufferedImage image) {
        PageEntry entry = new PageEntry(index);
        selectedPages.add(entry);
        createPagePreviewCard(entry, image);
    }

    private void createPagePreviewCard(PageEntry entry, BufferedImage image) {
        PdfPagePreviewCard card = new PdfPagePreviewCard(
            image,
            imageProperties.getImage() != null ? imageProperties.getImage().getName() : "PDF",
            entry.originalPageIndex,
            entry.id,
            () -> removePage(entry.id),
                null,
                240, 270, 230, 260
        );
        pageCards.add(card);
        imagesFlowPane.getChildren().add(card.getContainer());
    }

    private void updateUIState() {
        labelSelectFileName.setText(imageProperties.getImage() == null ? "Selected PDF file: none" : "Selected PDF: " +
                imageProperties.getImage().getName() + " (Pages: " + selectedPages.size() + ")");
        
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
                "Please keep at least one page in the PDF."
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

                File inputFile = imageProperties.getImage();
                File outputDirFile = imageProperties.getOutput();
                
                String baseName = inputFile.getName().replace(".pdf", "") + "_modified";
                File outputFile = generateUniquePdfOutputFile(outputDirFile.getAbsolutePath(), baseName);

                updateProgress(30, 100);

                try (PDDocument sourceDoc = Loader.loadPDF(inputFile);
                     PDDocument targetDoc = new PDDocument()) {
                    
                    PDFMergerUtility merger = new PDFMergerUtility();
                    int total = selectedPages.size();
                    for (int i = 0; i < total; i++) {
                        try (PDDocument tempDoc = new PDDocument()) {
                            tempDoc.addPage(sourceDoc.getPage(selectedPages.get(i).originalPageIndex));
                            merger.appendDocument(targetDoc, tempDoc);
                        }
                        updateProgress(30 + (60L * (i + 1) / total), 100);
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
            ErrorLogger.info("PDF pages removed successfully! Saved to: " + outputFile.getAbsolutePath());

            Platform.runLater(() -> {
                Message.showSuccessText(labelSuccess, "PDF saved!", imageProperties.getHideSuccessMessageTimer());
                
                imageProperties.getHideSuccessMessageTimer().setOnFinished(_ -> {
                    labelSuccess.setVisible(false);
                    progressBar.setProgress(0);
                });
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
            "Remove Pages PDF",
            """
                How to use:
                1. Select a PDF file using 'Select PDF file' or drag and drop.
                2. All pages will be displayed as thumbnails.
                3. Click the X button on any page thumbnail to remove it from the resulting PDF.
                4. Click 'Submit and Download' to save the modified PDF.
                
                If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    private void removePage(String cardId) {
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
