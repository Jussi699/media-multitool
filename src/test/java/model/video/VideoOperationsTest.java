package model.video;

import model.mediaTools.compressorVideo.Compressor;
import model.mediaTools.compressorVideo.VideoPresets;
import model.enums.MediaType;
import model.mediaTools.converterVideo.ConverterVideoAudioFile;
import model.properties.VideoAndAudioProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static services.VideoTestService.createTestVideo;

class VideoOperationsTest {

    @TempDir
    Path tempDir;

    @Test
    void testVideoConversion() throws Exception {
        File source = createTestVideo(tempDir.resolve("test_src.mp4").toFile(), 160, 120, 12, 6);
        ConverterVideoAudioFile converter = new ConverterVideoAudioFile();

        VideoAndAudioProperties properties = getVideoAndAudioProperties(source);

        boolean success = converter.convert(properties, MediaType.VIDEO, _ -> {});

        assertTrue(success);
        File result = converter.nameFileAfter;
        assertNotNull(result);
        assertTrue(result.exists());
    }

    private VideoAndAudioProperties getVideoAndAudioProperties(File source) {
        VideoAndAudioProperties properties = new VideoAndAudioProperties();
        properties.setSrcFile(source);
        properties.setOutput(tempDir.toFile());
        properties.setVideoBitRate(2000);
        properties.setAudioBitRate(128);
        properties.setChannel(2);
        properties.setSamplingRate(44100);
        properties.setFps(30);
        properties.setVideoCodec("libx264");
        properties.setAudioCodec("aac");
        properties.setFfmpegFormat("mp4");
        properties.setResolution("160x120");
        return properties;
    }

    @Test
    void testVideoCompression() throws Exception {
        File source = createTestVideo(tempDir.resolve("source_for_compress.mp4").toFile(), 160, 120, 20, 10);
        File output = new File(tempDir.toFile(), "compressed_" + UUID.randomUUID() + ".mp4");

        Compressor compressor = new Compressor();

        // Get adaptive presets for the source file
        VideoPresets.Preset[] presets = VideoPresets.createAdaptivePresets(source).orElseThrow();
        VideoPresets.Preset selectedPreset = presets[0]; // Use Basic preset

        compressor.compress(source, output, selectedPreset.video(), selectedPreset.audio(), selectedPreset.crf(), _ -> {});

        assertTrue(output.exists());
        assertTrue(output.length() > 0);
    }
}
