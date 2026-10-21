package model.image;

import model.mediaTools.converterImage.strategy.ImageConversionStrategy;
import model.mediaTools.converterImage.strategy.ImageStrategyFactory;
import model.mediaTools.compressorImage.Compressor;
import model.enums.RotateSide;
import model.imageTools.ImageTools;
import model.properties.ImageProperties;
import model.mediaTools.compressorImage.CompressionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import services.ImageTestService;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ImageOperationsTest {

    @TempDir
    Path tempDir;

    @Test
    void testConvertPngToAllFormats() throws IOException {
        File source = ImageTestService.createPng(tempDir, "sample.png");
        String[] formats = {"jpg", "bmp", "webp", "ico", "svg", "tiff", "ppm", "pgm", "pam"};

        for (String format : formats) {
            ImageConversionStrategy strategy = ImageStrategyFactory.getStrategy(source, format);
            File result = strategy.convert(source, tempDir.toFile(), format);
            assertNotNull(result, "Result file is null for format: " + format);
            assertTrue(result.exists(), "File should exist: " + result.getName());
            assertTrue(result.length() > 0, "File shouldn't be empty: " + result.getName());
        }
    }

    @Test
    void testConvertIcoToPng() throws IOException {
        File source = ImageTestService.createIco(tempDir, "sample.ico");

        ImageConversionStrategy strategy = ImageStrategyFactory.getStrategy(source, "png");
        File result = strategy.convert(source, tempDir.toFile(), "png");
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.length() > 0);
    }

    @Test
    void testConvertSvgToPng() throws IOException {
        File source = ImageTestService.createSvg(tempDir, "sample.svg");

        ImageConversionStrategy strategy = ImageStrategyFactory.getStrategy(source, "png");
        File result = strategy.convert(source, tempDir.toFile(), "png");
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.length() > 0);
    }

    @Test
    void testImageCompression() throws IOException {
        File source = ImageTestService.createJpg(tempDir, "sample.jpg");

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
    void testTurnImage() {
        int width = 100;
        int height = 50;
        BufferedImage image = ImageTestService.createBufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Optional<BufferedImage> flippedH = ImageTools.rotateImage(image, RotateSide.HORIZONTALLY);
        assertTrue(flippedH.isPresent());
        assertEquals(width, flippedH.get().getWidth());
        assertEquals(height, flippedH.get().getHeight());

        Optional<BufferedImage> flippedV = ImageTools.rotateImage(image, RotateSide.VERTICALLY);
        assertTrue(flippedV.isPresent());
        assertEquals(width, flippedV.get().getWidth());
        assertEquals(height, flippedV.get().getHeight());

        Optional<BufferedImage> rotatedR = ImageTools.rotateImage(image, RotateSide.RIGHT);
        assertTrue(rotatedR.isPresent());
        assertEquals(height, rotatedR.get().getWidth());
        assertEquals(width, rotatedR.get().getHeight());

        Optional<BufferedImage> rotatedL = ImageTools.rotateImage(image, RotateSide.LEFT);
        assertTrue(rotatedL.isPresent());
        assertEquals(height, rotatedL.get().getWidth());
        assertEquals(width, rotatedL.get().getHeight());
    }
}