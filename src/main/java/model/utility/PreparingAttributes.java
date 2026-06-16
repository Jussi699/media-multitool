package model.utility;

import model.converterVideo.ConverterVideoAudioFile;
import model.logger.ErrorLogger;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.info.VideoSize;

import java.util.Optional;

public class PreparingAttributes {
    public static AudioAttributes audioAttributes(int channels, int samplingRate, int bitrate, String audioCodec) {
        AudioAttributes audio = new AudioAttributes();
        audio.setChannels(channels);
        audio.setCodec(audioCodec);
        audio.setSamplingRate(samplingRate);
        if(ConverterVideoAudioFile.shouldSetAudioBitrate(audioCodec)){
            audio.setBitRate(bitrate * 1000);
        }
        return audio;
    }

    public static VideoAttributes videoAttributes(int fps, int bitrate, String pixelFormat, String videoCodec, String resolution){
        VideoAttributes video = new VideoAttributes();
        video.setFrameRate(fps);
        video.setCodec(videoCodec);
        if (bitrate > 0 && !isLosslessVideo(videoCodec)) {
            video.setBitRate(bitrate * 1000);
        }
        video.setPixelFormat(pixelFormat);
        parseSize(resolution).ifPresent(video::setSize);
        return video;
    }

    private static boolean isLosslessVideo(String codec) {
        if (codec == null) return false;
        String c = codec.toLowerCase();
        return c.contains("png") || c.contains("ffv1") || c.contains("huffyuv");
    }

    public static Optional<VideoSize> parseSize(String resolution) {
        if (resolution != null && resolution.contains("x")) {
            try {
                String[] res = resolution.split("x");
                int width = Integer.parseInt(res[0]);
                int height = Integer.parseInt(res[1]);

                if (width % 2 != 0) width--;
                if (height % 2 != 0) height--;

                return Optional.of(new VideoSize(width, height));
            } catch (Exception e) {
                ErrorLogger.warn("Invalid resolution format: " + resolution);
            }
        }
        return Optional.empty();
    }
}
