package media_multitool.watermarks;

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
    
    /**
     * Set callback for preview updates during drag
     */
    public void setOnUpdate(Consumer<WatermarkSettings> callback) {
        this.onUpdateCallback = callback;
    }
    
    /**
     * Set callback for when drag is complete
     */
    public void setOnDragComplete(Runnable callback) {
        this.onDragCompleteCallback = callback;
    }
    
    /**
     * Handle mouse press - check if click is inside watermark bounds
     */
    public void handleMousePressed(MouseEvent event) {
        if (!canDrag()) {
            return;
        }
        
        double imageViewWidth = imageView.getBoundsInLocal().getWidth();
        double imageViewHeight = imageView.getBoundsInLocal().getHeight();
        
        if (imageViewWidth <= 0 || imageViewHeight <= 0) {
            return;
        }
        
        double scaleX = image.getWidth() / imageViewWidth;
        double scaleY = image.getHeight() / imageViewHeight;
        
        double clickImageX = event.getX() * scaleX;
        double clickImageY = event.getY() * scaleY;
        
        double[] posData = WatermarkDimensionsHelper.getCurrentPosition(settings, image);
        double posX = posData[0];
        double posY = posData[1];
        double wmWidth = posData[2];
        double wmHeight = posData[3];
        
        if (clickImageX < posX - 10 || clickImageX > posX + wmWidth + 10 ||
            clickImageY < posY - 10 || clickImageY > posY + wmHeight + 10) {
            return;
        }
        
        isDragging = true;
        dragStartX = event.getX();
        dragStartY = event.getY();
        dragStartPosX = settings.getPositionX();
        dragStartPosY = settings.getPositionY();
        
        if (!settings.isUseCustomPosition()) {
            WatermarkDimensionsHelper.initDefaultPosition(settings, image);
            dragStartPosX = settings.getPositionX();
            dragStartPosY = settings.getPositionY();
        }
        
        imageView.setCursor(Cursor.CLOSED_HAND);
        event.consume();
    }
    
    /**
     * Handle mouse drag - update watermark position
     */
    public void handleMouseDragged(MouseEvent event) {
        if (!isDragging || image == null) {
            return;
        }
        
        double imageViewWidth = imageView.getBoundsInLocal().getWidth();
        double imageViewHeight = imageView.getBoundsInLocal().getHeight();
        
        if (imageViewWidth <= 0 || imageViewHeight <= 0) {
            return;
        }
        
        double scaleX = image.getWidth() / imageViewWidth;
        double scaleY = image.getHeight() / imageViewHeight;
        
        double deltaX = (event.getX() - dragStartX) * scaleX;
        double deltaY = (event.getY() - dragStartY) * scaleY;
        
        double newPosX = dragStartPosX + deltaX;
        double newPosY = dragStartPosY + deltaY;
        
        double wmWidth = WatermarkDimensionsHelper.calculateWidth(settings);
        double wmHeight = WatermarkDimensionsHelper.calculateHeight(settings);
        
        // Clamp to keep watermark fully inside image bounds
        newPosX = Math.clamp(newPosX, 0, image.getWidth() - wmWidth);
        newPosY = Math.clamp(newPosY, 0, image.getHeight() - wmHeight);
        
        settings.setPositionX(newPosX);
        settings.setPositionY(newPosY);
        settings.setUseCustomPosition(true);
        
        if (onUpdateCallback != null) {
            onUpdateCallback.accept(settings);
        }
        
        event.consume();
    }
    
    /**
     * Handle mouse release - complete drag operation
     */
    public void handleMouseReleased(MouseEvent event) {
        if (isDragging) {
            isDragging = false;
            imageView.setCursor(Cursor.OPEN_HAND);
            
            if (onDragCompleteCallback != null) {
                onDragCompleteCallback.run();
            }
            
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
        
        double imageViewWidth = imageView.getBoundsInLocal().getWidth();
        double imageViewHeight = imageView.getBoundsInLocal().getHeight();
        
        if (imageViewWidth <= 0 || imageViewHeight <= 0) {
            imageView.setCursor(Cursor.DEFAULT);
            return;
        }
        
        double scaleX = image.getWidth() / imageViewWidth;
        double scaleY = image.getHeight() / imageViewHeight;
        
        double clickImageX = event.getX() * scaleX;
        double clickImageY = event.getY() * scaleY;
        
        double[] posData = WatermarkDimensionsHelper.getCurrentPosition(settings, image);
        double posX = posData[0];
        double posY = posData[1];
        double wmWidth = posData[2];
        double wmHeight = posData[3];
        
        // Check if cursor is inside watermark bounds
        if (clickImageX >= posX && clickImageX <= posX + wmWidth &&
            clickImageY >= posY && clickImageY <= posY + wmHeight) {
            imageView.setCursor(Cursor.OPEN_HAND);
        } else {
            imageView.setCursor(Cursor.DEFAULT);
        }
    }
    
    /**
     * Handle mouse click - position watermark at click location
     */
    public void handleMouseClicked(MouseEvent event) {
        if (!canDrag() || !event.isStillSincePress()) {
            return;
        }
        
        double imageViewWidth = imageView.getBoundsInLocal().getWidth();
        double imageViewHeight = imageView.getBoundsInLocal().getHeight();
        
        if (imageViewWidth <= 0 || imageViewHeight <= 0) {
            return;
        }
        
        double scaleX = image.getWidth() / imageViewWidth;
        double scaleY = image.getHeight() / imageViewHeight;
        
        double clickX = event.getX() * scaleX;
        double clickY = event.getY() * scaleY;
        
        double wmWidth = WatermarkDimensionsHelper.calculateWidth(settings);
        double wmHeight = WatermarkDimensionsHelper.calculateHeight(settings);
        
        double posX = clickX - wmWidth / 2;
        double posY = clickY - wmHeight / 2;
        
        posX = Math.clamp(posX, 0, image.getWidth() - wmWidth);
        posY = Math.clamp(posY, 0, image.getHeight() - wmHeight);
        
        settings.setPositionX(posX);
        settings.setPositionY(posY);
        settings.setUseCustomPosition(true);
        
        if (onUpdateCallback != null) {
            onUpdateCallback.accept(settings);
        }
        
        if (onDragCompleteCallback != null) {
            onDragCompleteCallback.run();
        }
        
        event.consume();
    }
    
    /**
     * Check if watermark can be dragged
     */
    private boolean canDrag() {
        return WatermarkDimensionsHelper.canDrag(settings, image);
    }
}
