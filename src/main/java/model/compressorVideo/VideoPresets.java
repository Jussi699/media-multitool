package model.compressorVideo;

import model.utility.Parsers;
import model.utility.PreparingAttributes;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.Utility.getMetadata;

public class VideoPresets {

    /**
     * @param name  preset display name
     * @param video VideoAttributes (bitRate may be ignored for CRF-capable codecs)
     * @param audio AudioAttributes
     * @param crf   Constant Rate Factor for libx264/libvpx; -1 means use ABR (bitRate) instead
     */
    public record Preset(String name, VideoAttributes video, AudioAttributes audio, int crf) {}

    public static Optional<Preset[]> createAdaptivePresets(File srcFile) {
        return createAdaptivePresets(srcFile, true);
    }

    public static Optional<Preset[]> createAdaptivePresets(File srcFile, boolean compressAudio) {
        Optional<MultimediaInfo> infoOpt = getMetadata(srcFile);
        if (infoOpt.isEmpty()) {
            return Optional.empty();
        }

        PresetSettings settings = parseSettings(infoOpt.get());
        Preset[] presets = compressAudio
                ? createCompressedAudioPresets(settings)
                : createOriginalAudioPresets(settings);
        return Optional.of(presets);
    }

    private static PresetSettings parseSettings(MultimediaInfo info) {
        int vBitrate = Parsers.parseVideoBitrate(info);
        if (vBitrate <= 0) vBitrate = 5000;
        int aBitrate = Parsers.parseAudioBitrate(info);
        if (aBitrate <= 0) aBitrate = 192;
        int fps = Parsers.parseFps(info);
        if (fps <= 0) fps = 30;
        int samplingRate = Parsers.parseSamplingRate(info);
        if (samplingRate <= 0) samplingRate = 48000;
        int channels = Parsers.parseChannels(info);
        if (channels <= 0) channels = 2;
        String resolution = Parsers.parseResolution(info).orElse("1920x1080");

        int abrBasic  = (int) (vBitrate * 0.7);
        int abrStrong = (int) (vBitrate * 0.3);
        int abrSuper  = (int) (vBitrate * 0.5);

        return new PresetSettings(fps, aBitrate, samplingRate, channels, resolution,
                computeHalfResolution(resolution), abrBasic, abrStrong, abrSuper);
    }

    private static Preset[] createOriginalAudioPresets(PresetSettings settings) {
        AudioAttributes originalAudio = PreparingAttributes.audioAttributes(
                settings.channels(), settings.samplingRate(), settings.audioBitrate(), null);

        Preset basic = new Preset("Basic",
                PreparingAttributes.videoAttributes(settings.fps(), settings.abrBasic(), null, null, settings.resolution()),
                originalAudio, 28);

        Preset strong = new Preset("Strong",
                PreparingAttributes.videoAttributes(Math.min(settings.fps(), 24), settings.abrStrong(),
                        null, null, settings.halfResolution()),
                originalAudio, 35);

        Preset superPreset = new Preset("Super",
                PreparingAttributes.videoAttributes(settings.fps(), settings.abrSuper(), null, null, settings.resolution()),
                originalAudio, 32);

        return new Preset[]{basic, strong, superPreset};
    }

    private static Preset[] createCompressedAudioPresets(PresetSettings settings) {
        // Basic: Balanced size and quality (CRF 28, 128k audio)
        Preset basic = new Preset("Basic",
                PreparingAttributes.videoAttributes(settings.fps(), settings.abrBasic(), null, null, settings.resolution()),
                PreparingAttributes.audioAttributes(2, 44100, Math.min(settings.audioBitrate(), 128), null),
                28);

        // Strong: Maximum compression (CRF 35, 64k audio, lower FPS/res)
        Preset strong = new Preset("Strong",
                PreparingAttributes.videoAttributes(Math.min(settings.fps(), 24), settings.abrStrong(),
                        null, null, settings.halfResolution()),
                PreparingAttributes.audioAttributes(1, 22050, Math.min(settings.audioBitrate(), 64), null),
                35);

        // Super: Optimized quality (CRF 32, 192k audio, original resolution)
        Preset superPreset = new Preset("Super",
                PreparingAttributes.videoAttributes(settings.fps(), settings.abrSuper(), null, null, settings.resolution()),
                PreparingAttributes.audioAttributes(2, 48000, Math.min(settings.audioBitrate(), 192), null),
                32);

        return new Preset[]{basic, strong, superPreset};
    }

    private record PresetSettings(int fps, int audioBitrate, int samplingRate, int channels,
                                  String resolution, String halfResolution,
                                  int abrBasic, int abrStrong, int abrSuper) {}

    /** Halves the resolution, ensuring even dimensions for codec compatibility. */
    private static String computeHalfResolution(String resolution) {
        try {
            String[] parts = resolution.split("x");
            int w = Integer.parseInt(parts[0]) / 2;
            int h = Integer.parseInt(parts[1]) / 2;
            if (w % 2 != 0) w--;
            if (h % 2 != 0) h--;
            return w + "x" + h;
        } catch (Exception _) {
            return resolution;
        }
    }

    public static double calculateEstimatedSizeMB(Preset preset, long durationMillis, boolean hasAudio) {
        if (isNull(preset) || durationMillis <= 0) return 0;
        int vBitrate = preset.video().getBitRate().orElse(0);
        int aBitrate = (hasAudio && nonNull(preset.audio())) ? preset.audio().getBitRate().orElse(0) : 0;
        return ((vBitrate + aBitrate) * (durationMillis / 1000.0) / 8.0) / (1024.0 * 1024.0);
    }
}
