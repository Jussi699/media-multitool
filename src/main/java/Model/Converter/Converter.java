package Model.Converter;

import net.ifok.image.image4j.codec.ico.ICOEncoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Converter {
    public static void convert(File image, File pathForSave, String typeFile) {
        String type = DetermineType.determineType(image);
        try {
            BufferedImage bufImage = ImageIO.read(image);
            if (bufImage == null) {
                throw new IOException("Unable to read image");
            }
            String nameFile = image.getName().substring(0, image.getName().indexOf(".")) + "_converted.";
            String outputPath = pathForSave.getAbsolutePath() + File.separator + nameFile + typeFile;
            File outputImage = new File(outputPath);

            ImageIO.write(bufImage, typeFile, outputImage);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void convertToIco(File image, File pathForSave, String typeFile) {
        try {
            BufferedImage bufImage = ImageIO.read(image);
            String nameFile = image.getName().substring(0, image.getName().indexOf(".")) + "_converted.";
            String outputPath = pathForSave.getAbsolutePath() + File.separator + nameFile + typeFile;
            File outputImage = new File(outputPath);

            ICOEncoder.write(bufImage, outputImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
