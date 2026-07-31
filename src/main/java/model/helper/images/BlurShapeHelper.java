package model.helper.images;

import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class BlurShapeHelper {
    
    public enum ShapeType { RECTANGLE, ELLIPSE }

    public static class BlurShapeVisual {
        public ShapeType type;
        public double startX, startY, endX, endY;
        public boolean isSelected = false;

        public BlurShapeVisual(ShapeType type, double startX, double startY, double endX, double endY) {
            this.type = type;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }

        public double getAbsStartX(Bounds imageBounds) {
            double imageWidth = imageBounds.getWidth();
            return startX * imageWidth + imageBounds.getMinX();
        }

        public double getAbsStartY(Bounds imageBounds) {
            double imageHeight = imageBounds.getHeight();
            return startY * imageHeight + imageBounds.getMinY();
        }

        public double getAbsEndX(Bounds imageBounds) {
            double imageWidth = imageBounds.getWidth();
            return endX * imageWidth + imageBounds.getMinX();
        }

        public double getAbsEndY(Bounds imageBounds) {
            double imageHeight = imageBounds.getHeight();
            return endY * imageHeight + imageBounds.getMinY();
        }

        public List<ResizeHandle> getHandles(Bounds imageBounds) {
            double minX = Math.min(getAbsStartX(imageBounds), getAbsEndX(imageBounds));
            double maxX = Math.max(getAbsStartX(imageBounds), getAbsEndX(imageBounds));
            double minY = Math.min(getAbsStartY(imageBounds), getAbsEndY(imageBounds));
            double maxY = Math.max(getAbsStartY(imageBounds), getAbsEndY(imageBounds));

            List<ResizeHandle> handles = new ArrayList<>();
            handles.add(new ResizeHandle(minX, minY, ResizeHandle.Position.TOP_LEFT, this));
            handles.add(new ResizeHandle(maxX, minY, ResizeHandle.Position.TOP_RIGHT, this));
            handles.add(new ResizeHandle(minX, maxY, ResizeHandle.Position.BOTTOM_LEFT, this));
            handles.add(new ResizeHandle(maxX, maxY, ResizeHandle.Position.BOTTOM_RIGHT, this));
            handles.add(new ResizeHandle(minX, (minY + maxY) / 2, ResizeHandle.Position.LEFT, this));
            handles.add(new ResizeHandle(maxX, (minY + maxY) / 2, ResizeHandle.Position.RIGHT, this));
            handles.add(new ResizeHandle((minX + maxX) / 2, minY, ResizeHandle.Position.TOP, this));
            handles.add(new ResizeHandle((minX + maxX) / 2, maxY, ResizeHandle.Position.BOTTOM, this));
            return handles;
        }

        public ResizeHandle getHandleAt(double x, double y, Bounds imageBounds) {
            for (ResizeHandle handle : getHandles(imageBounds)) {
                if (Math.abs(handle.x - x) < 10 && Math.abs(handle.y - y) < 10) {
                    return handle;
                }
            }
            return null;
        }
    }

    public static class ResizeHandle {
        public enum Position {
            TOP(Cursor.N_RESIZE),
            LEFT(Cursor.W_RESIZE),
            RIGHT(Cursor.E_RESIZE),
            BOTTOM(Cursor.S_RESIZE),
            TOP_LEFT(Cursor.NW_RESIZE),
            TOP_RIGHT(Cursor.NE_RESIZE),
            BOTTOM_LEFT(Cursor.SW_RESIZE),
            BOTTOM_RIGHT(Cursor.SE_RESIZE);

            public final Cursor cursor;

            Position(Cursor cursor) {
                this.cursor = cursor;
            }
        }

        public double x, y;
        public Position position;
        public BlurShapeVisual shape;
        public Cursor cursor;

        public ResizeHandle(double x, double y, Position position, BlurShapeVisual shape) {
            this.x = x;
            this.y = y;
            this.position = position;
            this.shape = shape;
            this.cursor = position.cursor;
        }
    }

    public static Shape createShapeVisual(BlurShapeVisual shape, boolean isCurrent, Bounds imageBounds) {
        double minX = Math.min(shape.getAbsStartX(imageBounds), shape.getAbsEndX(imageBounds));
        double maxX = Math.max(shape.getAbsStartX(imageBounds), shape.getAbsEndX(imageBounds));
        double minY = Math.min(shape.getAbsStartY(imageBounds), shape.getAbsEndY(imageBounds));
        double maxY = Math.max(shape.getAbsStartY(imageBounds), shape.getAbsEndY(imageBounds));
        double width = maxX - minX;
        double height = maxY - minY;

        Color strokeColor;
        if (isCurrent) {
            strokeColor = Color.RED;
        } else if (shape.isSelected) {
            strokeColor = Color.LIME;
        } else {
            strokeColor = Color.CYAN;
        }

        Shape visual;
        if (shape.type == ShapeType.RECTANGLE) {
            Rectangle rect = new Rectangle(minX, minY, width, height);
            rect.setFill(null);
            rect.setStroke(strokeColor);
            rect.setStrokeWidth(2);
            rect.getStrokeDashArray().addAll(5.0, 5.0);
            rect.setStrokeDashOffset(0);
            visual = rect;
        } else {
            Ellipse ellipse = new Ellipse(minX + width / 2, minY + height / 2, width / 2, height / 2);
            ellipse.setFill(null);
            ellipse.setStroke(strokeColor);
            ellipse.setStrokeWidth(2);
            ellipse.getStrokeDashArray().addAll(5.0, 5.0);
            ellipse.setStrokeDashOffset(0);
            visual = ellipse;
        }

        return visual;
    }

    public static void drawDashedGuides(BlurShapeVisual shape, Bounds imageBounds, List<Shape> guidesList) {
        double minX = Math.min(shape.getAbsStartX(imageBounds), shape.getAbsEndX(imageBounds));
        double maxX = Math.max(shape.getAbsStartX(imageBounds), shape.getAbsEndX(imageBounds));
        double minY = Math.min(shape.getAbsStartY(imageBounds), shape.getAbsEndY(imageBounds));
        double maxY = Math.max(shape.getAbsStartY(imageBounds), shape.getAbsEndY(imageBounds));

        if (shape.type == ShapeType.RECTANGLE) {
            Line verticalLeft = new Line(minX, minY, minX, maxY);
            verticalLeft.setStroke(Color.color(0.5, 0.5, 0.5, 0.5));
            verticalLeft.setStrokeWidth(1);
            verticalLeft.getStrokeDashArray().addAll(5.0, 5.0);
            guidesList.add(verticalLeft);

            Line verticalRight = new Line(maxX, minY, maxX, maxY);
            verticalRight.setStroke(Color.color(0.5, 0.5, 0.5, 0.5));
            verticalRight.setStrokeWidth(1);
            verticalRight.getStrokeDashArray().addAll(5.0, 5.0);
            guidesList.add(verticalRight);

            Line horizontalTop = new Line(minX, minY, maxX, minY);
            horizontalTop.setStroke(Color.color(0.5, 0.5, 0.5, 0.5));
            horizontalTop.setStrokeWidth(1);
            horizontalTop.getStrokeDashArray().addAll(5.0, 5.0);
            guidesList.add(horizontalTop);

            Line horizontalBottom = new Line(minX, maxY, maxX, maxY);
            horizontalBottom.setStroke(Color.color(0.5, 0.5, 0.5, 0.5));
            horizontalBottom.setStrokeWidth(1);
            horizontalBottom.getStrokeDashArray().addAll(5.0, 5.0);
            guidesList.add(horizontalBottom);
        } else {
            double width = maxX - minX;
            double height = maxY - minY;
            
            Rectangle dashedRect = new Rectangle(minX, minY, width, height);
            dashedRect.setFill(null);
            dashedRect.setStroke(Color.CYAN);
            dashedRect.setStrokeWidth(1);
            dashedRect.getStrokeDashArray().addAll(5.0, 5.0);
            guidesList.add(dashedRect);
        }
    }

    public static boolean isPointInShape(double displayX, double displayY, BlurShapeVisual shape, Bounds imageBounds) {
        double imageWidth = imageBounds.getWidth();
        double imageHeight = imageBounds.getHeight();
        
        double normalizedX = (displayX - imageBounds.getMinX()) / imageWidth;
        double normalizedY = (displayY - imageBounds.getMinY()) / imageHeight;

        double minX = Math.min(shape.startX, shape.endX);
        double maxX = Math.max(shape.startX, shape.endX);
        double minY = Math.min(shape.startY, shape.endY);
        double maxY = Math.max(shape.startY, shape.endY);

        if (shape.type == ShapeType.RECTANGLE) {
            return normalizedX >= minX && normalizedX <= maxX && normalizedY >= minY && normalizedY <= maxY;
        } else {
            double centerX = (minX + maxX) / 2;
            double centerY = (minY + maxY) / 2;
            double radiusX = (maxX - minX) / 2;
            double radiusY = (maxY - minY) / 2;

            if (radiusX == 0 || radiusY == 0) return false;
            double dx = (normalizedX - centerX) / radiusX;
            double dy = (normalizedY - centerY) / radiusY;
            return (dx * dx + dy * dy) <= 1;
        }
    }

    public static boolean isPointInShapeImage(int imageX, int imageY, BlurShapeVisual shape, ImageView imageViewPreview) {
        double imageWidth = imageViewPreview.getImage().getWidth();
        double imageHeight = imageViewPreview.getImage().getHeight();

        double normalizedX = imageX / imageWidth;
        double normalizedY = imageY / imageHeight;

        double minX = Math.min(shape.startX, shape.endX);
        double maxX = Math.max(shape.startX, shape.endX);
        double minY = Math.min(shape.startY, shape.endY);
        double maxY = Math.max(shape.startY, shape.endY);

        if (shape.type == ShapeType.RECTANGLE) {
            return normalizedX >= minX && normalizedX <= maxX && normalizedY >= minY && normalizedY <= maxY;
        } else {
            double centerX = (minX + maxX) / 2;
            double centerY = (minY + maxY) / 2;
            double radiusX = (maxX - minX) / 2;
            double radiusY = (maxY - minY) / 2;

            if (radiusX == 0 || radiusY == 0) return false;
            double dx = (normalizedX - centerX) / radiusX;
            double dy = (normalizedY - centerY) / radiusY;
            return (dx * dx + dy * dy) <= 1;
        }
    }

    public static int getBlurredPixel(BufferedImage image, int x, int y, int radius) {
        long sumR = 0, sumG = 0, sumB = 0, sumA = 0;
        int count = 0;

        int width = image.getWidth();
        int height = image.getHeight();

        for (int ky = -radius; ky <= radius; ky++) {
            for (int kx = -radius; kx <= radius; kx++) {
                int pixelX = Math.clamp(x + kx, 0, width - 1);
                int pixelY = Math.clamp(y + ky, 0, height - 1);

                int rgb = image.getRGB(pixelX, pixelY);

                sumA += (rgb >> 24) & 0xFF;
                sumR += (rgb >> 16) & 0xFF;
                sumG += (rgb >> 8) & 0xFF;
                sumB += rgb & 0xFF;
                count++;
            }
        }

        int avgA = (int) (sumA / count);
        int avgR = (int) (sumR / count);
        int avgG = (int) (sumG / count);
        int avgB = (int) (sumB / count);

        return (avgA << 24) | (avgR << 16) | (avgG << 8) | avgB;
    }
}
