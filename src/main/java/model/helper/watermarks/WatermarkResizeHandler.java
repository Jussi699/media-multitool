package model.helper.watermarks;

import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class WatermarkResizeHandler {
    private final ImageView imageView;
    private BufferedImage image;
    private WatermarkSettings settings;
    
    private String activeHandle = null;
    private double resizeStartWidth, resizeStartHeight;
    private double resizeStartPosX, resizeStartPosY;
    private double resizeStartMouseX, resizeStartMouseY;
    
    private Consumer<WatermarkSettings> onUpdateCallback;
    private Runnable onResizeCompleteCallback;
    
    public WatermarkResizeHandler(ImageView imageView) {
        this.imageView = imageView;
    }

    public void setContext(BufferedImage image, WatermarkSettings settings) {
        this.image = image;
        this.settings = settings;
    }
    
    public void setOnUpdate(Consumer<WatermarkSettings> callback) {
        this.onUpdateCallback = callback;
    }
    
    public void setOnResizeComplete(Runnable callback) {
        this.onResizeCompleteCallback = callback;
    }
    
    /**
     * Compute ImageView-to-image scale factors. Returns null if view dimensions are invalid.
     */
    private double[] getImageScale() {
        double viewW = imageView.getBoundsInLocal().getWidth();
        double viewH = imageView.getBoundsInLocal().getHeight();
        if (viewW <= 0 || viewH <= 0) return null;
        return new double[]{image.getWidth() / viewW, image.getHeight() / viewH};
    }
    
    /**
     * Handle mouse press on resize handle
     */
    public void handleMousePressed(MouseEvent event, String handleId) {
        if (!canResize() || image == null) return;
        
        activeHandle = handleId;
        resizeStartMouseX = event.getScreenX();
        resizeStartMouseY = event.getScreenY();
        
        double[] dims = WatermarkDimensionsHelper.calculateDimensions(settings);
        resizeStartWidth = dims[0];
        resizeStartHeight = dims[1];
        
        if (!settings.isUseCustomPosition()) {
            WatermarkDimensionsHelper.initDefaultPosition(settings, image);
        }
        resizeStartPosX = settings.getPositionX();
        resizeStartPosY = settings.getPositionY();
        
        event.consume();
    }
    
    /**
     * Handle mouse drag on resize handle
     */
    public void handleMouseDragged(MouseEvent event) {
        if (activeHandle == null || image == null) return;
        
        double[] scale = getImageScale();
        if (scale == null) return;
        
        double dxImage = (event.getScreenX() - resizeStartMouseX) * scale[0];
        double dyImage = (event.getScreenY() - resizeStartMouseY) * scale[1];
        
        boolean isImage = settings.getType() == WatermarkSettings.WatermarkType.IMAGE;
        
        double newWidth      = resizeStartWidth;
        double newHeight     = resizeStartHeight;
        double newPosX       = resizeStartPosX;
        double newPosY       = resizeStartPosY;
        boolean changeWidth  = false;
        boolean changeHeight = false;
        
        switch (activeHandle) {
            case "TL" -> {
                if (isImage) {
                    double delta = Math.max(-dxImage, -dyImage);
                    newWidth = clampSize(resizeStartWidth + delta);
                    newHeight = newWidth;
                    double diff = resizeStartWidth - newWidth;
                    newPosX = resizeStartPosX + diff;
                    newPosY = resizeStartPosY + diff;
                    changeWidth = changeHeight = true;
                } else {
                    newHeight = clampSize(resizeStartHeight - dyImage);
                    newPosY = resizeStartPosY + (resizeStartHeight - newHeight);
                    changeHeight = true;
                }
            }
            case "TR" -> {
                if (isImage) {
                    double delta = Math.max(dxImage, -dyImage);
                    newWidth = clampSize(resizeStartWidth + delta);
                    newHeight = newWidth;
                    newPosY = resizeStartPosY + (resizeStartHeight - newWidth);
                    changeWidth = changeHeight = true;
                } else {
                    newHeight = clampSize(resizeStartHeight - dyImage);
                    newPosY = resizeStartPosY + (resizeStartHeight - newHeight);
                    changeHeight = true;
                }
            }
            case "BL" -> {
                if (isImage) {
                    double delta = Math.max(-dxImage, dyImage);
                    newWidth = clampSize(resizeStartWidth + delta);
                    newHeight = newWidth;
                    newPosX = resizeStartPosX + (resizeStartWidth - newWidth);
                    changeWidth = changeHeight = true;
                } else {
                    newHeight = clampSize(resizeStartHeight + dyImage);
                    changeHeight = true;
                }
            }
            case "BR" -> {
                if (isImage) {
                    double delta = Math.max(dxImage, dyImage);
                    newWidth = clampSize(resizeStartWidth + delta);
                    newHeight = newWidth;
                    changeWidth = changeHeight = true;
                } else {
                    newHeight = clampSize(resizeStartHeight + dyImage);
                    changeHeight = true;
                }
            }
        }
        
        if (isImage && changeWidth) {
            settings.setSize(newWidth);
        } else if (!isImage && changeHeight) {
            double ratio = newHeight / resizeStartHeight;
            double newFontSize = Math.clamp(settings.getFontSize() * ratio, 0.5, 10.0);
            settings.setFontSize(newFontSize);
            
            resizeStartHeight = WatermarkDimensionsHelper.calculateHeight(settings);
            resizeStartMouseY = event.getScreenY();
        }
        
        settings.setPositionX(Math.max(0, newPosX));
        settings.setPositionY(Math.max(0, newPosY));
        settings.setUseCustomPosition(true);
        
        if (onUpdateCallback != null) {
            onUpdateCallback.accept(settings);
        }
        
        event.consume();
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
    
    private boolean canResize() {
        return WatermarkDimensionsHelper.canDrag(settings, image);
    }
    
    private static double clampSize(double value) {
        int min_size = 10;
        return Math.max(min_size, value);
    }
}
