package model.preprocessing;

import com.thebuzzmedia.exiftool.ExifTool;
import com.thebuzzmedia.exiftool.ExifToolBuilder;
import com.thebuzzmedia.exiftool.Tag;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MediaTagPreprocessing {
    private static final Map<String, Tag> TAGS = createTags();

    private MediaTagPreprocessing() {
    }

    public static Map<String, String> getTags(File file) throws IOException {
        try (ExifTool exifTool = new ExifToolBuilder().build()) {
            Map<Tag, String> values = exifTool.getImageMeta(file, TAGS.values());
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, Tag> entry : TAGS.entrySet()) {
                String value = values.get(entry.getValue());
                if (value != null && !value.isBlank()) {
                    result.put(entry.getKey(), value);
                }
            }
            return result;
        } catch (Exception exception) {
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Unable to read media tags", exception);
        }
    }

    public static void applyTags(File file, Map<String, String> values) throws IOException {
        Map<Tag, String> tags = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            Tag tag = TAGS.get(entry.getKey());
            if (tag == null) {
                throw new IllegalArgumentException("Unsupported media tag: " + entry.getKey());
            }
            tags.put(tag, entry.getValue());
        }

        try (ExifTool exifTool = new ExifToolBuilder().build()) {
            exifTool.setImageMeta(file, tags);
        } catch (Exception exception) {
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Unable to write media tags", exception);
        }
    }

    private static Map<String, Tag> createTags() {
        Map<String, Tag> tags = new LinkedHashMap<>();
        tags.put("title", new MediaTag("Title"));
        tags.put("artist", new MediaTag("Artist"));
        tags.put("album", new MediaTag("Album"));
        tags.put("albumArtist", new MediaTag("AlbumArtist"));
        tags.put("composer", new MediaTag("Composer"));
        tags.put("track", new MediaTag("Track"));
        tags.put("discNumber", new MediaTag("DiscNumber"));
        tags.put("year", new MediaTag("Year"));
        tags.put("genre", new MediaTag("Genre"));
        tags.put("comment", new MediaTag("Comment"));
        tags.put("description", new MediaTag("Description"));
        tags.put("keywords", new MediaTag("Keywords"));
        tags.put("copyright", new MediaTag("Copyright"));
        tags.put("rating", new MediaTag("Rating"));
        return tags;
    }

    private record MediaTag(String name) implements Tag {
        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDisplayName() {
            return name;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T parse(String value) {
            return (T) value;
        }
    }
}
