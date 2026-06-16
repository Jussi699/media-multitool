package model.helper;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static model.enums.EnumCrop.*;

public class CropHelper {
    private static final double MIN_CROP_SIZE = 10.0;
    private static final double HANDLE_SIZE = 10.0;

    private final Pane cropOverlay;
    private final ImageView imageViewPreview;
    private final Rectangle cropRect;
    private final ScrollPane scrollPanePhoto;
    private final StackPane previewContainer;

    private final Map<HandlePosition, Rectangle> handles = new EnumMap<>(HandlePosition.class);

    private BufferedImage originalBufferedImage;
    private CropArea cropArea;
    private Point2D dragStartImagePoint;
    private CropArea dragStartCrop;
    private DragMode dragMode = DragMode.NONE;
    private HandlePosition activeHandle;

    public CropHelper(Pane cropOverlay, ImageView imageViewPreview, Rectangle cropRect,
                      ScrollPane scrollPanePhoto, StackPane previewContainer,
                      Slider imageScaleSlider) {
        this.cropOverlay = cropOverlay;
        this.imageViewPreview = imageViewPreview;
        this.cropRect = cropRect;
        this.scrollPanePhoto = scrollPanePhoto;
        this.previewContainer = previewContainer;

        setup(imageScaleSlider);
    }

    private void setup(Slider imageScaleSlider) {
        cropOverlay.setManaged(false);
        cropOverlay.prefWidthProperty().bind(previewContainer.widthProperty());
        cropOverlay.prefHeightProperty().bind(previewContainer.heightProperty());
        cropOverlay.setPickOnBounds(true);

        cropRect.setFill(Color.color(0.0, 0.0, 0.0, 0.12));
        cropRect.setStroke(Color.WHITE);
        cropRect.setStrokeWidth(2.0);
        cropRect.setCursor(Cursor.MOVE);
        cropRect.setManaged(false);

        cropOverlay.setOnMousePressed(this::startNewCrop);
        cropOverlay.setOnMouseDragged(this::resizeNewCrop);
        cropOverlay.setOnMouseReleased(this::finishCropDrag);

        cropRect.setOnMousePressed(this::startMoveCrop);
        cropRect.setOnMouseDragged(this::moveCrop);
        cropRect.setOnMouseReleased(this::finishCropDrag);

        for (HandlePosition position : HandlePosition.values()) {
            Rectangle handle = createHandle(position);
            handles.put(position, handle);
            cropOverlay.getChildren().add(handle);
        }

        imageViewPreview.boundsInParentProperty().addListener((_, _, _) -> updateCropOverlay());
        imageScaleSlider.valueProperty().addListener((_, _, _) -> Platform.runLater(this::updateCropOverlay));
    }

    private Rectangle createHandle(HandlePosition position) {
        Rectangle handle = new Rectangle(HANDLE_SIZE, HANDLE_SIZE);
        handle.setFill(Color.WHITE);
        handle.setStroke(Color.rgb(40, 40, 40));
        handle.setStrokeWidth(1.0);
        handle.setCursor(position.cursor);
        handle.setManaged(false);
        handle.setVisible(false);

        handle.setOnMousePressed(event -> startResizeCrop(event, position));
        handle.setOnMouseDragged(this::resizeCropByHandle);
        handle.setOnMouseReleased(this::finishCropDrag);

        return handle;
    }

    private void startNewCrop(MouseEvent event) {
        if (!canEditCrop(event) || event.getTarget() != cropOverlay) {
            return;
        }

        imagePointFromOverlay(event).ifPresent(point -> {
            dragMode = DragMode.CREATE;
            dragStartImagePoint = point;
            cropArea = new CropArea(point.getX(), point.getY(), MIN_CROP_SIZE, MIN_CROP_SIZE);
            scrollPanePhoto.setPannable(false);
            event.consume();
            updateCropOverlay();
        });
    }

