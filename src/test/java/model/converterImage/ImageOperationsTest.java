package model.converterImage;

import model.converterImage.strategy.ImageConversionStrategy;
import model.converterImage.strategy.ImageStrategyFactory;
import model.compressorImage.Compressor;
import model.properties.ImageProperties;
import model.compressorImage.CompressionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ImageOperationsTest {

    private final String PATH_TO_PNG = "E:\\test\\srcImage\\PNG.png";
    private final String PATH_TO_JPG = "E:\\test\\srcImage\\JPEG.jpeg";
    private final String PATH_TO_ICO = "E:\\test\\srcImage\\ICO.ico";
    private final String PATH_TO_SVG = "E:\\test\\srcImage\\SVG.svg";

    @TempDir
    Path tempDir;

    @Test
    public void testConvertPngToAllFormats() throws IOException {
        if (PATH_TO_PNG.isEmpty()) return;
        File source = new File(PATH_TO_PNG);
        String[] formats = {"jpg", "bmp", "webp", "ico", "svg"};
        
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
        if (PATH_TO_ICO.isEmpty()) return;
        File source = new File(PATH_TO_ICO);
        ImageConversionStrategy strategy = ImageStrategyFactory.getStrategy(source, "png");
        File result = strategy.convert(source, tempDir.toFile(), "png");
        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    public void testConvertSvgToPng() throws IOException {
        if (PATH_TO_SVG.isEmpty()) return;
        File source = new File(PATH_TO_SVG);
        ImageConversionStrategy strategy = ImageStrategyFactory.getStrategy(source, "png");
        File result = strategy.convert(source, tempDir.toFile(), "png");
        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    public void testImageCompression() throws IOException {
        if (PATH_TO_JPG.isEmpty()) return;
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
}
