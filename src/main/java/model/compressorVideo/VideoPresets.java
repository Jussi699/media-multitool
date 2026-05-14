package model.compressorVideo;

import model.utility.Parsers;
import model.utility.PreparingAttributes;
import model.utility.Util;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;
import java.util.Optional;

public class VideoPresets {

    public record Preset(String name, VideoAttributes video, AudioAttributes audio) {}

    public static Optional<Preset[]> createAdaptivePresets(File srcFile) {
        Optional<MultimediaInfo> infoOpt = Util.getMetadata(srcFile);
        if (infoOpt.isEmpty()) {
            return Optional.empty();
        }
        MultimediaInfo info = infoOpt.get();

        int vBitrate = Parsers.parseVideoBitrate(info);
        if (vBitrate <= 0) vBitrate = 5000;

        int aBitrate = Parsers.parseAudioBitrate(info);
        if (aBitrate <= 0) aBitrate = 192;

        int fps = Parsers.parseFps(info);
        if (fps <= 0) fps = 30;

        String resolution = Parsers.parseResolution(info).orElse("1920x1080");

        // Basic: Balanced size and quality (70% video bitrate, 128k audio)
        Preset basic = new Preset("Basic",
                PreparingAttributes.videoAttributes(fps, (int) (vBitrate * 0.7), null, null, resolution),
                PreparingAttributes.audioAttributes(2, 44100, Math.min(aBitrate, 128), null));

        // Strong: Maximum compression (30% video bitrate, 64k audio, lower FPS/res)
        String strongRes = resolution;
        try {
            String[] parts = resolution.split("x");
            int w = Integer.parseInt(parts[0]) / 2;
            int h = Integer.parseInt(parts[1]) / 2;
            // Ensure even dimensions
            if (w % 2 != 0) w--;
            if (h % 2 != 0) h--;
            strongRes = w + "x" + h;
        } catch (Exception ignored) {}

        Preset strong = new Preset("Strong",
                PreparingAttributes.videoAttributes(Math.min(fps, 24), (int) (vBitrate * 0.3), null, null, strongRes),
                PreparingAttributes.audioAttributes(1, 22050, Math.min(aBitrate, 64), null));

        // Super: Optimized high quality (50% video bitrate, 192k audio, original resolution)
        Preset superPreset = new Preset("Super",
                PreparingAttributes.videoAttributes(fps, (int) (vBitrate * 0.5), null, null, resolution),
                PreparingAttributes.audioAttributes(2, 48000, Math.min(aBitrate, 192), null));

        return Optional.of(new Preset[]{basic, strong, superPreset});
    }
}
