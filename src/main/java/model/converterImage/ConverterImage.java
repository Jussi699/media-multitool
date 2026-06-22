package model.converterImage;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Locale;

public class ConverterImage {
    
    public static String normalizeOutputFormat(String typeFile) {
        if (typeFile == null) {
            throw new IllegalArgumentException("Output format is null");
        }

        return switch (typeFile){
            case "tiff", "tif"        ->  "tif";
            case "jpeg", "jpg", "jpe" ->  typeFile.equalsIgnoreCase("jpe") ? "jpe" : "jpg";
            case "bmp"                ->  "bmp";
            default                   ->  typeFile.toLowerCase(Locale.ROOT);
        };
    }

    public static BufferedImage prepareImageForFormat(BufferedImage source, String format) {
        if ("pgm".equalsIgnoreCase(format)) {
            BufferedImage grayImage = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D graphics = grayImage.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
            graphics.drawImage(source, 0, 0, null);
            graphics.dispose();
            return grayImage;
        }

        if (!"jpg".equalsIgnoreCase(format) && !"jpeg".equalsIgnoreCase(format) && 
            !"bmp".equalsIgnoreCase(format) && !"ppm".equalsIgnoreCase(format) && !"pnm".equalsIgnoreCase(format)) {
            return source;
        }

        BufferedImage rgbImage = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return rgbImage;
    }
}
