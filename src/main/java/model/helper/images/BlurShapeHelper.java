package model.helper.images;

import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.paint.Color;
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

    /**
     * Builds four Summed Area Tables (one per channel A/R/G/B) from the given int[] pixel array.
     * Each SAT is a (width+1) x (height+1) long array with a 1-pixel border of zeros.
     * O(W*H) time and space.
     *
     * @param pixels flat ARGB pixel array (row-major, from DataBufferInt)
     * @param width  image width
     * @param height image height
     * @return long[4][] — indices 0=A, 1=R, 2=G, 3=B
     */
    public static long[][] buildSAT(int[] pixels, int width, int height) {
        int stride = width + 1;
        long[] satA = new long[stride * (height + 1)];
        long[] satR = new long[stride * (height + 1)];
        long[] satG = new long[stride * (height + 1)];
        long[] satB = new long[stride * (height + 1)];

        for (int y = 0; y < height; y++) {
            long rowA = 0, rowR = 0, rowG = 0, rowB = 0;
            for (int x = 0; x < width; x++) {
                int argb = pixels[y * width + x];
                rowA += (argb >> 24) & 0xFF;
                rowR += (argb >> 16) & 0xFF;
                rowG += (argb >>  8) & 0xFF;
                rowB +=  argb        & 0xFF;

                int idx = (y + 1) * stride + (x + 1);
                int idxAbove = y * stride + (x + 1);

                satA[idx] = rowA + satA[idxAbove];
                satR[idx] = rowR + satR[idxAbove];
                satG[idx] = rowG + satG[idxAbove];
                satB[idx] = rowB + satB[idxAbove];
            }
        }

        return new long[][]{satA, satR, satG, satB};
    }

    /**
     * Queries the SAT for a rectangle sum in O(1) using the inclusion-exclusion formula.
     * x1, y1 are inclusive; x2, y2 are exclusive.
     */
    private static long queryRect(long[] sat, int stride, int x1, int y1, int x2, int y2) {
        return sat[y2 * stride + x2]
             - sat[y1 * stride + x2]
             - sat[y2 * stride + x1]
             + sat[y1 * stride + x1];
    }

    /**
     * Applies a Gaussian-approximated blur to the given pixel array in-place using three consecutive
     * box-blur passes via SAT. By the Central Limit Theorem, three box blurs converge to a
     * Gaussian with sigma ≈ radius * sqrt(1/3), producing the same soft, photographic quality
     * as CSS filter:blur() or Photoshop Gaussian blur.
     * <p>
     * All three passes are O(W*H) each (SAT build and pixel write), so the total cost is still O(W*H)
     * regardless of radius — identical speed to a single box blur pass.
     *
     * @param pixels flat ARGB int[] from DataBufferInt (modified in-place)
     * @param width  image width
     * @param height image height
     * @param radius blur radius per box-blur pass; effective Gaussian sigma ≈ radius * 0.577
     */
    public static void applyGaussianBlur(int[] pixels, int width, int height, int radius) {
        if (radius <= 0) return;
        // Three box-blur passes via SAT, each O(W*H)
        for (int pass = 0; pass < 3; pass++) {
            long[][] sat = buildSAT(pixels, width, height);
            int stride = width + 1;
            for (int y = 0; y < height; y++) {
                int y1 = Math.max(0, y - radius);
                int y2 = Math.min(height, y + radius + 1);
                for (int x = 0; x < width; x++) {
                    int x1 = Math.max(0, x - radius);
                    int x2 = Math.min(width, x + radius + 1);
                    long area = (long)(x2 - x1) * (y2 - y1);
                    if (area == 0) continue;

                    int avgA = (int)(queryRect(sat[0], stride, x1, y1, x2, y2) / area);
                    int avgR = (int)(queryRect(sat[1], stride, x1, y1, x2, y2) / area);
                    int avgG = (int)(queryRect(sat[2], stride, x1, y1, x2, y2) / area);
                    int avgB = (int)(queryRect(sat[3], stride, x1, y1, x2, y2) / area);
                    pixels[y * width + x] = (avgA << 24) | (avgR << 16) | (avgG << 8) | avgB;
                }
            }
        }
    }
}
