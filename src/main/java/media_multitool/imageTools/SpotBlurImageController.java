package media_multitool.imageTools;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import media_multitool.AbstractImageToolController;
import model.helper.images.BlurShapeHelper.BlurShapeVisual;
import model.helper.images.BlurShapeHelper.ResizeHandle;
import model.helper.images.BlurShapeHelper.ShapeType;
import model.helper.images.SpotBlurHelper;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.utility.Global;
import model.utility.ResetContext;
import org.jspecify.annotations.NonNull;
import viewHelp.Alerts;
import viewHelp.InfoAlert;
import viewHelp.SliderSetup;
import viewHelp.ZoomControlHelper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class SpotBlurImageController extends AbstractImageToolController {
    private final ImageProperties imageProperties = new ImageProperties();
    private final List<BlurShapeVisual> blurShapes = new ArrayList<>();
    private static final int HANDLE_SIZE = 8;

    @FXML private Button btnSubmit, btnCancel, btnUndo;
    @FXML private Button btnAddRect, btnAddOval;
    @FXML private ImageView preview;
    @FXML private StackPane previewContainer;
    @FXML private ScrollPane scrollPaneImage;
    @FXML private Slider sliderBlurIntensity, imageScaleSlider;
    @FXML private Label labelSelectImageName, labelBlurIntensity;
    @FXML private Pane blurOverlay;

    private ShapeType selectedShapeType = ShapeType.RECTANGLE;
    private ZoomControlHelper zoomControlHelper;
    private BlurShapeVisual selectedShape;
    private BlurShapeVisual currentShape;
    private ResizeHandle resizeHandle;
    private List<Control> listControls;

    private double dragOffsetX, dragOffsetY, originalEndX;
    private double originalStartX, originalStartY, originalEndY;
    private boolean isDrawing = false;

    @Override
    protected ImageProperties getImageProperties() {
        return imageProperties;
    }

    @Override
    protected ImageView getImageView() {
        return preview;
    }

    @FXML
    public void initialize() {
        listControls = List.of(sliderBlurIntensity, btnSubmit, btnSubmitAndCopy, btnAddRect, btnAddOval, btnUndo, btnReset, imageScaleSlider);
        imageProperties.setOutput(getSavedPath());

        sliderBlurIntensity.setMin(0);
        sliderBlurIntensity.setValue(1);
        sliderBlurIntensity.setMax(100);

        setupTooltips();
        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(() -> blurShapes.isEmpty() ? null : processedImage, "Spot-blurred");

        zoomControlHelper = new ZoomControlHelper(scrollPaneImage, preview, imageScaleSlider, previewContainer, 1.0, 3.0);

        SliderSetup.bindPercentageLabel(sliderBlurIntensity, labelBlurIntensity, 20);

        setupSlider();
        setupListener();

        setupImageViewInteraction();

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfo(
                "Spot Blur",
                """
                        3. Adjust the blur intensity using the slider;
                        
                        4. Click "Rectangle" or "Ellipse" and drag on the image to add blur zones;

                        5. (Optional) Move or resize added shapes, or click "Undo" to remove the last shape;

                        6. (Optional) Use the zoom slider or mouse wheel for precision;

                        7. Click "Download" to apply the effect;
                        """,
                "The effect may take a long time to complete."
        );
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, preview, progressBar, true, "image"
        );
        reset(imageProperties, ctx, "Selected image file: none");

        processedImage = null;
        originalImage = null;
        blurShapes.clear();
        currentShape = null;
        selectedShape = null;
        blurOverlay.getChildren().clear();
        sliderBlurIntensity.setValue(1);
        labelBlurIntensity.setText("100%");
        selectedShapeType = ShapeType.RECTANGLE;
        btnAddRect.setStyle("-fx-background-color: #32CD32;");
        btnAddOval.setStyle("");

        zoomControlHelper.resetZoom();
        disableControls();
    }

    @FXML
    public void onActionAddRectBlur() {
        selectedShapeType = ShapeType.RECTANGLE;
        btnAddRect.setStyle("-fx-background-color: #32CD32;");
        btnAddOval.setStyle("");
    }

    @FXML
    public void onActionAddOvalBlur() {
        selectedShapeType = ShapeType.ELLIPSE;
        btnAddOval.setStyle("-fx-background-color: #32CD32;");
        btnAddRect.setStyle("");
    }

    @FXML
    public void onActionUndo() {
        if (!blurShapes.isEmpty()) {
            blurShapes.removeLast();
            updateShapeVisuals();
            generatePreview();
        }
    }

    @FXML
    public void handleBlurIntensityChange() {
        labelBlurIntensity.setText(String.valueOf(getSliderValues()));
    }

    @FXML
    private void cancelTask() {
        cancelCurrentTask();
    }

    @Override
    protected void generatePreview() {
        if (isNull(originalImage)) {
            return;
        }

        if (blurShapes.isEmpty()) {
            processedImage = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g2d = processedImage.createGraphics();
            g2d.drawImage(originalImage, 0, 0, null);
            g2d.dispose();

            setImagePreview(processedImage, preview);
            if (nonNull(zoomControlHelper)) {
                zoomControlHelper.resetZoom();
                zoomControlHelper.updateImageSize();
            }
            return;
        }

        double imgWidth = preview.getImage().getWidth();
        double imgHeight = preview.getImage().getHeight();

        Task<BufferedImage> task = new Task<>() {
            @Override
            protected BufferedImage call() {
                updateMessage("Applying spot blur...");

                BufferedImage res = SpotBlurHelper.applyBlurShapes(
                        originalImage,
                        blurShapes,
                        getSliderValues(),
                        imgWidth,
                        imgHeight
                );

                updateProgress(1.0, 1.0);
                return res;
            }
        };

        executeMediaTask(task, "Spot blur preview");
    }

    @Override
    protected BufferedImage getFinalImageForDownload(DoubleConsumer progressUpdater) {
        double imgWidth = preview.getImage().getWidth();
        double imgHeight = preview.getImage().getHeight();

        BufferedImage blurred = SpotBlurHelper.applyBlurShapes(
                originalImage, blurShapes,
                getSliderValues(), imgWidth, imgHeight
        );

        if (isNull(blurred)) {
            throw new IllegalStateException("Spot blur processing failed or was cancelled.");
        }

        processedImage = blurred;
        return processedImage;
    }

    @Override
    protected void lockUI() {
        disableControls();
        btnSelectFile.setDisable(true);
        btnChooseSaveDirectory.setDisable(true);
        btnReset.setDisable(true);
        btnCancel.setVisible(true);
        btnCancel.setManaged(true);
    }

    @Override
    protected void unlockUI() {
        enableControls();
        btnSelectFile.setDisable(false);
        btnChooseSaveDirectory.setDisable(false);
        btnReset.setDisable(false);
        btnCancel.setVisible(false);
        btnCancel.setManaged(false);
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
        if (result instanceof BufferedImage bi) {
            processedImage = bi;
            setImagePreview(processedImage, preview);
            Platform.runLater(() -> {
                if (nonNull(progressBar)) {
                    progressBar.setProgress(1.0);
                    if (nonNull(imageProperties.getHideSuccessMessageTimer())) {
                        imageProperties.getHideSuccessMessageTimer().playFromStart();
                    } else {
                        progressBar.setVisible(false);
                        progressBar.setManaged(false);
                    }
                }
            });
            return;
        }

        super.handleTaskSuccess(result);

        if (Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info(getClass(), "Spot blur successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Spot blur image saved!", imageProperties.getHideSuccessMessageTimer());
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

    @Override
    protected void submitAndDownload() {
        if (blurShapes.isEmpty()) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "No shapes", "No blur shapes",
                    "Please add at least one blur shape before downloading.");
            return;
        }

        super.submitAndDownload();
    }

    @Override
    protected void loadFile(File file) {
        blurShapes.clear();
        currentShape = null;
        selectedShape = null;
        resizeHandle = null;
        if (nonNull(blurOverlay)) {
            blurOverlay.getChildren().clear();
        }

        super.loadFile(file);
    }

    private void setupSlider() {
        sliderBlurIntensity.setOnMouseReleased(_ -> {
            if (!blurShapes.isEmpty()) {
                generatePreview();
            }
        });

        sliderBlurIntensity.setOnTouchReleased(_ -> {
            if (!blurShapes.isEmpty()) {
                generatePreview();
            }
        });
    }

    private void setupListener() {
        previewContainer.widthProperty().addListener((_, _, _) -> {
            if (!blurShapes.isEmpty()) {
                updateShapeVisuals();
            }
        });
        previewContainer.heightProperty().addListener((_, _, _) -> {
            if (!blurShapes.isEmpty()) {
                updateShapeVisuals();
            }
        });

        previewContainer.layoutBoundsProperty().addListener((_, oldVal, newVal) -> {
            if (!blurShapes.isEmpty() && nonNull(oldVal) && !oldVal.equals(newVal)) {
                Platform.runLater(this::updateShapeVisuals);
            }
        });
    }

    private void setupImageViewInteraction() {
        if (isNull(preview)) {
            return;
        }

        blurOverlay.setOnMousePressed(this::handleMousePressed);
        blurOverlay.setOnMouseDragged(this::handleMouseDragged);
        blurOverlay.setOnMouseReleased(this::handleMouseReleased);
        blurOverlay.setOnMouseMoved(this::handleMouseMoved);
    }

     private void handleMouseMoved(MouseEvent event) {
         ResizeHandle handle = getHandleAtPosition(event.getX(), event.getY());
         if (nonNull(handle)) {
             blurOverlay.setCursor(handle.cursor);
         } else if (nonNull(SpotBlurHelper.getShapeAtPosition(event.getX(), event.getY(), preview, blurShapes))) {
             blurOverlay.setCursor(Cursor.HAND);
         } else {
             blurOverlay.setCursor(Cursor.DEFAULT);
         }
     }

     private void setupResizeHandle(ResizeHandle handle) {
         if (nonNull(handle)) {
             resizeHandle = handle;
             selectedShape = handle.shape;
             originalStartX = selectedShape.startX;
             originalStartY = selectedShape.startY;
             originalEndX = selectedShape.endX;
             originalEndY = selectedShape.endY;
         }
     }

      private void setupShapeSelection(double displayX, double displayY, double normalizedX, double normalizedY) {
          selectedShape = SpotBlurHelper.getShapeAtPosition(displayX, displayY, preview, blurShapes);
          if (nonNull(selectedShape)) {
              dragOffsetX = normalizedX - selectedShape.startX;
              dragOffsetY = normalizedY - selectedShape.startY;
              selectedShape.isSelected = true;
              updateShapeVisuals();
          }
      }

     private void startNewShape(double normalizedX, double normalizedY) {
         if (nonNull(selectedShapeType)) {
             isDrawing = true;
             currentShape = new BlurShapeVisual(selectedShapeType, normalizedX, normalizedY, normalizedX, normalizedY);
             updateShapeVisuals();
         }
     }

    private ResizeHandle getHandleAtPosition(double x, double y) {
        Bounds imageBounds = preview.localToParent(preview.getBoundsInLocal());
        for (BlurShapeVisual shape : blurShapes) {
            ResizeHandle handle = shape.getHandleAt(x, y, imageBounds);
            if (nonNull(handle))
                return handle;
        }
        return null;
    }

    private void handleMouseDragged(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        event.consume();

        Bounds imageBounds = preview.localToParent(preview.getBoundsInLocal());

        double clampedX = Math.clamp(event.getX(), imageBounds.getMinX(), imageBounds.getMaxX());
        double clampedY = Math.clamp(event.getY(), imageBounds.getMinY(), imageBounds.getMaxY());
            
        double normalizedX = (clampedX - imageBounds.getMinX()) / imageBounds.getWidth();
        double normalizedY = (clampedY - imageBounds.getMinY()) / imageBounds.getHeight();

        if (resizeSelectedShape(normalizedX, normalizedY)) { return; }
        if (moveSelectedShape(normalizedX, normalizedY))   { return; }

        if (isDrawing && nonNull(currentShape)) {
            currentShape.endX = normalizedX;
            currentShape.endY = normalizedY;
            updateShapeVisuals();
        }
    }

    private boolean moveSelectedShape(double normalizedX, double normalizedY) {
        if (isNull(selectedShape) || isDrawing) return false;

        double newStartX = normalizedX - dragOffsetX;
        double newStartY = normalizedY - dragOffsetY;

        double currentWidth = selectedShape.endX - selectedShape.startX;
        double currentHeight = selectedShape.endY - selectedShape.startY;

        selectedShape.startX = newStartX;
        selectedShape.startY = newStartY;
        selectedShape.endX = newStartX + currentWidth;
        selectedShape.endY = newStartY + currentHeight;

        SpotBlurHelper.constrainShapeToImageBounds(selectedShape);
        updateShapeVisuals();
        return true;
    }

    private void handleMousePressed(MouseEvent event) {
        if (isNull(originalImage) || event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        event.consume();

        Bounds imageBounds = preview.localToParent(preview.getBoundsInLocal());

        if (!SpotBlurHelper.isPointInsideImage(event.getX(), event.getY(), imageBounds)) {
            return;
        }

        double normalizedX = (event.getX() - imageBounds.getMinX()) / imageBounds.getWidth();
        double normalizedY = (event.getY() - imageBounds.getMinY()) / imageBounds.getHeight();

        ResizeHandle handle = getHandleAtPosition(event.getX(), event.getY());
        if (nonNull(handle)) {
            setupResizeHandle(handle);
            return;
        }

        if (nonNull(SpotBlurHelper.getShapeAtPosition(event.getX(), event.getY(), preview, blurShapes))) {
            setupShapeSelection(event.getX(), event.getY(), normalizedX, normalizedY);
            return;
        }

        startNewShape(normalizedX, normalizedY);
    }

    private boolean resizeSelectedShape(double normalizedX, double normalizedY) {
        if (isNull(resizeHandle) || isNull(selectedShape)) return false;

        double left = originalStartX;
        double top = originalStartY;
        double right = originalEndX;
        double bottom = originalEndY;

        if (resizeHandle.position == ResizeHandle.Position.TOP_LEFT ||
                resizeHandle.position == ResizeHandle.Position.LEFT ||
                resizeHandle.position == ResizeHandle.Position.BOTTOM_LEFT) {
            left = normalizedX;
        }
        if (resizeHandle.position == ResizeHandle.Position.TOP_LEFT ||
                resizeHandle.position == ResizeHandle.Position.TOP ||
                resizeHandle.position == ResizeHandle.Position.TOP_RIGHT) {
            top = normalizedY;
        }
        if (resizeHandle.position == ResizeHandle.Position.TOP_RIGHT ||
                resizeHandle.position == ResizeHandle.Position.RIGHT ||
                resizeHandle.position == ResizeHandle.Position.BOTTOM_RIGHT) {
            right = normalizedX;
        }
        if (resizeHandle.position == ResizeHandle.Position.BOTTOM_LEFT ||
                resizeHandle.position == ResizeHandle.Position.BOTTOM ||
                resizeHandle.position == ResizeHandle.Position.BOTTOM_RIGHT) {
            bottom = normalizedY;
        }

        selectedShape.startX = Math.min(left, right);
        selectedShape.startY = Math.min(top, bottom);
        selectedShape.endX   = Math.max(left, right);
        selectedShape.endY   = Math.max(top, bottom);

        updateShapeVisuals();
        return true;
    }

        private void handleMouseReleased(MouseEvent event) {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            event.consume();

            if (isDrawing && nonNull(currentShape)) {
                isDrawing = false;

                if (Math.abs(currentShape.endX - currentShape.startX) > 0.01 && 
                    Math.abs(currentShape.endY - currentShape.startY) > 0.01) {
                    blurShapes.add(currentShape);
                    generatePreview();
                }

                currentShape = null;
                updateShapeVisuals();
            } else if (nonNull(selectedShape)) {
                SpotBlurHelper.constrainShapeToImageBounds(selectedShape);
                generatePreview();
                selectedShape.isSelected = false;
                selectedShape = null;
                updateShapeVisuals();
            } else {
                blurShapes.forEach(shape -> shape.isSelected = false);
                updateShapeVisuals();
                updateShapeVisuals();
            }
            
            resizeHandle = null;
        }

    private void updateShapeVisuals() {
        SpotBlurHelper.updateShapeVisuals(blurOverlay, preview, blurShapes, currentShape, HANDLE_SIZE);
    }

    private int getSliderValues() {
        return (int) sliderBlurIntensity.getValue();
    }
}
