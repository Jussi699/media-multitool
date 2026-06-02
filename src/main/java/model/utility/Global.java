package model.utility;

import java.util.ArrayList;
import java.util.List;

public class Global {
    private final static List<String> allSupportedVideoFormat = List.of(
            ".mp4", ".avi", ".mkv", ".mov", ".webm", ".flv", ".wmv", ".3gp"
    );

    private final static List<String> allSupportedImageFormat = List.of(
            ".png", ".jpg", ".jpeg", ".ico", ".webp",
            ".tiff", ".tif", ".bmp", ".ppm", ".pgm", ".pam", ".jpe", ".svg"
    );

    private final static List<String> allSupportedAudioFormat = List.of(
            ".mp3", ".wav", ".ogg", ".flac", ".m4a", ".aac", ".wma"
    );

    public static List<String> getAllSupportedVideoFormats() {
        return allSupportedVideoFormat;
    }

    public static List<String> getAllSupportedImageFormats() {
        return allSupportedImageFormat;
    }

    public static List<String> getAllSupportedVideoFormatsForFileChooser() {
        List<String> allSupportedVideoFormatWithStar = new ArrayList<>();
        for(String format : allSupportedVideoFormat) {
               allSupportedVideoFormatWithStar.add("*".concat(format));
        }

        return allSupportedVideoFormatWithStar;
    }

    public static List<String> getAllSupportedImageFormatsForFileChooser() {
        List<String> allSupportedImageFormatWithStar = new ArrayList<>();
        for(String format : allSupportedImageFormat) {
            allSupportedImageFormatWithStar.add("*".concat(format));
        }

        return allSupportedImageFormatWithStar;
    }

    public static List<String> getAllSupportedAudioFormats() {
        return allSupportedAudioFormat;
    }

    public static List<String> getAllSupportedAudioFormatsForFileChooser() {
        List<String> allSupportedAudioFormatWithStar = new ArrayList<>();
        for(String format : allSupportedAudioFormat) {
            allSupportedAudioFormatWithStar.add("*".concat(format));
        }

        return allSupportedAudioFormatWithStar;
    }
}
