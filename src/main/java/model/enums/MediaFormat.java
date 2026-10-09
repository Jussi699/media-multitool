package model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum MediaFormat {
    MP4("mp4", "video/mp4", MediaType.VIDEO),
    MKV("mkv", "video/x-matroska", MediaType.VIDEO),
    AVI("avi", "video/x-msvideo", MediaType.VIDEO),
    MOV("mov", "video/quicktime", MediaType.VIDEO),
    WEBM("webm", "video/webm", MediaType.VIDEO),
    WMV("wmv", "video/x-ms-wmv", MediaType.VIDEO),
    FLV("flv", "video/x-flv", MediaType.VIDEO),
    THREE_GP("3gp", "video/3gpp", MediaType.VIDEO),

    MP3("mp3", "audio/mpeg", MediaType.AUDIO),
    WAV("wav", "audio/wav", MediaType.AUDIO),
    FLAC("flac", "audio/flac", MediaType.AUDIO),
    AAC("aac", "audio/aac", MediaType.AUDIO),
    OGG("ogg", "audio/ogg", MediaType.AUDIO),
    M4A("m4a", "audio/mp4", MediaType.AUDIO),
    WMA("wma", "audio/x-ms-wma", MediaType.AUDIO),
    OPUS("opus", "audio/ogg", MediaType.AUDIO),
    ALAC("alac", "audio/mp4", MediaType.AUDIO),
    AIFF("aiff", "audio/aiff", MediaType.AUDIO),

    PNG("png", "image/png", MediaType.IMAGE),
    JPEG("jpeg", "image/jpeg", MediaType.IMAGE),
    JPG("jpg", "image/jpeg", MediaType.IMAGE),
    WEBP("webp", "image/webp", MediaType.IMAGE),
    TIFF("tif", "image/tiff", MediaType.IMAGE),
    BMP("bmp", "image/bmp", MediaType.IMAGE),
    PPM("ppm", "image/x-portable-pixmap", MediaType.IMAGE),
    PGM("pgm", "image/x-portable-graymap", MediaType.IMAGE),
    PAM("pam", "image/x-portable-arbitrarymap", MediaType.IMAGE),
    SVG("svg", "image/svg+xml", MediaType.IMAGE),
    ICO("ico", "image/x-icon", MediaType.IMAGE);

    private final String extension;
    private final String mimeType;
    private final MediaType mediaType;

    public static Optional<MediaFormat> fromButtonId(String buttonId) {
        if (buttonId == null) return Optional.empty();
        return switch (buttonId) {
            case "btnToMP4"       -> Optional.of(MP4);
            case "btnToAVI"       -> Optional.of(AVI);
            case "btnToMKV"       -> Optional.of(MKV);
            case "btnToWEBM"      -> Optional.of(WEBM);
            case "btnToMOV"       -> Optional.of(MOV);
            case "btnToFLV"       -> Optional.of(FLV);
            case "btnToWMV"       -> Optional.of(WMV);
            case "btnTo3GP"       -> Optional.of(THREE_GP);

            case "btnToMP3"       -> Optional.of(MP3);
            case "btnToAAC"       -> Optional.of(AAC);
            case "btnToOggVorbis" -> Optional.of(OGG);
            case "btnToOPUS"      -> Optional.of(OPUS);
            case "btnToFLAC"      -> Optional.of(FLAC);
            case "btnToALAC"      -> Optional.of(ALAC);
            case "btnToWAV"       -> Optional.of(WAV);
            case "btnToAIFF"      -> Optional.of(AIFF);

            case "btnToPNG"       -> Optional.of(PNG);
            case "btnToJPEG"      -> Optional.of(JPEG);
            case "btnToWEBP"      -> Optional.of(WEBP);
            case "btnToTIFF"      -> Optional.of(TIFF);
            case "btnToBMP"       -> Optional.of(BMP);
            case "btnToPPM"       -> Optional.of(PPM);
            case "btnToPAM"       -> Optional.of(PAM);
            case "btnToPGM"       -> Optional.of(PGM);
            case "btnToSVG"       -> Optional.of(SVG);

            default               -> Optional.empty();
        };
    }

    public static Optional<MediaFormat> fromExtension(String ext) {
        if (ext == null || ext.isBlank()) return Optional.empty();
        String cleanExt = ext.replace(".", "").trim();
        return Arrays.stream(values())
                .filter(f -> f.getExtension().equalsIgnoreCase(cleanExt))
                .findFirst();
    }
}
