package services;

import net.ifok.image.image4j.codec.ico.ICOEncoder;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImageTestService {

    public static BufferedImage createBufferedImage(int width, int height, int imageType) {
        BufferedImage image = new BufferedImage(width, height, imageType);
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(10, 10, Math.max(1, width / 2), Math.max(1, height / 2));
        g2d.setColor(Color.GREEN);
        g2d.drawString("Media Test", Math.max(2, width / 4), Math.max(10, height / 2));

        g2d.dispose();
        return image;
    }

    public static File createPng(Path directory, String fileName, int width, int height) throws IOException {
        BufferedImage img = createBufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        File file = directory.resolve(fileName).toFile();
        ImageIO.write(img, "png", file);
        return file;
    }

    public static File createPng(Path directory, String fileName) throws IOException {
        return createPng(directory, fileName, 200, 200);
    }

    public static File createJpg(Path directory, String fileName, int width, int height) throws IOException {
        BufferedImage img = createBufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        File file = directory.resolve(fileName).toFile();
        ImageIO.write(img, "jpg", file);
        return file;
    }

    public static File createJpg(Path directory, String fileName) throws IOException {
        return createJpg(directory, fileName, 200, 200);
    }

    public static File createBmp(Path directory, String fileName, int width, int height) throws IOException {
        BufferedImage img = createBufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        File file = directory.resolve(fileName).toFile();
        ImageIO.write(img, "bmp", file);
        return file;
    }

    public static File createBmp(Path directory, String fileName) throws IOException {
        return createBmp(directory, fileName, 200, 200);
    }

    public static File createTiff(Path directory, String fileName, int width, int height) throws IOException {
        BufferedImage img = createBufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        File file = directory.resolve(fileName).toFile();
        ImageIO.write(img, "tiff", file);
        return file;
    }

    public static File createTiff(Path directory, String fileName) throws IOException {
        return createTiff(directory, fileName, 200, 200);
    }

    public static File createIco(Path directory, String fileName, int size) throws IOException {
        BufferedImage img = createBufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        File file = directory.resolve(fileName).toFile();
        ICOEncoder.write(img, file);
        return file;
    }

    public static File createIco(Path directory, String fileName) throws IOException {
        return createIco(directory, fileName, 128);
    }

    public static File createSvg(Path directory, String fileName) throws IOException {
        String svgContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 200 200">
                    <rect width="200" height="200" fill="#f0f0f0"/>
                    <circle cx="100" cy="100" r="50" fill="#007acc"/>
                    <text x="100" y="110" font-size="16" text-anchor="middle" fill="#ffffff">SVG Test</text>
                </svg>
                """;
        Path filePath = directory.resolve(fileName);
        Files.writeString(filePath, svgContent);
        return filePath.toFile();
    }

    public static File createImage(Path directory, String fileName, String format, int width, int height) throws IOException {
        BufferedImage img = createBufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        File file = directory.resolve(fileName).toFile();
        ImageIO.write(img, format, file);
        return file;
    }

    public static BufferedImage readImage(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            throw new IOException("Failed to read image from: " + file.getAbsolutePath());
        }
        return img;
    }
}
