package model.helper.watermarks;

import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

public class WatermarkResizeHandler {
    private final ImageView imageView;
    private BufferedImage image;
    private WatermarkSettings settings;
    
    private String activeHandle = null;
    private double resizeStartWidth, resizeStartHeight;
    private double resizeStartPosX, resizeStartPosY;
    private double resizeStartScaleX, resizeStartScaleY;
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
        double[] mousePosition = toImageCoords(event);
        if (mousePosition == null) {
            activeHandle = null;
            return;
        }
        resizeStartMouseX = mousePosition[0];
        resizeStartMouseY = mousePosition[1];
        
        double[] dims = WatermarkDimensionsHelper.calculateDimensions(settings, image);
        resizeStartWidth = dims[0];
        resizeStartHeight = dims[1];
        resizeStartScaleX = settings.getResizeScaleX();
        resizeStartScaleY = settings.getResizeScaleY();
        
        if (!settings.isUseCustomPosition()) {
            WatermarkDimensionsHelper.initDefaultPosition(settings, image);
        }
        resizeStartPosX = settings.getPositionX();
        resizeStartPosY = settings.getPositionY();
        
        event.consume();
    }
    
    /**
     * Handle mouse drag on a resize handle
     */
    public void handleMouseDragged(MouseEvent event) {
        if (activeHandle == null || image == null) return;
        
        double[] mousePosition = toImageCoords(event);
        if (mousePosition == null) return;
        double dxImage = mousePosition[0] - resizeStartMouseX;
        double dyImage = mousePosition[1] - resizeStartMouseY;
        
        boolean isImage = settings.getType() == WatermarkSettings.WatermarkType.IMAGE;
        
        double newWidth      = resizeStartWidth;
        double newHeight     = resizeStartHeight;
        double newPosX       = resizeStartPosX;
        double newPosY       = resizeStartPosY;
        boolean changeWidth  = false;
        boolean changeHeight = false;
        boolean left = activeHandle.equals("TL") || activeHandle.equals("BL") || activeHandle.equals("LEFT");
        boolean right = activeHandle.equals("TR") || activeHandle.equals("BR") || activeHandle.equals("RIGHT");
        boolean top = activeHandle.equals("TL") || activeHandle.equals("TR") || activeHandle.equals("TOP");
        boolean bottom = activeHandle.equals("BL") || activeHandle.equals("BR") || activeHandle.equals("BOTTOM");
        boolean corner = (left || right) && (top || bottom);
        
        switch (activeHandle) {
            case "TL" -> {
                if (isImage) {
                    double factor = Math.max(
                            (resizeStartWidth - dxImage) / resizeStartWidth,
                            (resizeStartHeight - dyImage) / resizeStartHeight);
                    newWidth = resizeStartWidth * factor;
                    newHeight = resizeStartHeight * factor;
                } else {
                    newWidth = resizeStartWidth - dxImage;
                    newHeight = resizeStartHeight - dyImage;
                }
                changeWidth = changeHeight = true;
            }
            case "TR" -> {
                if (isImage) {
                    double factor = Math.max(
                            (resizeStartWidth + dxImage) / resizeStartWidth,
                            (resizeStartHeight - dyImage) / resizeStartHeight);
                    newWidth = resizeStartWidth * factor;
                    newHeight = resizeStartHeight * factor;
                } else {
                    newWidth = resizeStartWidth + dxImage;
                    newHeight = resizeStartHeight - dyImage;
                }
                changeWidth = changeHeight = true;
            }
            case "BL" -> {
                if (isImage) {
                    double factor = Math.max(
                            (resizeStartWidth - dxImage) / resizeStartWidth,
                            (resizeStartHeight + dyImage) / resizeStartHeight);
                    newWidth = resizeStartWidth * factor;
                    newHeight = resizeStartHeight * factor;
                } else {
                    newWidth = resizeStartWidth - dxImage;
                    newHeight = resizeStartHeight + dyImage;
                }
                changeWidth = changeHeight = true;
            }
            case "BR" -> {
                if (isImage) {
                    double factor = Math.max(
                            (resizeStartWidth + dxImage) / resizeStartWidth,
                            (resizeStartHeight + dyImage) / resizeStartHeight);
                    newWidth = resizeStartWidth * factor;
                    newHeight = resizeStartHeight * factor;
                } else {
                    newWidth = resizeStartWidth + dxImage;
                    newHeight = resizeStartHeight + dyImage;
                }
                changeWidth = changeHeight = true;
            }
            case "TOP" -> {
                newHeight = clampSize(resizeStartHeight - dyImage, image.getHeight());
                newPosY = resizeStartPosY + resizeStartHeight - newHeight;
                changeHeight = true;
            }
            case "RIGHT" -> {
                newWidth = clampSize(resizeStartWidth + dxImage, image.getWidth());
                changeWidth = true;
            }
            case "BOTTOM" -> {
                newHeight = clampSize(resizeStartHeight + dyImage, image.getHeight());
                changeHeight = true;
            }
            case "LEFT" -> {
                newWidth = clampSize(resizeStartWidth - dxImage, image.getWidth());
                newPosX = resizeStartPosX + resizeStartWidth - newWidth;
                changeWidth = true;
            }
        }
        
        double availableWidth = left ? resizeStartPosX + resizeStartWidth
                : right ? image.getWidth() - resizeStartPosX : image.getWidth();
        double availableHeight = top ? resizeStartPosY + resizeStartHeight
                : bottom ? image.getHeight() - resizeStartPosY : image.getHeight();
        if (corner && isImage) {
            double factor = Math.max(newWidth / resizeStartWidth, newHeight / resizeStartHeight);
            factor = Math.clamp(factor, Math.min(10 / resizeStartWidth, 10 / resizeStartHeight),
                    Math.min(availableWidth / resizeStartWidth, availableHeight / resizeStartHeight));
            newWidth = resizeStartWidth * factor;
            newHeight = resizeStartHeight * factor;
        } else {
            newWidth = clampSize(newWidth, availableWidth);
            newHeight = clampSize(newHeight, availableHeight);
        }

        if (left) {
            newPosX = resizeStartPosX + resizeStartWidth - newWidth;
        }
        if (top) {
            newPosY = resizeStartPosY + resizeStartHeight - newHeight;
        }

        if (changeWidth) {
            settings.setResizeScaleX(resizeStartScaleX * newWidth / resizeStartWidth);
        }
        if (changeHeight) {
            settings.setResizeScaleY(resizeStartScaleY * newHeight / resizeStartHeight);
        }
        
        settings.setPositionX(newPosX);
        settings.setPositionY(newPosY);
        settings.setUseCustomPosition(true);
        
        if (nonNull(onUpdateCallback)) {
            onUpdateCallback.accept(settings);
        }
        
        event.consume();
    }
    
    /**
     * Handle mouse release - complete resize operation
     */
    public void handleMouseReleased(MouseEvent event) {
        if (nonNull(activeHandle)) {
            activeHandle = null;
            if (nonNull(onResizeCompleteCallback)) {
                onResizeCompleteCallback.run();
            }
            event.consume();
        }
    }
    
    private boolean canResize() {
        return WatermarkDimensionsHelper.canDrag(settings, image);
    }
    
    private double[] toImageCoords(MouseEvent event) {
        double[] scale = getImageScale();
        if (scale == null) return null;
        javafx.geometry.Point2D localPoint = imageView.sceneToLocal(event.getSceneX(), event.getSceneY());
        return new double[]{localPoint.getX() * scale[0], localPoint.getY() * scale[1]};
    }

    private static double clampSize(double value, double maxSize) {
        return Math.clamp(value, Math.min(10, maxSize), maxSize);
    }
}
