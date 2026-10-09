package model.helper;

import javafx.event.ActionEvent;
import javafx.scene.control.ToggleButton;
import model.enums.MediaFormat;
import model.logger.ErrorLogger;

import java.util.Locale;
import java.util.Optional;

import static java.util.Objects.isNull;

public class MediaHelper {
    private MediaHelper() {
        /* This utility class should not be instantiated */
    }

    public static String getVideoCodec(MediaFormat format, boolean useGPU) {
        if (isNull(format)) {
            throw new IllegalArgumentException("Format cannot be null");
        }
        return switch (format) {
            case AVI                     -> useGPU ? "h264_nvenc" : "mpeg4";
            case WEBM                    -> "libvpx";
            case WMV                     -> "wmv2";
            case FLV                     -> "flv1";
            case MP4, MOV, MKV, THREE_GP -> useGPU ? "h264_nvenc" : "libx264";
            default -> {
                ErrorLogger.error("Unsupported video format: " + format);
                throw new IllegalArgumentException("Unsupported video format: " + format);
            }
        };
    }

    public static String getAudioCodec(MediaFormat format, boolean lossy) {
        if (isNull(format)) {
            throw new IllegalArgumentException("Format cannot be null");
        }
        return switch (format) {
            case MP3, FLV, AVI                -> "libmp3lame";
            case AAC                          -> "aac";
            case ALAC                         -> "alac";
            case WMV, WMA                     -> "wmav2";
            case OPUS, WEBM, OGG              -> "libopus";
            case FLAC                         -> "flac";
            case WAV                          -> "pcm_s16le";
            case AIFF                         -> "pcm_s16be";
            case MKV, MP4, MOV, THREE_GP, M4A -> lossy ? "aac" : "alac";
            default -> {
                ErrorLogger.error("Unsupported audio format: " + format);
                throw new IllegalArgumentException("Unsupported audio format: " + format);
            }
        };
    }

    public static boolean supportsCodecChoice(MediaFormat format) {
        if (isNull(format)) return false;
        return switch (format) {
            case MKV, MP4, MOV, THREE_GP, M4A, ALAC -> true;
            default -> false;
        };
    }

    public static String getFFmpegFormat(MediaFormat format) {
        if (isNull(format)) {
            ErrorLogger.error("Unexpected format: null");
            throw new IllegalArgumentException("Unexpected format: null");
        }
        return switch (format) {
            case MKV                   -> "matroska";
            case AVI                   -> "avi";
            case WEBM                  -> "webm";
            case MOV                   -> "mov";
            case WMV, WMA              -> "asf";
            case FLV                   -> "flv";
            case THREE_GP              -> "3gp";
            case MP3                   -> "mp3";
            case WAV                   -> "wav";
            case OGG                   -> "ogg";
            case FLAC                  -> "flac";
            case AAC                   -> "adts";
            case OPUS                  -> "opus";
            case AIFF                  -> "aiff";
            case MP4, M4A, ALAC        -> "mp4";
            default -> {
                ErrorLogger.error("Unsupported FFmpeg format: " + format);
                throw new IllegalArgumentException("Unsupported FFmpeg format: " + format);
            }
        };
    }

    public static String getFFmpegFormat(String format) {
        if (isNull(format)) {
            ErrorLogger.error("Unexpected format: null");
            throw new IllegalArgumentException("Unexpected format: null");
        }
        return MediaFormat.fromExtension(format)
                .map(MediaHelper::getFFmpegFormat)
                .orElse(format.toLowerCase(Locale.ROOT));
    }

    public static Optional<MediaFormat> selectFormat(ActionEvent e) {
        if (!(e.getSource() instanceof ToggleButton tb) || !tb.isSelected()) {
            return Optional.empty();
        }
        return MediaFormat.fromButtonId(tb.getId());
    }
}
