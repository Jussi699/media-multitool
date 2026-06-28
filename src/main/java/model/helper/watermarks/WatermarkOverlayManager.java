package model.helper.watermarks;

import javafx.scene.Cursor;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import lombok.Getter;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

public class WatermarkOverlayManager {
    private static final double HANDLE_SIZE = 6;
    
    /**
     * Named handle positions for cleaner access and iteration.
     */
    public enum HandlePosition {
        TL(Cursor.NW_RESIZE),  TR(Cursor.NE_RESIZE),
        BL(Cursor.SW_RESIZE),  BR(Cursor.SE_RESIZE);
        
        @Getter private final Cursor cursor;
        
        HandlePosition(Cursor cursor) {
            this.cursor = cursor;
        }
    }
    
    private final Pane overlayPane;
    private final StackPane previewContainer;
    
    private Rectangle boundingBox;
    private final Map<HandlePosition, Rectangle> handles = new EnumMap<>(HandlePosition.class);
    
    private double cachedScaleX   = 0;
    private double cachedScaleY   = 0;
    private int cachedImageWidth  = 0;
    private int cachedImageHeight = 0;
    private int cachedViewWidth   = 0;
    private int cachedViewHeight  = 0;
    
    public WatermarkOverlayManager(Pane overlayPane, StackPane previewContainer) {
        this.overlayPane = overlayPane;
        this.previewContainer = previewContainer;
    }
    
    /**
     * Get a handle by its position identifier.
     */
    public Rectangle getHandle(HandlePosition position) {
        return handles.get(position);
    }
    
    /**
     * Build overlay UI elements (bounding box and resize handles)
     */
    public void buildOverlayElements() {
        overlayPane.getChildren().clear();
        handles.clear();
        
        boundingBox = new Rectangle();
        boundingBox.setFill(Color.rgb(0, 255, 0, 0.08));
        boundingBox.setStroke(Color.rgb(0, 255, 0, 0.8));
        boundingBox.setStrokeWidth(1.5);
        boundingBox.setStrokeType(StrokeType.OUTSIDE);
        boundingBox.getStrokeDashArray().addAll(6.0, 4.0);
        boundingBox.setMouseTransparent(true);
        overlayPane.getChildren().add(boundingBox);
        
        HandlePosition[] cornerHandles = {
            HandlePosition.TL, HandlePosition.TR,
            HandlePosition.BL, HandlePosition.BR
        };
        
        for (HandlePosition pos : cornerHandles) {
            Rectangle handle = createHandle(pos.getCursor());
            handles.put(pos, handle);
            overlayPane.getChildren().add(handle);
        }
    }

    public void clearOverlay() {
        overlayPane.getChildren().clear();
        handles.clear();
        boundingBox = null;
    }

    private Rectangle createHandle(Cursor cursor) {
        Rectangle handle = new Rectangle(HANDLE_SIZE, HANDLE_SIZE);
        handle.setFill(Color.WHITE);
        handle.setStroke(Color.rgb(0, 200, 0));
        handle.setStrokeWidth(1);
        handle.setCursor(cursor);
        return handle;
    }
    
    /**
     * Update overlay position and visibility
     */
    public void updateOverlay(
        WatermarkSettings settings,
        BufferedImage image,
        javafx.scene.image.ImageView imageView
    ) {
        if (overlayPane == null || image == null) {
            return;
        }
        
        boolean shouldShow = WatermarkDimensionsHelper.canDrag(settings, image);
        overlayPane.setVisible(shouldShow);
        
        if (shouldShow && boundingBox != null) {
            updateOverlayPosition(settings, image, imageView);
        }
    }
    
    /**
     * Update overlay position based on watermark position
     * Optimized: cache scale factors and container dimensions to avoid redundant calculations
     */
    private void updateOverlayPosition(
        WatermarkSettings settings,
        BufferedImage image,
        javafx.scene.image.ImageView imageView
    ) {
        double imageViewWidth = imageView.getBoundsInLocal().getWidth();
        double imageViewHeight = imageView.getBoundsInLocal().getHeight();
        
        if (imageViewWidth <= 0 || imageViewHeight <= 0) {
            return;
        }
        
        boolean dimensionsChanged = cachedImageWidth != image.getWidth() ||
                                   cachedImageHeight != image.getHeight() ||
                                   cachedViewWidth != (int)imageViewWidth ||
                                   cachedViewHeight != (int)imageViewHeight;
        
        if (dimensionsChanged) {
            cachedScaleX = imageViewWidth / image.getWidth();
            cachedScaleY = imageViewHeight / image.getHeight();
            cachedImageWidth = image.getWidth();
            cachedImageHeight = image.getHeight();
            cachedViewWidth = (int)imageViewWidth;
            cachedViewHeight = (int)imageViewHeight;
        }
        
        double[] posData = WatermarkDimensionsHelper.getCurrentPosition(settings, image);
        
        double containerWidth = previewContainer.getWidth();
        double containerHeight = previewContainer.getHeight();
        double imageStartX = (containerWidth - imageViewWidth) / 2;
        double imageStartY = (containerHeight - imageViewHeight) / 2;
        
        double displayX = imageStartX + posData[0] * cachedScaleX;
        double displayY = imageStartY + posData[1] * cachedScaleY;
        double displayW = Math.max(20, posData[2] * cachedScaleX);
        double displayH = Math.max(14, posData[3] * cachedScaleY);
        
        overlayPane.setPrefWidth(containerWidth);
        overlayPane.setPrefHeight(containerHeight);
        
        boundingBox.setX(displayX);
        boundingBox.setY(displayY);
        boundingBox.setWidth(displayW);
        boundingBox.setHeight(displayH);
        
        double hh = HANDLE_SIZE / 2;
        
        positionHandle(HandlePosition.TL, displayX - hh,                displayY - hh);
        positionHandle(HandlePosition.TR, displayX + displayW - hh,     displayY - hh);
        positionHandle(HandlePosition.BL, displayX - hh,                displayY + displayH - hh);
        positionHandle(HandlePosition.BR, displayX + displayW - hh,     displayY + displayH - hh);
    }
    
    private void positionHandle(HandlePosition pos, double x, double y) {
        Rectangle handle = handles.get(pos);
        handle.setX(x);
        handle.setY(y);
    }
}
