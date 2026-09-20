package media_multitool.watermarks;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import media_multitool.AbstractMediaController;
import media_multitool.watermarks.viewController.WatermarkPhotoController;
import media_multitool.watermarks.viewController.WatermarkTextController;
import model.helper.images.CropHelper;
import model.helper.watermarks.*;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.ResetContext;
import viewHelp.Alerts;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import viewHelp.OpenWatermarkWindow;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import static model.utility.PathWorker.createOutputFile;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class WatermarkPdfController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();

    @FXML private Slider imageScaleSlider;
    @FXML private ScrollPane scrollPaneImage;
    @FXML private StackPane dropZone, previewContainer;
    @FXML private Pane watermarkOverlayPane, cropOverlay;
    @FXML private Label labelSelectImageName, textDragZone, labelPreviewPlaceholder;
    @FXML private ImageView imageViewPreview;
    @FXML private Button btnSelectFile, btnChoiceFolderForSaveFile, btnWatermarkText, btnWatermarkPhoto, btnSubmit;

    private BufferedImage firstPagePreviewImage;
    private File pdfFile;
    private CropHelper cropHelper;
    private List<Control> listControls;

    private WatermarkSettings currentWatermarkSettings;
    private Stage textWatermarkStage, photoWatermarkStage;
    private WatermarkTextController textWatermarkController;
    private WatermarkPhotoController photoWatermarkController;

    private WatermarkOverlayManager overlayManager;
    private WatermarkInteractionSetup interactionSetup;

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML
    public void initialize() {
        if (imageScaleSlider == null) {
            return;
        }

        currentWatermarkSettings = new WatermarkSettings();

        listControls = List.of(btnSubmit, btnWatermarkText, btnWatermarkPhoto, btnReset);

        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        cropHelper = new CropHelper(cropOverlay, imageViewPreview, new Rectangle(), scrollPaneImage, previewContainer, imageScaleSlider);

        overlayManager = new WatermarkOverlayManager(watermarkOverlayPane, previewContainer);

        interactionSetup = new WatermarkInteractionSetup(
                imageViewPreview,
                previewContainer,
                watermarkOverlayPane,
                new WatermarkDragHandler(imageViewPreview),
                new WatermarkResizeHandler(imageViewPreview),
                overlayManager,
                () -> firstPagePreviewImage,
                () -> currentWatermarkSettings,
                this::refreshPreview,
                this::syncSettingsToSubWindows
        );

        isPressedReset();
        setupDragAndDrop(dropZone, List.of(".pdf"), this::loadFile);
        interactionSetup.setup();
    }

    private void refreshPreview() {
        updatePreviewWithWatermark();
        updateWatermarkOverlay();
    }

    private void syncSettingsToSubWindows() {
        if (photoWatermarkController != null && photoWatermarkStage != null && photoWatermarkStage.isShowing()) {
            photoWatermarkController.loadSettings(currentWatermarkSettings);
        }
        if (textWatermarkController != null && textWatermarkStage != null && textWatermarkStage.isShowing()) {
            textWatermarkController.loadSettings(currentWatermarkSettings);
        }
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Watermark PDF",
                """
                        How to use:
                        1. Select a PDF file or drag it into the drop zone.
                        \s
                        2. Click "Text" or "Photo" to open watermark settings.
                        \s
                        3. Configure watermark settings - changes appear in real-time on the first page preview.
                        \s
                        4. Click or drag on the preview to reposition the watermark.
                           Drag the handles on corners to resize.
                        \s
                        5. Click "Submit and Download" to save the watermarked PDF.
                           The watermark will be applied to ALL pages with the same settings.
                        \s
                        This tool helps you add watermarks to your PDF documents.
                        \s
                        If you have any questions or problems, please go to Info and write to me on Discord.
                        """
        );
    }

    @Override
    protected void lockUI() {
        btnSelectFile.setDisable(true);
        btnChoiceFolderForSaveFile.setDisable(true);
        btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSelectFile.setDisable(false);
        btnChoiceFolderForSaveFile.setDisable(false);
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
        SelectFile selectFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                "Select PDF file"
        ).ifPresent(this::loadFile);
    }

    @FXML
    private void onChoiceFolderForSaveFile() {
        selectOutputDirectory(btnChoiceFolderForSaveFile, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save PDF");
    }

    private boolean checks() {
        if (pdfFile == null || imageProperties.getOutput() == null) {
            return false;
        }

        if (currentWatermarkSettings.getType() == WatermarkSettings.WatermarkType.NONE) {
            showErrorMessage(labelSuccess, "Please configure a watermark first.", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            return false;
        }

        return true;
    }

    @FXML
    private void submitAndDownload() {
        if (checks()) {

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(5, 100);

                File outputFile = createOutputFile(
                        imageProperties.getImage(),
                        imageProperties.getOutput(),
                        "watermarked",
                        "pdf"
                );

                try (PDDocument sourceDoc = Loader.loadPDF(pdfFile); PDDocument outputDoc = new PDDocument()) {
                    int pageCount = sourceDoc.getNumberOfPages();

                    for (int i = 0; i < pageCount; i++) {
                        updateProgress(5 + (90.0 * i / pageCount), 100);

                        PDRectangle mediaBox = sourceDoc.getPage(i).getMediaBox();

                        PDFRenderer renderer = new PDFRenderer(sourceDoc);
                        float exportDpi = 300f;
                        BufferedImage pageImage = renderer.renderImageWithDPI(i, exportDpi);

                        WatermarkSettings scaledSettings =
                                WatermarkRenderer.scaleSettingsForExport(currentWatermarkSettings.copy(), firstPagePreviewImage, pageImage);

                        BufferedImage watermarkedPage = WatermarkRenderer.applyWatermark(pageImage, scaledSettings);

                        PDPage newPage = new PDPage(mediaBox);
                        outputDoc.addPage(newPage);

                        if (watermarkedPage == null) {
                            ErrorLogger.error("Failed to watermark page " + i);
                            continue;
                        }
                        PDImageXObject pdImage = LosslessFactory.createFromImage(outputDoc, watermarkedPage);
                        try (PDPageContentStream contentStream = new PDPageContentStream(outputDoc, newPage)) {
                            contentStream.drawImage(pdImage, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());
                        }
                    }

                    updateProgress(95, 100);
                    outputDoc.save(outputFile);
                }

                updateProgress(100, 100);
                return outputFile;
            }
        };

        executeMediaTask(task);
        if (progressBar != null) {
            progressBar.setVisible(true);
            progressBar.setManaged(true);
        }
        labelSuccess.setManaged(true);
       }
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.FALSE.equals(result)) {
            return;
        }

        File outputFile = (File) result;
        ErrorLogger.info("PDF with watermark saved successfully to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Watermarked PDF saved!", imageProperties.getHideSuccessMessageTimer());
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
    private void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectImageName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, progressBar, true, "PDF"
        );
        reset(imageProperties, ctx, "Selected PDF file: none");

        firstPagePreviewImage = null;
        pdfFile = null;
        currentWatermarkSettings = new WatermarkSettings();

        resetSubWindowControllers();

        if (watermarkOverlayPane != null) {
            overlayManager.clearOverlay();
        }

        if (cropHelper != null) {
            cropHelper.reset();
        }
        disableControls();
    }

    private void resetSubWindowControllers() {
        if (textWatermarkController != null) {
            textWatermarkController.resetToDefaults();
        }
        if (photoWatermarkController != null) {
            photoWatermarkController.resetToDefaults();
        }
    }

    private void loadFile(File selectedFile) {
        if (selectedFile == null || !selectedFile.getName().toLowerCase().endsWith(".pdf")) {
            showErrorMessage(labelSuccess, "Please select a valid PDF file.", imageProperties.getHideSuccessMessageTimer());
            return;
        }

        enableControls();
        pdfFile = selectedFile;
        imageProperties.setImage(selectedFile);

        labelSelectImageName.setText("Selected PDF: " + selectedFile.getName());
        textDragZone.setText("Selected PDF: " + selectedFile.getName());

        loadPdf(selectedFile);

        if (dropZone != null && !dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);
    }

    private void loadPdf(File selectedFile) {
        try (PDDocument doc = Loader.loadPDF(selectedFile)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            float previewDpi = 72f;
            firstPagePreviewImage = renderer.renderImageWithDPI(0, previewDpi);

            if (firstPagePreviewImage == null) {
                showErrorMessage(labelSuccess, "Failed to render PDF preview.", imageProperties.getHideSuccessMessageTimer());
                return;
            }

            interactionSetup.rebuildOverlay();

            updatePreviewWithWatermark();
            updateWatermarkOverlay();

            if (labelPreviewPlaceholder != null) {
                labelPreviewPlaceholder.setVisible(false);
            }
        } catch (Exception e) {
            ErrorLogger.error("Failed to load PDF preview: " + e.getMessage());
            showErrorMessage(labelSuccess, "Failed to load PDF.", imageProperties.getHideSuccessMessageTimer());
        }
    }

    private void updatePreviewWithWatermark() {
        if (firstPagePreviewImage == null) {
            return;
        }

        Image previewImage = WatermarkRenderer.renderPreview(firstPagePreviewImage, currentWatermarkSettings);
        if (previewImage != null && imageViewPreview != null) {
            imageViewPreview.setImage(previewImage);
        }
    }

    public void updateWatermarkPreview(WatermarkSettings settings) {
        this.currentWatermarkSettings = settings.copy();
        updatePreviewWithWatermark();
        updateWatermarkOverlay();
    }

    private void updateWatermarkOverlay() {
        overlayManager.updateOverlay(currentWatermarkSettings, firstPagePreviewImage, imageViewPreview);
    }

    public void updateWatermarkPosition(double relX, double relY, WatermarkSettings settings) {
        WatermarkDimensionsHelper.applyRelativePosition(relX, relY, settings, firstPagePreviewImage);
        updateWatermarkPreview(settings);
    }

    public void handleOpenWindowWatermarkText() {
        Stage[] holder = {textWatermarkStage};
        RecordOpenWatermarkWindow record = new RecordOpenWatermarkWindow(
                holder,
                "/viewses/watermark-views/window-watermark-text.fxml",
                "Text Watermark Settings",
                currentWatermarkSettings,
                btnWatermarkText,
                WatermarkSettings.WatermarkType.TEXT
        );

        OpenWatermarkWindow window = new OpenWatermarkWindow();

        WatermarkTextController ctrl = window.openWatermarkWindow(
                record,
                 (WatermarkTextController c) -> {
                     c.setMainPdfController(this);
                     c.setWindowTitle("Text Watermark Settings (PDF)");
                     textWatermarkController = c;
                 },
                c -> c.loadSettings(currentWatermarkSettings)
        );
        textWatermarkStage = holder[0];
        if (ctrl != null) {
            textWatermarkController = ctrl;
            updateWatermarkPreview(ctrl.getSettings());
        }
    }

    public void handleOpenWindowWatermarkPhoto() {
        Stage[] holder = {photoWatermarkStage};

        RecordOpenWatermarkWindow record = new RecordOpenWatermarkWindow(
                holder,
                "/viewses/watermark-views/window-watermark-photo.fxml",
                "Photo Watermark Settings",
                currentWatermarkSettings,
                btnWatermarkPhoto,
                WatermarkSettings.WatermarkType.IMAGE
        );

        WatermarkPhotoController ctrl = new OpenWatermarkWindow().openWatermarkWindow(
                record,
                (WatermarkPhotoController c) -> {
                    c.setMainPdfController(this);
                    c.setWindowTitle("Photo Watermark Settings (PDF)");
                    photoWatermarkController = c;
                },
                c -> c.loadSettings(currentWatermarkSettings)
        );
        photoWatermarkStage = holder[0];
        if (ctrl != null) {
            photoWatermarkController = ctrl;
            updateWatermarkPreview(ctrl.getSettings());
        }
    }
}
