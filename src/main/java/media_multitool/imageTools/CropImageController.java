package media_multitool.imageTools;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import media_multitool.AbstractImageToolController;
import model.enums.AspectRatio;
import model.helper.images.CropHelper;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.utility.Global;
import model.utility.ResetContext;
import org.jspecify.annotations.NonNull;
import viewHelp.InfoAlert;
import viewHelp.ZoomControlHelper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.function.DoubleConsumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class CropImageController extends AbstractImageToolController {
    private final ImageProperties imageProperties = new ImageProperties();

    @FXML private Button btnAspectRatioSquare, btnAspectRatio9x16, btnAspectRatio16x9;
    @FXML private Button btnAspectRatio4x5, btnAspectRatio5x4, btnAspectRatio3x4;
    @FXML private Button btnAspectRatio4x3, btnAspectRatio2x3, btnAspectRatio3x2;
    @FXML private Button btnAspectRatio5x7,btnAspectRatio7x5, btnAspectRatio1x2;
    @FXML private Button btnAspectRatio2x1, btnSubmit;
    @FXML private ImageView imageViewPreview;
    @FXML private StackPane previewContainer;
    @FXML private ScrollPane scrollPaneImage;
    @FXML private Slider imageScaleSlider;
    @FXML private Rectangle cropRect;
    @FXML private Pane cropOverlay;

    private ZoomControlHelper zoomControlHelper;
    private CropHelper cropHelper;
    private List<Control> listControls;

    @Override
    protected ImageProperties getImageProperties() {
        return imageProperties;
    }

    @Override
    protected ImageView getImageView() {
        return imageViewPreview;
    }

    @FXML
    public void initialize() {
        listControls = List.of(
                btnAspectRatioSquare, btnAspectRatio9x16, btnAspectRatio16x9, btnAspectRatio4x5,
                btnAspectRatio3x4, btnAspectRatio5x4, btnAspectRatio4x3, btnAspectRatio2x3,
                btnAspectRatio3x2, btnAspectRatio5x7, btnAspectRatio7x5, btnAspectRatio1x2,
                btnAspectRatio2x1, imageScaleSlider, btnSubmit, btnSubmitAndCopy, btnReset
        );

        imageProperties.setOutput(getSavedPath());

        setupTooltips();
        setupClearMessageTimer(labelSuccess, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(
                () -> {
                    CropHelper.CropArea cropArea = isNull(cropHelper) ? null : cropHelper.getCropArea();
                    return isNull(cropArea) ? null : createCroppedImage(cropArea);
                },
                "Cropped"
        );

        zoomControlHelper = new ZoomControlHelper(scrollPaneImage, imageViewPreview, imageScaleSlider, previewContainer, 1.0, 3.0);
        cropHelper = new CropHelper(cropOverlay, imageViewPreview, cropRect, scrollPaneImage, previewContainer, imageScaleSlider);

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfo(
                "Crop Image",
                """
                        3. Drag and resize the crop box on the image, or select an aspect ratio preset;

                        4. (Optional) Use the zoom slider or mouse wheel for precision;

                        5. Click "Crop and Download";
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, progressBar, true, "image"
        );
        reset(imageProperties, ctx, "Selected image file: none");

        originalImage  = null;
        processedImage = null;

        if (nonNull(cropHelper)) {
            cropHelper.reset();
        }

        if (nonNull(zoomControlHelper)) {
            zoomControlHelper.resetZoom();
        }
        disableControls();
    }

    @Override
    protected void generatePreview() {
        if (isNull(originalImage)) {
            return;
        }

        processedImage = originalImage;
        setImagePreview(processedImage, imageViewPreview);

        if (nonNull(zoomControlHelper)) {
            zoomControlHelper.resetZoom();
        }

        Platform.runLater(() -> {
            if (nonNull(zoomControlHelper)) {
                zoomControlHelper.updateImageSize();
            }
            if (nonNull(cropHelper)) {
                cropHelper.setOriginalBufferedImage(originalImage);
                cropHelper.createDefaultCrop();
                cropHelper.updateCropOverlay();
            }
        });
    }

    @Override
    protected BufferedImage getFinalImageForDownload(DoubleConsumer progressUpdater) {
        if (isNull(originalImage)) {
            throw new IllegalStateException("Original image is null");
        }

        CropHelper.CropArea cropArea = cropHelper.getCropArea();
        if (isNull(cropArea)) {
            throw new IllegalStateException("Select crop area first.");
        }

        return createCroppedImage(cropArea);
    }

    @Override
    protected void lockUI() {
        btnSelectFile.setDisable(true);
        btnChooseSaveDirectory.setDisable(true);
        btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSelectFile.setDisable(false);
        btnChooseSaveDirectory.setDisable(false);
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

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.FALSE.equals(result)) {
            return;
        }

        File outputFile = (File) result;
        ErrorLogger.info(getClass(), "Image cropped successfully to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Cropped image saved!", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @Override
    protected void handleTaskFailure(@NonNull Throwable exception) {
        super.handleTaskFailure(exception);
        Platform.runLater(() -> {
            showErrorMessage(labelSuccess, "Error: " + exception.getMessage(), imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    private BufferedImage createCroppedImage(CropHelper.CropArea cropArea) {
        if (isNull(originalImage) || isNull(cropArea)) {
            return null;
        }

        int x      = Math.clamp((int) Math.floor(cropArea.x()), 0, originalImage.getWidth() - 1);
        int y      = Math.clamp((int) Math.floor(cropArea.y()), 0, originalImage.getHeight() - 1);
        int width  = Math.clamp((int) Math.round(cropArea.width()), 1, originalImage.getWidth() - x);
        int height = Math.clamp((int) Math.round(cropArea.height()), 1, originalImage.getHeight() - y);
        return copyImage(originalImage.getSubimage(x, y, width, height));
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    public void onActionSelectAspectRatio(ActionEvent event) {
        Button btn = (Button) event.getSource();

        AspectRatio ratio = switch (btn.getId()) {
            case "btnAspectRatioSquare" -> AspectRatio.TO_SQUARE;
            case "btnAspectRatio9x16"   -> AspectRatio.TO_9X16;
            case "btnAspectRatio16x9"   -> AspectRatio.TO_16X9;
            case "btnAspectRatio4x5"    -> AspectRatio.TO_4X5;
            case "btnAspectRatio5x4"    -> AspectRatio.TO_5X4;
            case "btnAspectRatio3x4"    -> AspectRatio.TO_3X4;
            case "btnAspectRatio4x3"    -> AspectRatio.TO_4X3;
            case "btnAspectRatio2x3"    -> AspectRatio.TO_2X3;
            case "btnAspectRatio3x2"    -> AspectRatio.TO_3X2;
            case "btnAspectRatio5x7"    -> AspectRatio.TO_5X7;
            case "btnAspectRatio7x5"    -> AspectRatio.TO_7X5;
            case "btnAspectRatio1x2"    -> AspectRatio.TO_1X2;
            case "btnAspectRatio2x1"    -> AspectRatio.TO_2X1;
            default -> throw new IllegalArgumentException("Unexpected value: " + btn.getId());
        };

        cropHelper.setupAspectRatio(ratio);
    }
}