    private void resizeNewCrop(MouseEvent event) {
        if (dragMode != DragMode.CREATE || dragStartImagePoint == null) {
            return;
        }

        imagePointFromOverlay(event).ifPresent(point -> {
            double x = Math.min(dragStartImagePoint.getX(), point.getX());
            double y = Math.min(dragStartImagePoint.getY(), point.getY());
            double right = Math.max(dragStartImagePoint.getX(), point.getX());
            double bottom = Math.max(dragStartImagePoint.getY(), point.getY());
            cropArea = normalizeCrop(x, y, right - x, bottom - y);
            updateCropOverlay();
            event.consume();
        });
    }

    private void startMoveCrop(MouseEvent event) {
        if (!canEditCrop(event) || cropArea == null) {
            return;
        }

        imagePointFromOverlay(event).ifPresent(point -> {
            dragMode = DragMode.MOVE;
            dragStartImagePoint = point;
            dragStartCrop = cropArea.copy();
            scrollPanePhoto.setPannable(false);
            event.consume();
        });
    }

    private void moveCrop(MouseEvent event) {
        if (dragMode != DragMode.MOVE || dragStartCrop == null || dragStartImagePoint == null) {
            return;
        }

        imagePointFromOverlay(event).ifPresent(point -> {
            double dx = point.getX() - dragStartImagePoint.getX();
            double dy = point.getY() - dragStartImagePoint.getY();
            cropArea = normalizeCrop(dragStartCrop.x + dx, dragStartCrop.y + dy, dragStartCrop.width, dragStartCrop.height);
            updateCropOverlay();
            event.consume();
        });
    }

    private void startResizeCrop(MouseEvent event, HandlePosition position) {
        if (!canEditCrop(event) || cropArea == null) {
            return;
        }

        imagePointFromOverlay(event).ifPresent(point -> {
            dragMode = DragMode.RESIZE;
            activeHandle = position;
            dragStartImagePoint = point;
            dragStartCrop = cropArea.copy();
            scrollPanePhoto.setPannable(false);
            event.consume();
        });
    }

    private void resizeCropByHandle(MouseEvent event) {
        if (dragMode != DragMode.RESIZE || dragStartCrop == null || activeHandle == null) {
            return;
        }

        imagePointFromOverlay(event).ifPresent(point -> {
            double left = dragStartCrop.x;
            double top = dragStartCrop.y;
            double right = dragStartCrop.x + dragStartCrop.width;
            double bottom = dragStartCrop.y + dragStartCrop.height;

            if (activeHandle.changesLeft) {
                left = point.getX();
            }
            if (activeHandle.changesRight) {
                right = point.getX();
            }
            if (activeHandle.changesTop) {
                top = point.getY();
            }
            if (activeHandle.changesBottom) {
                bottom = point.getY();
            }

            double x = Math.min(left, right);
            double y = Math.min(top, bottom);
            cropArea = normalizeCrop(x, y, Math.abs(right - left), Math.abs(bottom - top));
            updateCropOverlay();
            event.consume();
        });
    }

    private void finishCropDrag(MouseEvent event) {
        dragMode = DragMode.NONE;
        activeHandle = null;
        dragStartImagePoint = null;
        dragStartCrop = null;
        scrollPanePhoto.setPannable(true);
        event.consume();
    }

    private boolean canEditCrop(MouseEvent event) {
        return event.getButton() == MouseButton.PRIMARY && originalBufferedImage != null && imageViewPreview.getImage() != null;
    }

    private Optional<Point2D> imagePointFromOverlay(MouseEvent event) {
        Bounds displayedBounds = getDisplayedImageBoundsInParent();
        if (displayedBounds == null) {
            return Optional.empty();
        }

        double parentX = Math.clamp(event.getX(), displayedBounds.getMinX(), displayedBounds.getMaxX());
        double parentY = Math.clamp(event.getY(), displayedBounds.getMinY(), displayedBounds.getMaxY());
        Point2D localPoint = imageViewPreview.parentToLocal(parentX, parentY);
        DisplayedImage displayedImage = getDisplayedImageInLocal();

        if (displayedImage == null) {
            return Optional.empty();
        }

        double imageX = (localPoint.getX() - displayedImage.x) / displayedImage.width * originalBufferedImage.getWidth();
        double imageY = (localPoint.getY() - displayedImage.y) / displayedImage.height * originalBufferedImage.getHeight();

        return Optional.of(new Point2D(
                Math.clamp(imageX, 0.0, originalBufferedImage.getWidth()),
                Math.clamp(imageY, 0.0, originalBufferedImage.getHeight())
        ));
    }

