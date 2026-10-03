package model.preprocessing;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.MicrosoftTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffImageWriterLossless;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public final class PhotoTagPreprocessing {
    private PhotoTagPreprocessing() {
    }

    public static boolean supports(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".tif") || name.endsWith(".tiff");
    }

    public static Map<String, String> getTags(File file) throws IOException {
        requireSupportedFormat(file);
        try {
            ImageMetadata metadata = Imaging.getMetadata(file);
            TiffImageMetadata exif = getTiffMetadata(metadata);
            Map<String, String> tags = new LinkedHashMap<>();
            if (nonNull(exif)) {
                putIfPresent(tags, "title", exif.findField(TiffTagConstants.TIFF_TAG_DOCUMENT_NAME));
                if (!tags.containsKey("title")) {
                    putIfPresent(tags, "title", exif.findField(MicrosoftTagConstants.EXIF_TAG_XPTITLE));
                }
                putIfPresent(tags, "artist", exif.findField(TiffTagConstants.TIFF_TAG_ARTIST));
                putIfPresent(tags, "comment", exif.findField(MicrosoftTagConstants.EXIF_TAG_XPCOMMENT));
                putIfPresent(tags, "description", exif.findField(TiffTagConstants.TIFF_TAG_IMAGE_DESCRIPTION));
                putIfPresent(tags, "keywords", exif.findField(MicrosoftTagConstants.EXIF_TAG_XPKEYWORDS));
                putIfPresent(tags, "copyright", exif.findField(TiffTagConstants.TIFF_TAG_COPYRIGHT));
                putIfPresent(tags, "rating", exif.findField(MicrosoftTagConstants.EXIF_TAG_RATING));
            }
            return tags;
        } catch (Exception exception) {
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Unable to read photo tags", exception);
        }
    }

    public static void applyTags(File file, Map<String, String> values) throws IOException {
        requireSupportedFormat(file);
        Path source = file.toPath();
        Path temporary = Files.createTempFile(source.getParent(), "media-tags-", extension(file));
        try {
            TiffOutputSet outputSet = getOutputSet(file);
            updateTags(outputSet, values);
            if (isJpeg(file)) {
                try (OutputStream output = Files.newOutputStream(temporary)) {
                    new ExifRewriter().updateExifMetadataLossless(file, output, outputSet);
                }
            } else {
                byte[] original = Files.readAllBytes(source);
                try (OutputStream output = Files.newOutputStream(temporary)) {
                    new TiffImageWriterLossless(original).write(output, outputSet);
                }
            }
            replaceOriginal(temporary, source);
        } catch (Exception exception) {
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Unable to write photo tags", exception);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static TiffImageMetadata getTiffMetadata(ImageMetadata metadata) {
        if (metadata instanceof JpegImageMetadata jpegMetadata) {
            return jpegMetadata.getExif();
        }
        if (metadata instanceof TiffImageMetadata tiffMetadata) {
            return tiffMetadata;
        }
        return null;
    }

    private static TiffOutputSet getOutputSet(File file) throws Exception {
        ImageMetadata metadata = Imaging.getMetadata(file);
        TiffImageMetadata tiffMetadata = getTiffMetadata(metadata);
        if (nonNull(tiffMetadata)) {
            return tiffMetadata.getOutputSet();
        }
        if (isJpeg(file)) {
            return new TiffOutputSet(ByteOrder.LITTLE_ENDIAN);
        }
        throw new IOException("The TIFF image does not contain writable metadata.");
    }

    private static void updateTags(TiffOutputSet outputSet, Map<String, String> values) throws Exception {
        TiffOutputDirectory root = outputSet.getOrCreateRootDirectory();
        setString(root, TiffTagConstants.TIFF_TAG_DOCUMENT_NAME, values.get("title"));
        setXpString(root, MicrosoftTagConstants.EXIF_TAG_XPTITLE, values.get("title"));
        setString(root, TiffTagConstants.TIFF_TAG_ARTIST, values.get("artist"));
        setXpString(root, MicrosoftTagConstants.EXIF_TAG_XPCOMMENT, values.get("comment"));
        setString(root, TiffTagConstants.TIFF_TAG_IMAGE_DESCRIPTION, values.get("description"));
        setXpString(root, MicrosoftTagConstants.EXIF_TAG_XPKEYWORDS, values.get("keywords"));
        setString(root, TiffTagConstants.TIFF_TAG_COPYRIGHT, values.get("copyright"));

        root.removeField(MicrosoftTagConstants.EXIF_TAG_RATING);
        String rating = values.get("rating");
        if (rating != null && !rating.isBlank()) {
            short ratingValue;
            try {
                ratingValue = Short.parseShort(rating.trim());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Photo rating must be a number from 0 to 5.", exception);
            }
            if (ratingValue < 0 || ratingValue > 5) {
                throw new IllegalArgumentException("Photo rating must be a number from 0 to 5.");
            }
            root.add(MicrosoftTagConstants.EXIF_TAG_RATING, ratingValue);
        }
    }

    private static void setString(TiffOutputDirectory directory,
                                  org.apache.commons.imaging.formats.tiff.taginfos.TagInfoAscii tag,
                                  String value) throws Exception {
        directory.removeField(tag);
        if (value != null && !value.isBlank()) {
            directory.add(tag, value);
        }
    }

    private static void setXpString(TiffOutputDirectory directory,
                                    org.apache.commons.imaging.formats.tiff.taginfos.TagInfoXpString tag,
                                    String value) throws Exception {
        directory.removeField(tag);
        if (value != null && !value.isBlank()) {
            directory.add(tag, value);
        }
    }

    private static void putIfPresent(Map<String, String> tags, String key, TiffField field) throws Exception {
        if (isNull(field)) {
            return;
        }
        Object value = field.getValue();
        if (nonNull(value) && !value.toString().isBlank()) {
            tags.put(key, value.toString());
        }
    }

    private static boolean isJpeg(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

    private static String extension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? ".tmp" : name.substring(dot);
    }

    private static void requireSupportedFormat(File file) throws IOException {
        if (!supports(file)) {
            throw new IOException("Photo tag editing supports JPEG and TIFF files only.");
        }
    }

    private static void replaceOriginal(Path temporary, Path source) throws IOException {
        try {
            Files.move(temporary, source, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException _) {
            Files.move(temporary, source, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
