package media_multitool.watermarks;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
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

    private WatermarkDragHandler dragHandler;
    private WatermarkResizeHandler resizeHandler;
    private WatermarkOverlayManager overlayManager;

    private static final float PREVIEW_DPI = 72f;
    private static final float EXPORT_DPI = 300f;

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

        listControls = List.of(btnSubmit, btnWatermarkText, btnWatermarkPhoto);

        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        cropHelper = new CropHelper(cropOverlay, imageViewPreview, new Rectangle(), scrollPaneImage, previewContainer, imageScaleSlider);

        dragHandler = new WatermarkDragHandler(imageViewPreview);
        resizeHandler = new WatermarkResizeHandler(imageViewPreview);
        overlayManager = new WatermarkOverlayManager(watermarkOverlayPane, previewContainer);

        isPressedReset();
        setupDragAndDrop(dropZone, List.of(".pdf"), this::loadFile);
        setupWatermarkInteraction();
    }

    private void setupWatermarkInteraction() {
        if (imageViewPreview == null || previewContainer == null) {
            return;
        }

        imageViewPreview.setPickOnBounds(true);

        if (watermarkOverlayPane != null) {
            overlayManager.buildOverlayElements();
        }

        initListener();
        setupHandle();
        setupMouse();
    }

    private void setupMouse() {
        imageViewPreview.setOnMousePressed(event -> {
            dragHandler.setContext(firstPagePreviewImage, currentWatermarkSettings);
            dragHandler.handleMousePressed(event);
        });
        imageViewPreview.setOnMouseDragged(dragHandler::handleMouseDragged);
        imageViewPreview.setOnMouseReleased(dragHandler::handleMouseReleased);
        imageViewPreview.setOnMouseMoved(dragHandler::handleMouseMoved);
        imageViewPreview.setOnMouseClicked(event -> {
            dragHandler.setContext(firstPagePreviewImage, currentWatermarkSettings);
            dragHandler.handleMouseClicked(event);
        });

        WatermarkOverlayManager.HandlePosition[] cornerHandles = {
                WatermarkOverlayManager.HandlePosition.TL,
                WatermarkOverlayManager.HandlePosition.TR,
                WatermarkOverlayManager.HandlePosition.BL,
                WatermarkOverlayManager.HandlePosition.BR
        };
        for (WatermarkOverlayManager.HandlePosition pos : cornerHandles) {
            javafx.scene.shape.Rectangle handle = overlayManager.getHandle(pos);
            if (handle != null) {
                attachResizeHandler(handle, pos.name());
            }
        }
    }

    private void setupHandle() {
        dragHandler.setOnUpdate(settings -> {
            dragHandler.setContext(firstPagePreviewImage, settings);
            updatePreviewWithWatermark();
            updateWatermarkOverlay();
        });
        dragHandler.setOnDragComplete(this::syncSettingsToSubWindows);

        resizeHandler.setOnUpdate(settings -> {
            resizeHandler.setContext(firstPagePreviewImage, settings);
            updatePreviewWithWatermark();
            updateWatermarkOverlay();
        });
        resizeHandler.setOnResizeComplete(this::syncSettingsToSubWindows);
    }

    private void initListener() {
        previewContainer.widthProperty().addListener((_, _, _) -> {
            if (firstPagePreviewImage != null && currentWatermarkSettings.getType() != WatermarkSettings.WatermarkType.NONE) {
                updateWatermarkOverlay();
            }
        });
        previewContainer.heightProperty().addListener((_, _, _) -> {
            if (firstPagePreviewImage != null && currentWatermarkSettings.getType() != WatermarkSettings.WatermarkType.NONE) {
                updateWatermarkOverlay();
            }
        });

        imageViewPreview.fitWidthProperty().addListener((_, _, _) -> {
            if (firstPagePreviewImage != null && currentWatermarkSettings.getType() != WatermarkSettings.WatermarkType.NONE) {
                updateWatermarkOverlay();
            }
        });
        imageViewPreview.fitHeightProperty().addListener((_, _, _) -> {
            if (firstPagePreviewImage != null && currentWatermarkSettings.getType() != WatermarkSettings.WatermarkType.NONE) {
                updateWatermarkOverlay();
            }
        });
    }

    private void attachResizeHandler(javafx.scene.shape.Rectangle handle, String handleId) {
        handle.setOnMousePressed(event -> {
            resizeHandler.setContext(firstPagePreviewImage, currentWatermarkSettings);
            resizeHandler.handleMousePressed(event, handleId);
        });
        handle.setOnMouseDragged(event -> {
            resizeHandler.setContext(firstPagePreviewImage, currentWatermarkSettings);
            resizeHandler.handleMouseDragged(event);
        });
        handle.setOnMouseReleased(resizeHandler::handleMouseReleased);
    }

    private void attachAllResizeHandlers() {
        WatermarkOverlayManager.HandlePosition[] cornerHandles = {
            WatermarkOverlayManager.HandlePosition.TL,
            WatermarkOverlayManager.HandlePosition.TR,
            WatermarkOverlayManager.HandlePosition.BL,
            WatermarkOverlayManager.HandlePosition.BR
        };
        for (WatermarkOverlayManager.HandlePosition pos : cornerHandles) {
            javafx.scene.shape.Rectangle handle = overlayManager.getHandle(pos);
            if (handle != null) {
                attachResizeHandler(handle, pos.name());
            }
        }
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
    public void onChoiceFolderForSaveFile() {
        selectOutputDirectory(btnChoiceFolderForSaveFile, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save PDF");
    }

    @FXML
    public void submitAndDownload() {
        if (pdfFile == null || imageProperties.getOutput() == null) {
            return;
        }

        if (currentWatermarkSettings.getType() == WatermarkSettings.WatermarkType.NONE) {
            showErrorMessage(labelSuccess, "Please configure a watermark first.", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            return;
        }

        WatermarkSettings settingsToSave = currentWatermarkSettings.copy();
        File inputFile = pdfFile;

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

                try (PDDocument sourceDoc = Loader.loadPDF(inputFile)) {
                    int pageCount = sourceDoc.getNumberOfPages();
                    PDFRenderer renderer = new PDFRenderer(sourceDoc);

                    PDDocument outputDoc = new PDDocument();

                    for (int i = 0; i < pageCount; i++) {
                        updateProgress(5 + (90.0 * i / pageCount), 100);

                        PDPage sourcePage = sourceDoc.getPage(i);
                        PDRectangle mediaBox = sourcePage.getMediaBox();

                        BufferedImage pageImage = renderer.renderImageWithDPI(i, EXPORT_DPI);
                        
                        WatermarkSettings scaledSettings = scaleSettingsForPage(settingsToSave, pageImage);
                        
                        BufferedImage watermarkedPage = WatermarkRenderer.applyWatermark(pageImage, scaledSettings);

                        PDPage newPage = new PDPage(mediaBox);
                        outputDoc.addPage(newPage);

                        PDImageXObject pdImage = LosslessFactory.createFromImage(outputDoc, watermarkedPage);
                        try (PDPageContentStream contentStream = new PDPageContentStream(outputDoc, newPage)) {
                            contentStream.drawImage(pdImage, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());
                        }
                    }

                    updateProgress(95, 100);
                    outputDoc.save(outputFile);
                    outputDoc.close();
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

    /**
     * Scale watermark settings from preview image coordinates to export image coordinates.
     * The preview image was rendered at PREVIEW_DPI, and the export is at EXPORT_DPI.
     * Position and size need to scale proportionally.
     */
    private WatermarkSettings scaleSettingsForPage(WatermarkSettings previewSettings, BufferedImage exportPageImage) {
        if (firstPagePreviewImage == null) {
            return previewSettings;
        }

        WatermarkSettings scaled = previewSettings.copy();

        double scaleX = (double) exportPageImage.getWidth() / firstPagePreviewImage.getWidth();
        double scaleY = (double) exportPageImage.getHeight() / firstPagePreviewImage.getHeight();
        double scale = Math.min(scaleX, scaleY);

        if (scaled.isUseCustomPosition()) {
            scaled.setPositionX(scaled.getPositionX() * scaleX);
            scaled.setPositionY(scaled.getPositionY() * scaleY);
        }

        if (scaled.getType() == WatermarkSettings.WatermarkType.IMAGE) {
            scaled.setSize(scaled.getSize() * scale);
            scaled.setSpacing(scaled.getSpacing() * scale);
        } else if (scaled.getType() == WatermarkSettings.WatermarkType.TEXT) {
            scaled.setFontSize(scaled.getFontSize() * scale);
            scaled.setSpacing(scaled.getSpacing() * scale);
        }

        return scaled;
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
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectImageName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, progressBar, true
        );
        reset(imageProperties, ctx, "Selected PDF file: none");

        firstPagePreviewImage = null;
        pdfFile = null;
        currentWatermarkSettings = new WatermarkSettings();

        if (watermarkOverlayPane != null) {
            overlayManager.clearOverlay();
        }

        if (cropHelper != null) {
            cropHelper.reset();
        }
        disableControls();
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

        try (PDDocument doc = Loader.loadPDF(selectedFile)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            firstPagePreviewImage = renderer.renderImageWithDPI(0, PREVIEW_DPI);

            if (firstPagePreviewImage == null) {
                showErrorMessage(labelSuccess, "Failed to render PDF preview.", imageProperties.getHideSuccessMessageTimer());
                return;
            }

            if (watermarkOverlayPane != null) {
                overlayManager.buildOverlayElements();
                attachAllResizeHandlers();
            }

            updatePreviewWithWatermark();
            updateWatermarkOverlay();

            if (labelPreviewPlaceholder != null) {
                labelPreviewPlaceholder.setVisible(false);
            }
        } catch (Exception e) {
            ErrorLogger.error("Failed to load PDF preview: " + e.getMessage());
            showErrorMessage(labelSuccess, "Failed to load PDF.", imageProperties.getHideSuccessMessageTimer());
        }

        if (dropZone != null && !dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);
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
        if (firstPagePreviewImage == null) {
            return;
        }

        int x = (int) (relX * firstPagePreviewImage.getWidth() - settings.getSize() / 2);
        int y = (int) (relY * firstPagePreviewImage.getHeight() - settings.getSize() / 2);

        x = Math.clamp(x, 0, firstPagePreviewImage.getWidth() - (int) settings.getSize());
        y = Math.clamp(y, 0, firstPagePreviewImage.getHeight() - (int) settings.getSize());

        settings.setPositionX(x);
        settings.setPositionY(y);
        settings.setUseCustomPosition(true);

        updateWatermarkPreview(settings);
    }

    private <T> T openWatermarkWindow(
            Stage[] stageHolder, String fxmlPath, String title,
            Button ownerButton, java.util.function.Consumer<T> controllerSetup,
            WatermarkSettings.WatermarkType expectedType, java.util.function.Consumer<T> settingsLoader
    ) {
        try {
            T controller;
            if (stageHolder[0] == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Scene scene = new Scene(loader.load());

                controller = loader.getController();
                controllerSetup.accept(controller);

                Stage stage = new Stage();
                stage.initModality(Modality.NONE);
                stage.initOwner(ownerButton.getScene().getWindow());
                stage.setTitle(title);
                stage.setScene(scene);
                stage.setMinWidth(400);
                stage.setMinHeight(400);
                stage.setOnCloseRequest(_ -> stageHolder[0] = null);
                stageHolder[0] = stage;
            } else {
                controller = null;
            }

            if (controller != null && currentWatermarkSettings.getType() == expectedType) {
                settingsLoader.accept(controller);
            }

            if (!stageHolder[0].isShowing()) {
                stageHolder[0].show();
            } else {
                stageHolder[0].toFront();
            }

            return controller;
        } catch (Exception e) {
            ErrorLogger.error("Failed to open " + title + ": " + e.getMessage());
            return null;
        }
    }

    public void handleOpenWindowWatermarkText() {
        Stage[] holder = {textWatermarkStage};
        WatermarkTextController ctrl = openWatermarkWindow(
                holder,
                "/viewses/watermark-views/window-watermark-text-pdf.fxml",
                "Text Watermark Settings",
                btnWatermarkText,
                (WatermarkTextController c) -> {
                    c.setMainPdfController(this);
                    textWatermarkController = c;
                },
                WatermarkSettings.WatermarkType.TEXT,
                c -> c.loadSettings(currentWatermarkSettings)
        );
        textWatermarkStage = holder[0];
        if (ctrl != null) textWatermarkController = ctrl;
    }

    public void handleOpenWindowWatermarkPhoto() {
        Stage[] holder = {photoWatermarkStage};
        WatermarkPhotoController ctrl = openWatermarkWindow(
                holder,
                "/viewses/watermark-views/window-watermark-photo-pdf.fxml",
                "Photo Watermark Settings",
                btnWatermarkPhoto,
                (WatermarkPhotoController c) -> {
                    c.setMainPdfController(this);
                    photoWatermarkController = c;
                },
                WatermarkSettings.WatermarkType.IMAGE,
                c -> c.loadSettings(currentWatermarkSettings)
        );
        photoWatermarkStage = holder[0];
        if (ctrl != null) photoWatermarkController = ctrl;
    }
}
