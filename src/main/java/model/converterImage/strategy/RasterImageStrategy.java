package model.converterImage.strategy;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import model.converterImage.UsefulMethods;
import model.converterImage.ConverterImage;

import static model.utility.PathWorker.createOutputFile;

public class RasterImageStrategy implements ImageConversionStrategy {
    @Override
    public File convert(File source, File destinationDir, String format) throws IOException {
        BufferedImage bufImage = UsefulMethods.readPreviewImage(source)
                .orElseThrow(() -> new IOException("Unable to read image: " + source.getName()));
        
        String outputFormat = ConverterImage.normalizeOutputFormat(format);
        File outputImage = createOutputFile(source, destinationDir, format);
        
        BufferedImage preparedImage = ConverterImage.prepareImageForFormat(bufImage, outputFormat);
        boolean written = ImageIO.write(preparedImage, outputFormat, outputImage);
        
        if (!written) {
            throw new IOException("Unsupported output format: " + format);
        }
        return outputImage;
    }
}
