package model.mediaTools.converterImage.strategy;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import model.mediaTools.converterImage.ConverterImage;

import static model.utility.io.FileHelper.readPreviewImage;
import static model.utility.io.PathWorker.createOutputFile;

public class RasterImageStrategy implements ImageConversionStrategy {
    @Override
    public File convert(File source, File destinationDir, String format) throws IOException {
        BufferedImage bufImage = readPreviewImage(source)
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
