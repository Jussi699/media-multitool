package model.helper.images;

import javafx.geometry.Bounds;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.helper.images.BlurShapeHelper.*;

public class SpotBlurHelper {
    private SpotBlurHelper() {
        /* This utility class should not be instantiated */
    }

    public static boolean isPointInsideImage(double x, double y, Bounds imageBounds) {
        return x >= imageBounds.getMinX() && x <= imageBounds.getMaxX() &&
                y >= imageBounds.getMinY() && y <= imageBounds.getMaxY();
    }

    public static BufferedImage applyBlurShapes(BufferedImage sourceImage, List<BlurShapeVisual> blurShapes, int blurIntensity, double imageWidth, double imageHeight) {
        if (isNull(sourceImage)) return null;
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();

        BufferedImage src = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = src.createGraphics();
        g.drawImage(sourceImage, 0, 0, null);
        g.dispose();

        int[] srcPixels = ((DataBufferInt) src.getRaster().getDataBuffer()).getData();
        int[] blurredPixels = Arrays.copyOf(srcPixels, srcPixels.length);
        applyGaussianBlur(blurredPixels, width, height, blurIntensity);

        if (Thread.interrupted()) return null;

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] dstPixels = ((DataBufferInt) result.getRaster().getDataBuffer()).getData();
        System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.length);

        for (int y = 0; y < height; y++) {
            if (Thread.interrupted()) return null;
            for (int x = 0; x < width; x++) {
                for (BlurShapeVisual shape : blurShapes) {
                    if (BlurShapeHelper.isPointInShapeImage(x, y, shape, imageWidth, imageHeight)) {
                        dstPixels[y * width + x] = blurredPixels[y * width + x];
                        break;
                    }
                }
            }
        }
        return result;
    }

    public static BlurShapeVisual getShapeAtPosition(double x, double y, ImageView preview, List<BlurShapeVisual> blurShapes) {
        Bounds imageBounds = preview.localToParent(preview.getBoundsInLocal());
        for (BlurShapeVisual shape : blurShapes) {
            if (BlurShapeHelper.isPointInShape(x, y, shape, imageBounds)) {
                return shape;
            }
        }
        return null;
    }

    public static void constrainShapeToImageBounds(BlurShapeVisual shape) {
        if (shape == null) return;
        double minX = Math.min(shape.startX, shape.endX);
        double maxX = Math.max(shape.startX, shape.endX);
        double minY = Math.min(shape.startY, shape.endY);
        double maxY = Math.max(shape.startY, shape.endY);

        boolean fitsHorizontally = (minX >= 0.0 && maxX <= 1.0);
        boolean fitsVertically = (minY >= 0.0 && maxY <= 1.0);

        if (!fitsHorizontally) {
            if (minX < 0.0) {
                double diff = -minX;
                shape.startX += diff;
                shape.endX += diff;
            } else if (maxX > 1.0) {
                double diff = maxX - 1.0;
                shape.startX -= diff;
                shape.endX -= diff;
            }
        }

        if (!fitsVertically) {
            if (minY < 0.0) {
                double diff = -minY;
                shape.startY += diff;
                shape.endY += diff;
            } else if (maxY > 1.0) {
                double diff = maxY - 1.0;
                shape.startY -= diff;
                shape.endY -= diff;
            }
        }
    }

    public static void updateShapeVisuals(Pane blurOverlay, ImageView preview, List<BlurShapeVisual> blurShapes, BlurShapeVisual currentShape, int handleSize) {
        if (isNull(blurOverlay) || isNull(preview)) return;
        blurOverlay.getChildren().clear();
        Bounds imageBounds = preview.localToParent(preview.getBoundsInLocal());

        for (BlurShapeVisual shape : blurShapes) {
            Shape visual = BlurShapeHelper.createShapeVisual(shape, false, imageBounds);
            blurOverlay.getChildren().add(visual);

            List<Shape> guides = new ArrayList<>();
            BlurShapeHelper.drawDashedGuides(shape, imageBounds, guides);
            blurOverlay.getChildren().addAll(guides);

            for (ResizeHandle handle : shape.getHandles(imageBounds)) {
                Rectangle handleRect = new Rectangle(handle.x - (double) handleSize / 2, handle.y - (double) handleSize / 2, handleSize, handleSize);
                handleRect.setFill(Color.WHITE);
                handleRect.setStroke(Color.CYAN);
                handleRect.setStrokeWidth(1);
                blurOverlay.getChildren().add(handleRect);
            }
        }

        if (nonNull(currentShape)) {
            Shape visual = BlurShapeHelper.createShapeVisual(currentShape, true, imageBounds);
            blurOverlay.getChildren().add(visual);
        }
    }
}
