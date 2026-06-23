package media_multitool.watermarks;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import lombok.Getter;

import java.awt.image.BufferedImage;

public class WatermarkOverlayManager {
    private static final double HANDLE_SIZE = 8;
    
    private final Pane overlayPane;
    private final StackPane previewContainer;
    
    private Rectangle boundingBox;

    @Getter private Rectangle handleTL, handleTC, handleTR, handleML, handleMR, handleBL, handleBC, handleBR;

    public WatermarkOverlayManager(Pane overlayPane, StackPane previewContainer) {
        this.overlayPane = overlayPane;
        this.previewContainer = previewContainer;
    }
    
    /**
     * Build overlay UI elements (bounding box and resize handles)
     */
    public void buildOverlayElements() {
        overlayPane.getChildren().clear();
        
        boundingBox = new Rectangle();
        boundingBox.setFill(Color.rgb(0, 255, 0, 0.08));
        boundingBox.setStroke(Color.rgb(0, 255, 0, 0.8));
        boundingBox.setStrokeWidth(1.5);
        boundingBox.setStrokeType(StrokeType.OUTSIDE);
        boundingBox.getStrokeDashArray().addAll(6.0, 4.0);
        boundingBox.setMouseTransparent(true);
        overlayPane.getChildren().add(boundingBox);
        
        handleTL = createHandle(Cursor.NW_RESIZE);
        handleTC = createHandle(Cursor.N_RESIZE);
        handleTR = createHandle(Cursor.NE_RESIZE);
        handleML = createHandle(Cursor.W_RESIZE);
        handleMR = createHandle(Cursor.E_RESIZE);
        handleBL = createHandle(Cursor.SW_RESIZE);
        handleBC = createHandle(Cursor.S_RESIZE);
        handleBR = createHandle(Cursor.SE_RESIZE);
        
        overlayPane.getChildren().addAll(
            handleTL, handleTC, handleTR,
            handleML, handleMR,
            handleBL, handleBC, handleBR
        );
    }
    
    /**
     * Create a resize handle with specified cursor
     */
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
            Platform.runLater(() -> updateOverlayPosition(settings, image, imageView));
        }
    }
    
    /**
     * Update overlay position based on watermark position
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
        
        double scaleX = imageViewWidth / image.getWidth();
        double scaleY = imageViewHeight / image.getHeight();
        
        double[] posData = WatermarkDimensionsHelper.getCurrentPosition(settings, image);
        double posX = posData[0];
        double posY = posData[1];
        double wmWidth = posData[2];
        double wmHeight = posData[3];
        
        double containerWidth = previewContainer.getWidth();
        double containerHeight = previewContainer.getHeight();
        double imageStartX = (containerWidth - imageViewWidth) / 2;
        double imageStartY = (containerHeight - imageViewHeight) / 2;
        
        double displayX = imageStartX + posX * scaleX;
        double displayY = imageStartY + posY * scaleY;
        double displayW = Math.max(20, wmWidth * scaleX);
        double displayH = Math.max(14, wmHeight * scaleY);
        
        overlayPane.setPrefWidth(containerWidth);
        overlayPane.setPrefHeight(containerHeight);
        
        boundingBox.setX(displayX);
        boundingBox.setY(displayY);
        boundingBox.setWidth(displayW);
        boundingBox.setHeight(displayH);
        
        double hh = HANDLE_SIZE / 2;
        
        positionHandle(handleTL, displayX - hh, displayY - hh);
        positionHandle(handleTR, displayX + displayW - hh, displayY - hh);
        positionHandle(handleBL, displayX - hh, displayY + displayH - hh);
        positionHandle(handleBR, displayX + displayW - hh, displayY + displayH - hh);
        
        positionHandle(handleTC, displayX + displayW / 2 - hh, displayY - hh);
        positionHandle(handleBC, displayX + displayW / 2 - hh, displayY + displayH - hh);
        positionHandle(handleML, displayX - hh, displayY + displayH / 2 - hh);
        positionHandle(handleMR, displayX + displayW - hh, displayY + displayH / 2 - hh);
    }
    
    /**
     * Position a handle at specified coordinates
     */
    private void positionHandle(Rectangle handle, double x, double y) {
        handle.setX(x);
        handle.setY(y);
    }
}