    private CropArea normalizeCrop(double x, double y, double width, double height) {
        double imageWidth = originalBufferedImage.getWidth();
        double imageHeight = originalBufferedImage.getHeight();
        double normalizedWidth = Math.clamp(width, MIN_CROP_SIZE, imageWidth);
        double normalizedHeight = Math.clamp(height, MIN_CROP_SIZE, imageHeight);
        double normalizedX = Math.clamp(x, 0.0, imageWidth - normalizedWidth);
        double normalizedY = Math.clamp(y, 0.0, imageHeight - normalizedHeight);

        return new CropArea(normalizedX, normalizedY, normalizedWidth, normalizedHeight);
    }

    private Bounds imageCropToParentBounds(CropArea area) {
        DisplayedImage displayedImage = getDisplayedImageInLocal();
        if (displayedImage == null) {
            return null;
        }

        double localX = displayedImage.x + area.x / originalBufferedImage.getWidth() * displayedImage.width;
        double localY = displayedImage.y + area.y / originalBufferedImage.getHeight() * displayedImage.height;
        double localRight = displayedImage.x + (area.x + area.width) / originalBufferedImage.getWidth() * displayedImage.width;
        double localBottom = displayedImage.y + (area.y + area.height) / originalBufferedImage.getHeight() * displayedImage.height;

        Point2D topLeft = imageViewPreview.localToParent(localX, localY);
        Point2D bottomRight = imageViewPreview.localToParent(localRight, localBottom);

        return new javafx.geometry.BoundingBox(
                topLeft.getX(),
                topLeft.getY(),
                bottomRight.getX() - topLeft.getX(),
                bottomRight.getY() - topLeft.getY()
        );
    }

    private Bounds getDisplayedImageBoundsInParent() {
        DisplayedImage displayedImage = getDisplayedImageInLocal();
        if (displayedImage == null) {
            return null;
        }

        Point2D topLeft = imageViewPreview.localToParent(displayedImage.x, displayedImage.y);
        Point2D bottomRight = imageViewPreview.localToParent(
                displayedImage.x + displayedImage.width,
                displayedImage.y + displayedImage.height
        );

        return new javafx.geometry.BoundingBox(
                topLeft.getX(),
                topLeft.getY(),
                bottomRight.getX() - topLeft.getX(),
                bottomRight.getY() - topLeft.getY()
        );
    }

    private DisplayedImage getDisplayedImageInLocal() {
        Image image = imageViewPreview.getImage();
        if (image == null) {
            return null;
        }

        Bounds localBounds = imageViewPreview.getBoundsInLocal();
        double ratio = Math.min(localBounds.getWidth() / image.getWidth(), localBounds.getHeight() / image.getHeight());
        double displayedWidth = image.getWidth() * ratio;
        double displayedHeight = image.getHeight() * ratio;
        double offsetX = (localBounds.getWidth() - displayedWidth) / 2.0;
        double offsetY = (localBounds.getHeight() - displayedHeight) / 2.0;

        return new DisplayedImage(offsetX, offsetY, displayedWidth, displayedHeight);
    }

    private void positionHandles(Bounds bounds) {
        placeHandle(HandlePosition.TOP_LEFT, bounds.getMinX(), bounds.getMinY());
        placeHandle(HandlePosition.TOP, bounds.getMinX() + bounds.getWidth() / 2.0, bounds.getMinY());
        placeHandle(HandlePosition.TOP_RIGHT, bounds.getMaxX(), bounds.getMinY());
        placeHandle(HandlePosition.RIGHT, bounds.getMaxX(), bounds.getMinY() + bounds.getHeight() / 2.0);
        placeHandle(HandlePosition.BOTTOM_RIGHT, bounds.getMaxX(), bounds.getMaxY());
        placeHandle(HandlePosition.BOTTOM, bounds.getMinX() + bounds.getWidth() / 2.0, bounds.getMaxY());
        placeHandle(HandlePosition.BOTTOM_LEFT, bounds.getMinX(), bounds.getMaxY());
        placeHandle(HandlePosition.LEFT, bounds.getMinX(), bounds.getMinY() + bounds.getHeight() / 2.0);
    }

