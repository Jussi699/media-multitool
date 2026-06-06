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
            ".mp3", ".wav", ".ogg", ".flac", ".m4a", ".m4b", ".aac", ".wma",
            ".alac", ".aif", ".aifc", ".aiff", ".dsf", ".mka", ".mpc", ".ofr", ".ofs",
            ".ape", ".wv", ".tak"
    );

    public static List<String> getAllSupportedVideoFormats() {
        return allSupportedVideoFormat;
    }

    public static List<String> getAllSupportedImageFormats() {
        return allSupportedImageFormat;
    }

    public static List<String> getAllSupportedAudioFormats() {
        return allSupportedAudioFormat;
    }

    public static List<String> getSupportedVideoFormatsForFileChooser() {
        List<String> supported = new ArrayList<>();
        for(String format : allSupportedVideoFormat) {
            supported.add("*".concat(format));
        }

        return supported;
    }

    public static List<String> getSupportedImageFormatsForFileChooser() {
        List<String> supported = new ArrayList<>();
        for(String format : allSupportedImageFormat) {
            supported.add("*".concat(format));
        }

        return supported;
    }

    public static List<String> getSupportedAudioFormatsForFileChooser() {
        List<String> supported = new ArrayList<>();
        for(String format : allSupportedAudioFormat) {
            supported.add("*".concat(format));
        }

        return supported;
    }
}
