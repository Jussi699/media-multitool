package model.compressorVideo;

import model.utility.Parsers;
import model.utility.PreparingAttributes;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;
import java.util.Optional;

import static viewHelp.Utility.getMetadata;

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
        MultimediaInfo info = infoOpt.get();

        int vBitrate = Parsers.parseVideoBitrate(info);
        boolean bitrateUnknown = vBitrate <= 0;
        if (bitrateUnknown) vBitrate = 5000;

        int aBitrate = Parsers.parseAudioBitrate(info);
        if (aBitrate <= 0) aBitrate = 192;

        int fps = Parsers.parseFps(info);
        if (fps <= 0) fps = 30;

        int samplingRate = Parsers.parseSamplingRate(info);
        if (samplingRate <= 0) samplingRate = 48000;

        int channels = Parsers.parseChannels(info);
        if (channels <= 0) channels = 2;

        String resolution = Parsers.parseResolution(info).orElse("1920x1080");

        // CRF values for libx264 (lower = better quality, larger file)
        // Basic=28: balanced quality, Strong=35: maximum compression, Super=32: between both
        int crfBasic  = 28;
        int crfStrong = 35;
        int crfSuper  = 32;

        // ABR fallback bitrates (used for GPU/mpeg4 codecs that don't support CRF via JAVE)
        int abrBasic  = (int) (vBitrate * 0.7);
        int abrStrong = (int) (vBitrate * 0.3);
        int abrSuper  = (int) (vBitrate * 0.5);

        String strongRes = computeHalfResolution(resolution);

        if (!compressAudio) {
            AudioAttributes originalAudio = PreparingAttributes.audioAttributes(channels, samplingRate, aBitrate, null);

            Preset basic  = new Preset("Basic",  PreparingAttributes.videoAttributes(fps,                 abrBasic,  null, null, resolution), originalAudio, crfBasic);
            Preset strong = new Preset("Strong", PreparingAttributes.videoAttributes(Math.min(fps, 24),   abrStrong, null, null, strongRes),  originalAudio, crfStrong);
            Preset superP = new Preset("Super",  PreparingAttributes.videoAttributes(fps,                 abrSuper,  null, null, resolution), originalAudio, crfSuper);

            return Optional.of(new Preset[]{basic, strong, superP});
        }

        // Basic: Balanced size and quality (CRF 28, 128k audio)
        Preset basic = new Preset("Basic",
                PreparingAttributes.videoAttributes(fps, abrBasic, null, null, resolution),
                PreparingAttributes.audioAttributes(2, 44100, Math.min(aBitrate, 128), null),
                crfBasic);

        // Strong: Maximum compression (CRF 35, 64k audio, lower FPS/res)
        Preset strong = new Preset("Strong",
                PreparingAttributes.videoAttributes(Math.min(fps, 24), abrStrong, null, null, strongRes),
                PreparingAttributes.audioAttributes(1, 22050, Math.min(aBitrate, 64), null),
                crfStrong);

        // Super: Optimized quality (CRF 32, 192k audio, original resolution)
        Preset superPreset = new Preset("Super",
                PreparingAttributes.videoAttributes(fps, abrSuper, null, null, resolution),
                PreparingAttributes.audioAttributes(2, 48000, Math.min(aBitrate, 192), null),
                crfSuper);

        return Optional.of(new Preset[]{basic, strong, superPreset});
    }

    /** Halves the resolution, ensuring even dimensions for codec compatibility. */
    private static String computeHalfResolution(String resolution) {
        try {
            String[] parts = resolution.split("x");
            int w = Integer.parseInt(parts[0]) / 2;
            int h = Integer.parseInt(parts[1]) / 2;
            if (w % 2 != 0) w--;
            if (h % 2 != 0) h--;
            return w + "x" + h;
        } catch (Exception ignored) {
            return resolution;
        }
    }
}
