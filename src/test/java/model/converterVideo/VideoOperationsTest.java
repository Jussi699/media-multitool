package model.converterVideo;

import model.compressorVideo.Compressor;
import model.compressorVideo.VideoPresets;
import model.enums.TypeMedia;
import model.properties.VideoAndAudioProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class VideoOperationsTest {

    private final String PATH_TO_VIDEO = "E:\\test\\srcVideo\\MP4.MP4";

    @TempDir
    Path tempDir;

    @Test
    public void testVideoConversion() {
        File source = new File(PATH_TO_VIDEO);
        ConverterVideoAudioFile converter = new ConverterVideoAudioFile();

        VideoAndAudioProperties properties = getVideoAndAudioProperties(source);

        boolean success = converter.convert(properties, TypeMedia.VIDEO , _  -> {});

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
        properties.setVideoCodec("h264_nvenc");
        properties.setAudioCodec("aac");
        properties.setFfmpegFormat("mp4");
        properties.setResolution("1280x720");
        return properties;
    }

    @Test
    public void testVideoCompression() throws Exception {
        File source = new File(PATH_TO_VIDEO);
        File output = new File(tempDir.toFile(), "compressed_" + UUID.randomUUID() + ".mp4");

        Compressor compressor = new Compressor();
        
        // Get adaptive presets for the source file
        VideoPresets.Preset[] presets = VideoPresets.createAdaptivePresets(source).orElseThrow();
        VideoPresets.Preset selectedPreset = presets[0]; // Use Basic preset
        
        compressor.compress(source, output, selectedPreset.video(), selectedPreset.audio(), _ -> {});

        assertTrue(output.exists());
        assertTrue(output.length() > 0);
    }
}
