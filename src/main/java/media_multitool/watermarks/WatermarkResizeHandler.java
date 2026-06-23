package media_multitool.watermarks;

import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class WatermarkResizeHandler {
    private final ImageView imageView;
    private BufferedImage image;
    private WatermarkSettings settings;
    
    private String activeHandle = null;
    private double resizeStartWidth, resizeStartHeight, resizeStartPosX, resizeStartPosY, resizeStartMouseX, resizeStartMouseY;
    
    private Consumer<WatermarkSettings> onUpdateCallback;
    private Runnable onResizeCompleteCallback;
    
    public WatermarkResizeHandler(ImageView imageView) {
        this.imageView = imageView;
    }
    
    /**
     * Set current image and settings
     */
    public void setContext(BufferedImage image, WatermarkSettings settings) {
        this.image = image;
        this.settings = settings;
    }
    
    /**
     * Set callback for preview updates during resize
     */
    public void setOnUpdate(Consumer<WatermarkSettings> callback) {
        this.onUpdateCallback = callback;
    }
    
    /**
     * Set callback for when resize is complete
     */
    public void setOnResizeComplete(Runnable callback) {
        this.onResizeCompleteCallback = callback;
    }
    
    /**
     * Handle mouse press on resize handle
     */
    public void handleMousePressed(MouseEvent event, String handleId) {
        if (!canResize() || image == null) {
            return;
        }
        
        activeHandle = handleId;
        resizeStartMouseX = event.getScreenX();
        resizeStartMouseY = event.getScreenY();
        resizeStartWidth = WatermarkDimensionsHelper.calculateWidth(settings);
        resizeStartHeight = WatermarkDimensionsHelper.calculateHeight(settings);
        resizeStartPosX = settings.getPositionX();
        resizeStartPosY = settings.getPositionY();
        
        if (!settings.isUseCustomPosition()) {
            WatermarkDimensionsHelper.initDefaultPosition(settings, image);
            resizeStartPosX = settings.getPositionX();
            resizeStartPosY = settings.getPositionY();
        }
        
        event.consume();
    }
    
    /**
     * Handle mouse drag on resize handle
     */
    public void handleMouseDragged(MouseEvent event) {
        if (activeHandle == null || image == null) {
            return;
        }
        
        double imageViewWidth = imageView.getBoundsInLocal().getWidth();
        double imageViewHeight = imageView.getBoundsInLocal().getHeight();
        
        if (imageViewWidth <= 0 || imageViewHeight <= 0) {
            return;
        }
        
        double scaleX = image.getWidth() / imageViewWidth;
        double scaleY = image.getHeight() / imageViewHeight;
        
        double dxScreen = event.getScreenX() - resizeStartMouseX;
        double dyScreen = event.getScreenY() - resizeStartMouseY;
        
        double dxImage = dxScreen * scaleX;
        double dyImage = dyScreen * scaleY;
        
        boolean isImage = settings.getType() == WatermarkSettings.WatermarkType.IMAGE;
        
        ResizeResult result = calculateNewSize(dxImage, dyImage, isImage);
        
        applyResize(result, isImage, event.getScreenY());
        
        event.consume();
    }
    
    /**
     * Calculate new size based on handle and delta
     */
    private ResizeResult calculateNewSize(double dxImage, double dyImage, boolean isImage) {
        ResizeResult result = new ResizeResult();
        result.newWidth = resizeStartWidth;
        result.newHeight = resizeStartHeight;
        result.newPosX = resizeStartPosX;
        result.newPosY = resizeStartPosY;
        
        switch (activeHandle) {
            case "TL" -> {
                if (isImage) {
                    double delta = Math.max(-dxImage, -dyImage);
                    result.newWidth = Math.max(10, resizeStartWidth + delta);
                    result.newHeight = result.newWidth;
                    double diff = resizeStartWidth - result.newWidth;
                    result.newPosX = resizeStartPosX + diff;
                    result.newPosY = resizeStartPosY + diff;
                    result.changeWidth = true;
                    result.changeHeight = true;
                } else {
                    result.newHeight = Math.max(10, resizeStartHeight - dyImage);
                    result.newPosY = resizeStartPosY + (resizeStartHeight - result.newHeight);
                    result.changeHeight = true;
                }
            }
            case "TC" -> {
                result.newHeight = Math.max(10, resizeStartHeight - dyImage);
                result.newPosY = resizeStartPosY + (resizeStartHeight - result.newHeight);
                result.changeHeight = true;
            }
            case "TR" -> {
                if (isImage) {
                    double delta = Math.max(dxImage, -dyImage);
                    result.newWidth = Math.max(10, resizeStartWidth + delta);
                    result.newHeight = result.newWidth;
                    result.newPosY = resizeStartPosY + (resizeStartHeight - result.newWidth);
                    result.changeWidth = true;
                    result.changeHeight = true;
                } else {
                    result.newHeight = Math.max(10, resizeStartHeight - dyImage);
                    result.newPosY = resizeStartPosY + (resizeStartHeight - result.newHeight);
                    result.changeHeight = true;
                }
            }
            case "ML" -> {
                if (isImage) {
                    result.newWidth = Math.max(10, resizeStartWidth - dxImage);
                    result.newPosX = resizeStartPosX + (resizeStartWidth - result.newWidth);
                    result.changeWidth = true;
                }
            }
            case "MR" -> {
                if (isImage) {
                    result.newWidth = Math.max(10, resizeStartWidth + dxImage);
                    result.changeWidth = true;
                }
            }
            case "BL" -> {
                if (isImage) {
                    double delta = Math.max(-dxImage, dyImage);
                    result.newWidth = Math.max(10, resizeStartWidth + delta);
                    result.newHeight = result.newWidth;
                    result.newPosX = resizeStartPosX + (resizeStartWidth - result.newWidth);
                    result.changeWidth = true;
                    result.changeHeight = true;
                } else {
                    result.newHeight = Math.max(10, resizeStartHeight + dyImage);
                    result.changeHeight = true;
                }
            }
            case "BC" -> {
                result.newHeight = Math.max(10, resizeStartHeight + dyImage);
                result.changeHeight = true;
            }
            case "BR" -> {
                if (isImage) {
                    double delta = Math.max(dxImage, dyImage);
                    result.newWidth = Math.max(10, resizeStartWidth + delta);
                    result.newHeight = result.newWidth;
                    result.changeWidth = true;
                    result.changeHeight = true;
                } else {
                    result.newHeight = Math.max(10, resizeStartHeight + dyImage);
                    result.changeHeight = true;
                }
            }
        }
        
        return result;
    }
    
    /**
     * Apply calculated resize to settings
     */
    private void applyResize(ResizeResult result, boolean isImage, double currentMouseY) {
        if (isImage && result.changeWidth) {
            settings.setSize(result.newWidth);
        } else if (!isImage && result.changeHeight) {
            double ratio = result.newHeight / resizeStartHeight;
            double newFontSize = settings.getFontSize() * ratio;
            newFontSize = Math.clamp(newFontSize, 0.5, 10.0);
            settings.setFontSize(newFontSize);
            
            resizeStartHeight = WatermarkDimensionsHelper.calculateHeight(settings);
            resizeStartMouseY = currentMouseY;
        }
        
        result.newPosX = Math.max(0, result.newPosX);
        result.newPosY = Math.max(0, result.newPosY);
        settings.setPositionX(result.newPosX);
        settings.setPositionY(result.newPosY);
        settings.setUseCustomPosition(true);
        
        if (onUpdateCallback != null) {
            onUpdateCallback.accept(settings);
        }
    }
    
    /**
     * Handle mouse release - complete resize operation
     */
    public void handleMouseReleased(MouseEvent event) {
        if (activeHandle != null) {
            activeHandle = null;
            
            if (onResizeCompleteCallback != null) {
                onResizeCompleteCallback.run();
            }
            
            event.consume();
        }
    }
    
    /**
     * Check if watermark can be resized
     */
    private boolean canResize() {
        return WatermarkDimensionsHelper.canDrag(settings, image);
    }
    
    /**
     * Internal class to hold resize calculation results
     */
    private static class ResizeResult {
        double newWidth;
        double newHeight;
        double newPosX;
        double newPosY;
        boolean changeWidth;
        boolean changeHeight;
    }
}
