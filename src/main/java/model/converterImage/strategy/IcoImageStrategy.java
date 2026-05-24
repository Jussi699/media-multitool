package model.converterImage.strategy;

import model.utility.Util;
import net.ifok.image.image4j.codec.ico.ICOEncoder;
import net.ifok.image.image4j.codec.ico.ICODecoder;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import model.converterImage.UsefulMethods;
import model.converterImage.ConverterImage;

public class IcoImageStrategy implements ImageConversionStrategy {
    private int size = 256; // Default size

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public File convert(File source, File destinationDir, String format) throws IOException {
        String inputExtension = UsefulMethods.getFileExtension(source).toLowerCase();
        
        if ("ico".equals(inputExtension)) {
            // Convert FROM Ico
            List<BufferedImage> images = ICODecoder.read(source);
            if (images.isEmpty()) {
                throw new IOException("ICO file does not contain images!");
            }
            String outputFormat = ConverterImage.normalizeOutputFormat(format);
            BufferedImage bestImage = ConverterImage.prepareImageForFormat(UsefulMethods.getLargestImage(images), outputFormat);
            File outputImage = Util.createOutputFile(source, destinationDir, format);
            
            if ("svg".equalsIgnoreCase(outputFormat)) {
                new SvgImageStrategy().convert(source, destinationDir, "svg");
                return outputImage;
            }

            boolean written = ImageIO.write(bestImage, outputFormat, outputImage);
            if (!written) {
                throw new IOException("Unsupported output format: " + format);
            }
            return outputImage;
        } else {
            // Convert TO Ico
            BufferedImage bufImage = UsefulMethods.readPreviewImage(source)
                    .orElseThrow(() -> new IOException("Unable to read image: " + source.getName()));
            BufferedImage resized = resizeImage(bufImage, size, size);
            File outputImage = Util.createOutputFile(source, destinationDir, "ico");
            ICOEncoder.write(resized, outputImage);
            return outputImage;
        }
    }

    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();
        return resized;
    }
}
