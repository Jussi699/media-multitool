package model.converterImage;

import model.converterImage.strategy.ImageConversionStrategy;
import model.converterImage.strategy.ImageStrategyFactory;
import model.compressorImage.Compressor;
import com.imagetools.RotateSide;
import model.properties.ImageProperties;
import model.compressorImage.CompressionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ImageOperationsTest {

    @TempDir
    Path tempDir;

    @Test
    public void testConvertPngToAllFormats() throws IOException {
        String PATH_TO_PNG = "E:\\test\\srcImage\\PNG.png";
        File source = new File(PATH_TO_PNG);
        String[] formats = {"jpg", "bmp", "webp", "ico", "svg", "tiff", "ppm", "pgm", "pam"};
        
        for (String format : formats) {
            ImageConversionStrategy strategy = ImageStrategyFactory.getStrategy(source, format);
            File result = strategy.convert(source, tempDir.toFile(), format);
            assertNotNull(result);
            assertTrue(result.exists());
            assertTrue(result.length() > 0);
        }
    }

    @Test
    public void testConvertIcoToPng() throws IOException {
        String PATH_TO_ICO = "E:\\test\\srcImage\\ICO.ico";
        File source = new File(PATH_TO_ICO);
        ImageConversionStrategy strategy = ImageStrategyFactory.getStrategy(source, "png");
        File result = strategy.convert(source, tempDir.toFile(), "png");
        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    public void testConvertSvgToPng() throws IOException {
        String PATH_TO_SVG = "E:\\test\\srcImage\\SVG.svg";
        File source = new File(PATH_TO_SVG);
        ImageConversionStrategy strategy = ImageStrategyFactory.getStrategy(source, "png");
        File result = strategy.convert(source, tempDir.toFile(), "png");
        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    public void testImageCompression() {
        String PATH_TO_JPG = "E:\\test\\srcImage\\JPEG.jpeg";
        File source = new File(PATH_TO_JPG);
        ImageProperties props = new ImageProperties();
        props.setImage(source);
        props.setOutput(tempDir.toFile());
        props.setScale(0.5f);
        props.setQuality(0.6f);

        Compressor compressor = new Compressor();
        Optional<CompressionResult> result = compressor.compressorStandardImage(props);
        
        assertTrue(result.isPresent());
        assertTrue(result.get().outputFile().exists());
    }

    @Test
    public void testTurnImage() {
        int width = 100;
        int height = 50;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        Optional<BufferedImage> flippedH = com.imagetools.ImageTools.rotateImage(image, RotateSide.HORIZONTALLY);
        assertTrue(flippedH.isPresent());
        assertEquals(width, flippedH.get().getWidth());
        assertEquals(height, flippedH.get().getHeight());

        Optional<BufferedImage> flippedV = com.imagetools.ImageTools.rotateImage(image, RotateSide.VERTICALLY);
        assertTrue(flippedV.isPresent());
        assertEquals(width, flippedV.get().getWidth());
        assertEquals(height, flippedV.get().getHeight());

        Optional<BufferedImage> rotatedR = com.imagetools.ImageTools.rotateImage(image, RotateSide.RIGHT);
        assertTrue(rotatedR.isPresent());
        assertEquals(height, rotatedR.get().getWidth());
        assertEquals(width, rotatedR.get().getHeight());

        Optional<BufferedImage> rotatedL = com.imagetools.ImageTools.rotateImage(image, RotateSide.LEFT);
        assertTrue(rotatedL.isPresent());
        assertEquals(height, rotatedL.get().getWidth());
        assertEquals(width, rotatedL.get().getHeight());
    }
}
