package model.helper.watermarks;

import javafx.scene.Cursor;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class WatermarkDragHandler {
    private final ImageView imageView;
    private BufferedImage image;
    private WatermarkSettings settings;
    
    private double dragStartX, dragStartY;
    private double dragStartPosX, dragStartPosY;
    private boolean isDragging = false;
    
    private Consumer<WatermarkSettings> onUpdateCallback;
    private Runnable onDragCompleteCallback;
    
    public WatermarkDragHandler(ImageView imageView) {
        this.imageView = imageView;
    }

    public void setContext(BufferedImage image, WatermarkSettings settings) {
        this.image = image;
        this.settings = settings;
    }
    
    public void setOnUpdate(Consumer<WatermarkSettings> callback) {
        this.onUpdateCallback = callback;
    }
    
    public void setOnDragComplete(Runnable callback) {
        this.onDragCompleteCallback = callback;
    }
    
    /**
     * Compute ImageView-to-image scale factors. Returns null if view dimensions are invalid.
     */
    private double[] getImageScale() {
        double viewW = imageView.getBoundsInLocal().getWidth();
        double viewH = imageView.getBoundsInLocal().getHeight();
        if (viewW <= 0 || viewH <= 0) {
            return null;
        }
        return new double[]{image.getWidth() / viewW, image.getHeight() / viewH};
    }
    
    /**
     * Convert mouse event coordinates to image-space coordinates.
     * Returns null if scale cannot be computed.
     */
    private double[] toImageCoords(MouseEvent event) {
        double[] scale = getImageScale();
        if (scale == null) return null;
        return new double[]{event.getX() * scale[0], event.getY() * scale[1]};
    }
    
    /**
     * Handle mouse press - check if click is inside watermark bounds
     */
    public void handleMousePressed(MouseEvent event) {
        if (!canDrag()) return;
        
        double[] imgCoords = toImageCoords(event);
        if (imgCoords == null) return;
        
        double[] posData = WatermarkDimensionsHelper.getCurrentPosition(settings, image);
        double posX = posData[0], posY = posData[1];
        double wmWidth = posData[2], wmHeight = posData[3];
        
        if (imgCoords[0] < posX - 10 || imgCoords[0] > posX + wmWidth + 10 ||
            imgCoords[1] < posY - 10 || imgCoords[1] > posY + wmHeight + 10) {
            return;
        }
        
        isDragging = true;
        dragStartX = event.getX();
        dragStartY = event.getY();
        
        ensureCustomPosition();
        dragStartPosX = settings.getPositionX();
        dragStartPosY = settings.getPositionY();
        
        imageView.setCursor(Cursor.CLOSED_HAND);
        event.consume();
    }
    
    /**
     * Handle mouse drag - update watermark position
     * Optimized to compute scale factor once instead of twice
     */
    public void handleMouseDragged(MouseEvent event) {
        if (!isDragging || image == null) return;
        
        double[] scale = getImageScale();
        if (scale == null) return;
        
        double deltaX = (event.getX() - dragStartX) * scale[0];
        double deltaY = (event.getY() - dragStartY) * scale[1];
        
        double[] dims = WatermarkDimensionsHelper.calculateDimensions(settings);
        
        double newPosX = Math.clamp(dragStartPosX + deltaX, 0, image.getWidth() - dims[0]);
        double newPosY = Math.clamp(dragStartPosY + deltaY, 0, image.getHeight() - dims[1]);
        
        settings.setPositionX(newPosX);
        settings.setPositionY(newPosY);
        settings.setUseCustomPosition(true);
        
        notifyUpdate();
        event.consume();
    }
    
    /**
     * Handle mouse release - complete drag operation
     */
    public void handleMouseReleased(MouseEvent event) {
        if (isDragging) {
            isDragging = false;
            imageView.setCursor(Cursor.OPEN_HAND);
            notifyDragComplete();
            event.consume();
        }
    }
    
    /**
     * Handle mouse move - update cursor based on position
     */
    public void handleMouseMoved(MouseEvent event) {
        if (image == null || !canDrag()) {
            imageView.setCursor(Cursor.DEFAULT);
            return;
        }
        
        double[] imgCoords = toImageCoords(event);
        if (imgCoords == null) {
            imageView.setCursor(Cursor.DEFAULT);
            return;
        }
        
        double[] posData = WatermarkDimensionsHelper.getCurrentPosition(settings, image);
        boolean inside = imgCoords[0] >= posData[0] && imgCoords[0] <= posData[0] + posData[2] &&
                          imgCoords[1] >= posData[1] && imgCoords[1] <= posData[1] + posData[3];
        
        imageView.setCursor(inside ? Cursor.OPEN_HAND : Cursor.DEFAULT);
    }
    
    /**
     * Handle mouse click - position watermark at click location
     */
    public void handleMouseClicked(MouseEvent event) {
        if (!canDrag() || !event.isStillSincePress()) return;
        
        double[] imgCoords = toImageCoords(event);
        if (imgCoords == null) return;
        
        double[] dims = WatermarkDimensionsHelper.calculateDimensions(settings);
        
        double posX = Math.clamp(imgCoords[0] - dims[0] / 2, 0, image.getWidth() - dims[0]);
        double posY = Math.clamp(imgCoords[1] - dims[1] / 2, 0, image.getHeight() - dims[1]);
        
        settings.setPositionX(posX);
        settings.setPositionY(posY);
        settings.setUseCustomPosition(true);
        
        notifyUpdate();
        notifyDragComplete();
        event.consume();
    }
    
    /**
     * Ensure settings have a custom position initialized (centered if not set)
     */
    private void ensureCustomPosition() {
        if (!settings.isUseCustomPosition()) {
            WatermarkDimensionsHelper.initDefaultPosition(settings, image);
        }
    }
    
    private boolean canDrag() {
        return WatermarkDimensionsHelper.canDrag(settings, image);
    }
    
    private void notifyUpdate() {
        if (onUpdateCallback != null) {
            onUpdateCallback.accept(settings);
        }
    }
    
    private void notifyDragComplete() {
        if (onDragCompleteCallback != null) {
            onDragCompleteCallback.run();
        }
    }
}
