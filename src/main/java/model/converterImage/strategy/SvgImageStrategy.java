package model.converterImage.strategy;

import model.utility.Util;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;
import model.converterImage.UsefulMethods;

public class SvgImageStrategy implements ImageConversionStrategy {
    @Override
    public File convert(File source, File destinationDir, String format) throws IOException {
        BufferedImage bufImage = UsefulMethods.readPreviewImage(source)
                .orElseThrow(() -> new IOException("Unable to read image: " + source.getName()));
        
        File outputImage = Util.createOutputFile(source, destinationDir, "svg");
        saveAsSvg(bufImage, outputImage);
        return outputImage;
    }

    private void saveAsSvg(BufferedImage image, File outputFile) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
            writer.write(String.format("<svg width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n",
                    image.getWidth(), image.getHeight()));
            writer.write(String.format("  <image width=\"%d\" height=\"%d\" xlink:href=\"data:image/png;base64,%s\"/>\n",
                    image.getWidth(), image.getHeight(), base64));
            writer.write("</svg>");
        }
    }
}
