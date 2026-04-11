package Model.Converter;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class DetermineType {
    public static String determineType(File file) {
        try(ImageInputStream iis = ImageIO.createImageInputStream(file)){
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if(readers.hasNext()) {
                ImageReader reader = readers.next();
                return reader.getFormatName();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return null;
    }
}
