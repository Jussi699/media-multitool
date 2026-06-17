package model.helper;

import javafx.event.ActionEvent;
import javafx.scene.control.ToggleButton;
import model.logger.ErrorLogger;

import java.util.Locale;
import java.util.Optional;

public class MediaHelper {
    public static String getVideoCodec(String format, boolean useGPU) {
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "avi"             -> useGPU ? "h264_nvenc" : "mpeg4";
            case "webm"            -> "libvpx";
            case "wmv", "asf"      -> "wmv2";
            case "flv"             -> "flv1";
            case "mp4", "m4v",
                 "mov", "mkv",
                 "matroska", "3gp" -> useGPU ? "h264_nvenc" : "libx264";
            default ->  {
                ErrorLogger.error("Unexpected value: " + format);
                throw new IllegalArgumentException("Unexpected value: " + format);
            }
        };
    }

    public static String getAudioCodec(String format, boolean lossy) {
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "mp3", "flv", "avi"        -> "libmp3lame";
            case "aac"                      -> "aac";
            case "alac"                     -> "alac";
            case "wmv", "asf", "wma"        -> "wmav2";
            case "opus", "webm", "ogg"      -> "libopus";
            case "flac"                     -> "flac";
            case "wav"                      -> "pcm_s16le";
            case "aiff", "aif"              -> "pcm_s16be";
            case "mkv", "matroska",
                 "mp4", "m4v", "mov", "3gp",
                 "m4a", "m4b"               -> lossy ? "aac" : "alac";
            default -> {
                ErrorLogger.error("Unexpected format: " + format);
                throw new IllegalArgumentException("Unexpected format: " + format);
            }
        };
    }

    public static boolean supportsCodecChoice(String format) {
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "mkv", "matroska", "mp4", "m4v", "mov", "3gp", "m4a", "m4b" -> true;
            default -> false;
        };
    }

    public static String getFFmpegFormat(String format) {
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "mkv", "matroska" -> "matroska";
            case "avi"                -> "avi";
            case "webm"               -> "webm";
            case "mov"                -> "mov";
            case "wmv", "asf"         -> "asf";
            case "flv"                -> "flv";
            case "3gp"                -> "3gp";
            case "mp3"                -> "mp3";
            case "wav"                -> "wav";
            case "ogg"                -> "ogg";
            case "flac"               -> "flac";
            case "aac"                -> "adts";
            case "opus"               -> "opus";
            case "aiff", "aif"        -> "aiff";
            case "mp4", "m4v",
                 "m4a", "m4b", "alac" -> "mp4";
            default -> {
                ErrorLogger.error("Unexpected value: " + format);
                throw new IllegalArgumentException("Unexpected value: " + format);
            }
        };
    }

    public static Optional<String> selectFormatVideo(ActionEvent e) {
        ToggleButton tb = (ToggleButton) e.getSource();
        if (!tb.isSelected()) return Optional.empty();

        return switch (tb.getId()) {
            case "btnToMP4"  -> Optional.of("mp4");
            case "btnToAVI"  -> Optional.of("avi");
            case "btnToMKV"  -> Optional.of("mkv");
            case "btnToWEBM" -> Optional.of("webm");
            case "btnToMOV"  -> Optional.of("mov");
            case "btnToFLV"  -> Optional.of("flv");
            case "btnToWMV"  -> Optional.of("wmv");
            case "btnTo3GP"  -> Optional.of("3gp");
            default -> {
                ErrorLogger.error("Unexpected value: " + tb.getId());
                yield Optional.empty();
            }
        };
    }
}