    private void placeHandle(HandlePosition position, double centerX, double centerY) {
        Rectangle handle = handles.get(position);
        handle.setX(centerX - HANDLE_SIZE / 2.0);
        handle.setY(centerY - HANDLE_SIZE / 2.0);
    }

    public void createDefaultCrop() {
        if (originalBufferedImage == null) {
            return;
        }

        double width = originalBufferedImage.getWidth() * 0.8;
        double height = originalBufferedImage.getHeight() * 0.8;
        cropArea = normalizeCrop(
                (originalBufferedImage.getWidth() - width) / 2.0,
                (originalBufferedImage.getHeight() - height) / 2.0,
                width,
                height
        );
        updateCropOverlay();
    }

    public void setOriginalBufferedImage(BufferedImage originalBufferedImage) {
        this.originalBufferedImage = originalBufferedImage;
    }

    public void reset() {
        cropArea = null;
        cropRect.setVisible(false);
        handles.values().forEach(handle -> handle.setVisible(false));
    }

    public CropArea getCropArea() {
        return cropArea;
    }

    public record CropArea(double x, double y, double width, double height) {
        public CropArea copy() {
            return new CropArea(x, y, width, height);
        }
    }

    private record DisplayedImage(double x, double y, double width, double height) {
    }

    public void updateCropOverlay() {
        boolean hasCrop = cropArea != null && originalBufferedImage != null && imageViewPreview.getImage() != null;
        cropRect.setVisible(hasCrop);

        handles.values().forEach(handle -> handle.setVisible(hasCrop));

        if (!hasCrop) {
            return;
        }

        Bounds cropBounds = imageCropToParentBounds(cropArea);
        if (cropBounds == null) {
            return;
        }

        cropRect.setX(cropBounds.getMinX());
        cropRect.setY(cropBounds.getMinY());
        cropRect.setWidth(cropBounds.getWidth());
        cropRect.setHeight(cropBounds.getHeight());

        positionHandles(cropBounds);
    }

    public double getImageWidth() {
        if (originalBufferedImage != null) {
            return originalBufferedImage.getWidth();
        }
        return 0.0;
    }

    public double getImageHeight() {
        if (originalBufferedImage != null) {
            return originalBufferedImage.getHeight();
        }
        return 0.0;
    }

    public void setupAspectRatio(double ratioWidth, double ratioHeight) {
        if (cropArea == null || originalBufferedImage == null || ratioWidth <= 0 || ratioHeight <= 0) return;

        double imageWidth = originalBufferedImage.getWidth();
        double imageHeight = originalBufferedImage.getHeight();
        double targetRatio = ratioWidth / ratioHeight;

        double centerX = cropArea.x + cropArea.width / 2.0;
        double centerY = cropArea.y + cropArea.height / 2.0;

        double newWidth = cropArea.width;
        double newHeight = newWidth / targetRatio;

        if (newHeight > imageHeight) {
            newHeight = imageHeight;
            newWidth = newHeight * targetRatio;
        }

        if (newWidth > imageWidth) {
            newWidth = imageWidth;
            newHeight = newWidth / targetRatio;
        }

        if (newWidth < MIN_CROP_SIZE) {
            newWidth = MIN_CROP_SIZE;
            newHeight = newWidth / targetRatio;
        }
        if (newHeight < MIN_CROP_SIZE) {
            newHeight = MIN_CROP_SIZE;
            newWidth = newHeight * targetRatio;
        }

        double newX = centerX - newWidth / 2.0;
        double newY = centerY - newHeight / 2.0;

        cropArea = normalizeCrop(newX, newY, newWidth, newHeight);
        updateCropOverlay();
    }
}
