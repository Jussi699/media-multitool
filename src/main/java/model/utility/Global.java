package model.utility;

import java.util.ArrayList;
import java.util.List;

public class Global {
    private static final List<String> allSupportedVideoFormat = List.of(
            ".mp4", ".avi", ".mkv", ".mov", ".webm", ".flv", ".wmv", ".3gp"
    );

    private static final List<String> allSupportedImageFormat = List.of(
            ".png", ".jpg", ".jpeg", ".ico", ".webp",
            ".tiff", ".tif", ".bmp", ".ppm", ".pgm", ".pam", ".jpe", ".svg"
    );

    private static final List<String> allSupportedAudioFormat = List.of(
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

    public static List<String> getAllSupportedMetadataFormats() {
        List<String> all = getAllSupportedMediaFormats();
        all.add(".pdf");
        return all;
    }

    public static List<String> getAllSupportedMediaFormats() {
        List<String> all = new ArrayList<>();
        all.addAll(allSupportedImageFormat);
        all.addAll(allSupportedAudioFormat);
        all.addAll(allSupportedVideoFormat);
        return all;
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

    public static List<String> getSupportedMediaFormatsForFileChooser() {
        List<String> supported = new ArrayList<>();
        for (String format : getAllSupportedMediaFormats()) {
            supported.add("*".concat(format));
        }
        return supported;
    }

    public static List<String> getSupportedMetadataFormatsForFileChooser() {
        List<String> supported = new ArrayList<>();
        for (String format : getAllSupportedMetadataFormats()) {
            supported.add("*".concat(format));
        }
        return supported;
    }
}
