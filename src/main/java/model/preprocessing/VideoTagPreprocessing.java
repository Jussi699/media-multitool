package model.preprocessing;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.ArgType;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingArgument;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.ValueArgument;
import ws.schild.jave.encode.VideoAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class VideoTagPreprocessing {
    private static final Map<String, String> FFMPEG_TAGS = createFfmpegTags();

    private VideoTagPreprocessing() {
    }

    public static Map<String, String> getTags(File file) throws IOException {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file)) {
            grabber.start();
            Map<String, String> metadata = new LinkedHashMap<>(grabber.getMetadata());
            mergeIfMissing(metadata, grabber.getVideoMetadata());
            mergeIfMissing(metadata, grabber.getAudioMetadata());
            return normalizeTags(metadata);
        } catch (Exception exception) {
            throw new IOException("Unable to read video tags", exception);
        }
    }

    public static void applyTags(File file, Map<String, String> values) throws IOException {
        Map<String, String> supportedValues = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String ffmpegKey = FFMPEG_TAGS.get(entry.getKey());
            if (ffmpegKey == null) {
                throw new IllegalArgumentException("Unsupported video tag: " + entry.getKey());
            }
            supportedValues.put(ffmpegKey, entry.getValue() == null ? "" : entry.getValue());
        }

        Path source = file.toPath();
        Path temporary = Files.createTempFile(source.getParent(), "media-tags-", extension(file));
        try {
            EncodingAttributes attributes = new EncodingAttributes()
                    .setMapMetaData(true)
                    .setVideoAttributes(new VideoAttributes().setCodec(VideoAttributes.DIRECT_STREAM_COPY))
                    .setAudioAttributes(new AudioAttributes().setCodec(AudioAttributes.DIRECT_STREAM_COPY));
            List<EncodingArgument> arguments = supportedValues.entrySet().stream()
                    .map(entry -> (EncodingArgument) new ValueArgument(
                            ArgType.OUTFILE,
                            "-metadata",
                            ignored -> Optional.of(entry.getKey() + "=" + entry.getValue())))
                    .toList();
            Encoder encoder = new Encoder();
            encoder.encode(List.of(new MultimediaObject(file)), temporary.toFile(), attributes, null, arguments);
            replaceOriginal(temporary, source);
        } catch (Exception exception) {
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Unable to write video tags", exception);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Map<String, String> normalizeTags(Map<String, String> metadata) {
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            String field = switch (key) {
                case "title" -> "title";
                case "artist", "author" -> "artist";
                case "album" -> "album";
                case "album_artist", "albumartist" -> "albumArtist";
                case "composer" -> "composer";
                case "track", "tracknumber" -> "track";
                case "disc", "discnumber" -> "discNumber";
                case "date", "year", "creation_time" -> "year";
                case "genre" -> "genre";
                case "comment" -> "comment";
                case "description", "synopsis" -> "description";
                case "keywords" -> "keywords";
                case "copyright" -> "copyright";
                case "rating" -> "rating";
                default -> null;
            };
            if (field != null && entry.getValue() != null && !entry.getValue().isBlank()) {
                String value = entry.getValue();
                if (field.equals("year") && value.length() >= 4) {
                    value = value.substring(0, 4);
                }
                normalized.putIfAbsent(field, value);
            }
        }
        return normalized;
    }

    private static void mergeIfMissing(Map<String, String> target, Map<String, String> additional) {
        additional.forEach(target::putIfAbsent);
    }

    private static Map<String, String> createFfmpegTags() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("title", "title");
        tags.put("artist", "artist");
        tags.put("album", "album");
        tags.put("albumArtist", "album_artist");
        tags.put("composer", "composer");
        tags.put("track", "track");
        tags.put("discNumber", "disc");
        tags.put("year", "date");
        tags.put("genre", "genre");
        tags.put("comment", "comment");
        tags.put("description", "description");
        tags.put("keywords", "keywords");
        tags.put("copyright", "copyright");
        tags.put("rating", "rating");
        return Map.copyOf(tags);
    }

    private static String extension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? ".tmp" : name.substring(dot);
    }

    private static void replaceOriginal(Path temporary, Path source) throws IOException {
        try {
            Files.move(temporary, source, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException _) {
            Files.move(temporary, source, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
