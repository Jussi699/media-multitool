package model.metadata;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.GpsDirectory;
import model.exceptions.MetadataProcessingException;
import model.logger.ErrorLogger;
import model.utility.media.ImageFormatDetector;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jspecify.annotations.NonNull;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.info.AudioInfo;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.info.VideoInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class MetadataHelper {
    private MetadataHelper() {
        /* This utility class should not be instantiated */
    }

    private static final String PATTERN_DATE = "yyyy-MM-dd HH:mm:ss";

    public static List<MetadataEntry> extractMetadata(File file) {
        List<MetadataEntry> entries = new ArrayList<>();
        if (isNull(file) || !file.exists()) {
            return entries;
        }

        addGeneralAttributes(file, entries);

        String ext = ImageFormatDetector.getExtensionByString(file.getName()).toLowerCase(Locale.ROOT);

        if (isAudioFormat(ext)) {
            extractAudioMetadata(file, entries);
        } else if (isPdfFormat(ext)) {
            extractPdfMetadata(file, entries);
        } else {
            boolean extractedWithDrew = extractMediaAndGpsMetadata(file, entries);

            if (isVideoFormat(ext)) {
                extractVideoTechnicalMetadata(file, entries);
            } else if (!extractedWithDrew) {
                extractAudioMetadata(file, entries);
            }
        }

        return entries;
    }

    private static void addGeneralAttributes(File file, List<MetadataEntry> entries) {
        String category = "General File Information";
        entries.add(new MetadataEntry(category, "File Name", file.getName(), false, null));
        entries.add(new MetadataEntry(category, "File Size", formatFileSize(file.length()), false, null));
        entries.add(new MetadataEntry(category, "Absolute Path", file.getAbsolutePath(), false, null));

        try {
            String mimeType = Files.probeContentType(file.toPath());
            if (nonNull(mimeType) && !mimeType.isBlank()) {
                entries.add(new MetadataEntry(category, "MIME / Content Type", mimeType, false, null));
            }
        } catch (Exception _) {
            // Ignored
        }

        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_DATE);

            String created = attrs.creationTime().toInstant().atZone(ZoneId.systemDefault()).format(formatter);
            entries.add(new MetadataEntry(category, "Creation Time", created, false, null));

            String modified = attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()).format(formatter);
            entries.add(new MetadataEntry(category, "Last Modified", modified, false, null));
        } catch (Exception _) {
            // Ignored
        }
    }

    private static boolean extractMediaAndGpsMetadata(File file, List<MetadataEntry> entries) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            if (isNull(metadata)) return false;

            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (nonNull(gpsDirectory)) {
                String catGps = "GPS & Location";
                GeoLocation geoLocation = gpsDirectory.getGeoLocation();
                if (nonNull(geoLocation) && !geoLocation.isZero()) {
                    double lat = geoLocation.getLatitude();
                    double lon = geoLocation.getLongitude();
                    String decCoords = String.format(Locale.US, "%.6f, %.6f", lat, lon);
                    entries.add(new MetadataEntry(catGps, "GPS Coordinates (Decimal)", decCoords, true, "GPS_COORDS"));
                    entries.add(new MetadataEntry(catGps, "Google Maps Link", "https://www.google.com/maps?q=" + String.format(Locale.US, "%.6f,%.6f", lat, lon), true, "GPS_LINK"));
                    entries.add(new MetadataEntry(catGps, "OpenStreetMap Link", "https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lon, true, "GPS_LINK"));
                }

                for (Tag tag : gpsDirectory.getTags()) {
                    String desc = tag.getDescription();
                    if (nonNull(desc) && !desc.isBlank()) {
                        entries.add(new MetadataEntry(catGps, tag.getTagName(), desc, true, tag.getTagName()));
                    }
                }
            }

            addToEntriesData(metadata, entries);

            return true;
        } catch (Exception e) {
            ErrorLogger.warn("Metadata extractor could not parse file: " + e.getMessage());
            return false;
        }
    }

    private static void addToEntriesData(@NonNull Metadata metadata, @NonNull List<MetadataEntry> entries) {
        for (Directory directory : metadata.getDirectories()) {
            if (directory instanceof GpsDirectory) continue;

            String dirName = directory.getName();
            String category = categorizeDirectoryName(dirName);
            boolean canDelete = isDirectoryDeletable(category);

            for (Tag tag : directory.getTags()) {
                String desc = tag.getDescription();
                if (nonNull(desc) && !desc.isBlank()) {
                    entries.add(new MetadataEntry(category, tag.getTagName(), desc, canDelete, tag.getTagName()));
                }
            }
        }
    }

    private static boolean isDirectoryDeletable(String category) {
        return "GPS & Location".equals(category)
                || "Camera & EXIF".equals(category)
                || "IPTC & Rights".equals(category)
                || "Camera MakerNotes".equals(category)
                || "Metadata".equals(category);
    }

    private static String categorizeDirectoryName(String dirName) {
        if (isNull(dirName)) return "Metadata";
        String lower = dirName.toLowerCase(Locale.ROOT);

        final boolean isGps = lower.contains("gps");
        final boolean isCameraExif = lower.contains("exif") || lower.contains("camera")
                || lower.contains("subifd") || lower.contains("ifd0");
        final boolean isIptcRights = lower.contains("iptc") || lower.contains("xmp") || lower.contains("photoshop");
        final boolean isMakerNotes = lower.contains("makernote") || lower.contains("canon") || lower.contains("nikon")
                || lower.contains("sony") || lower.contains("apple") || lower.contains("samsung");
        final boolean isVideoAudioTracks = lower.contains("quicktime") || lower.contains("mp4")
                || lower.contains("video") || lower.contains("avi");
        final boolean isImageFormat = lower.contains("png") || lower.contains("jpeg") || lower.contains("webp")
                || lower.contains("gif") || lower.contains("bmp") || lower.contains("jfif");

        if (isGps) {
            return "GPS & Location";
        } else if (isCameraExif) {
            return "Camera & EXIF";
        } else if (isIptcRights) {
            return "IPTC & Rights";
        } else if (isMakerNotes) {
            return "Camera MakerNotes";
        } else if (isVideoAudioTracks) {
            return "Video / Audio Tracks";
        } else if (isImageFormat) {
            return "Image Format Details";
        }
        return dirName;
    }

    private static void extractAudioMetadata(File file, List<MetadataEntry> entries) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            addAudioTechnicalMetadata(audioFile.getAudioHeader(), entries);
            addAudioTagMetadata(audioFile.getTag(), entries);
        } catch (Exception e) {
            ErrorLogger.warn("Could not read audio metadata: " + e.getMessage());
        }
    }

    private static void addAudioTechnicalMetadata(AudioHeader header, List<MetadataEntry> entries) {
        if (isNull(header)) {
            return;
        }

        String category = "Audio Technical";
        addIfNotEmpty(entries, category, "Encoding / Format", header.getEncodingType(), false, null);
        addIfNotEmpty(entries, category, "Bitrate", header.getBitRate() + " kbps", false, null);
        addIfNotEmpty(entries, category, "Sample Rate", header.getSampleRate() + " Hz", false, null);
        addIfNotEmpty(entries, category, "Channels", header.getChannels(), false, null);

        int seconds = header.getTrackLength();
        String duration = String.format("%02d:%02d", seconds / 60, seconds % 60);
        addIfNotEmpty(entries, category, "Duration", duration, false, null);
    }

    private static void addAudioTagMetadata(org.jaudiotagger.tag.Tag tag, List<MetadataEntry> entries) {
        if (isNull(tag)) {
            return;
        }

        String category = "Audio Tag";
        for (FieldKey key : FieldKey.values()) {
            try {
                String value = tag.getFirst(key);
                if (nonNull(value) && !value.trim().isEmpty()) {
                    entries.add(new MetadataEntry(category, formatTagKey(key.name()), value, true, key));
                }
            } catch (Exception _) {
                // Ignored
            }
        }

        var artwork = tag.getFirstArtwork();
        if (nonNull(artwork)) {
            entries.add(new MetadataEntry(category, "Embedded Artwork",
                    artwork.getMimeType() + " (" + artwork.getBinaryData().length + " bytes)",
                    true, "ARTWORK"));
        }
    }

    private static void extractPdfMetadata(File file, List<MetadataEntry> entries) {
        try (PDDocument document = Loader.loadPDF(file)) {
            addPdfDocumentMetadata(document.getDocumentInformation(), entries);
            addPdfTechnicalMetadata(document, entries);
        } catch (Exception e) {
            ErrorLogger.warn("Could not read PDF metadata: " + e.getMessage());
        }
    }

    private static void addPdfDocumentMetadata(PDDocumentInformation info, List<MetadataEntry> entries) {
        if (isNull(info)) {
            return;
        }

        String category = "PDF Information";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_DATE);

        addIfNotEmpty(entries, category, "Title", info.getTitle(), true, "Title");
        addIfNotEmpty(entries, category, "Author", info.getAuthor(), true, "Author");
        addIfNotEmpty(entries, category, "Subject", info.getSubject(), true, "Subject");
        addIfNotEmpty(entries, category, "Keywords", info.getKeywords(), true, "Keywords");
        addIfNotEmpty(entries, category, "Creator", info.getCreator(), true, "Creator");
        addIfNotEmpty(entries, category, "Producer", info.getProducer(), true, "Producer");

        if (nonNull(info.getCreationDate())) {
            entries.add(new MetadataEntry(category, "Creation Date",
                    formatDateTime(info.getCreationDate(), formatter), true, "Creation Date"));
        }
        if (nonNull(info.getModificationDate())) {
            entries.add(new MetadataEntry(category, "Modification Date",
                    formatDateTime(info.getModificationDate(), formatter), true, "Modification Date"));
        }
        addIfNotEmpty(entries, category, "Trapped", info.getTrapped(), true, "Trapped");
        addPdfCustomMetadata(info, entries, category);
    }

    private static void addPdfCustomMetadata(PDDocumentInformation info, List<MetadataEntry> entries, String category) {
        for (String customKey : info.getMetadataKeys()) {
            if (isStandardPdfKey(customKey)) {
                continue;
            }

            String customValue = info.getCustomMetadataValue(customKey);
            if (nonNull(customValue) && !customValue.isBlank()) {
                entries.add(new MetadataEntry(category, customKey, customValue, true, customKey));
            }
        }
    }

    private static void addPdfTechnicalMetadata(PDDocument document, List<MetadataEntry> entries) {
        String category = "PDF Technical";
        entries.add(new MetadataEntry(category, "Number of Pages",
                String.valueOf(document.getNumberOfPages()), false, null));
        entries.add(new MetadataEntry(category, "PDF Version", String.valueOf(document.getVersion()), false, null));
        entries.add(new MetadataEntry(category, "Is Encrypted", document.isEncrypted() ? "Yes" : "No", false, null));
    }

    private static void extractVideoTechnicalMetadata(File file, List<MetadataEntry> entries) {
        try {
            MultimediaObject multimediaObject = new MultimediaObject(file);
            MultimediaInfo info = multimediaObject.getInfo();

            if (isNull(info)) {
                return;
            }

            addVideoContainerMetadata(info, entries);
            addVideoStreamMetadata(info.getVideo(), entries);
            addAudioStreamMetadata(info.getAudio(), entries);
        } catch (Exception e) {
            ErrorLogger.warn("Could not read video metadata: " + e.getMessage());
        }
    }

    private static void addVideoContainerMetadata(MultimediaInfo info, List<MetadataEntry> entries) {
        String category = "Video / Media Container";
        addIfNotEmpty(entries, category, "Container Format", info.getFormat(), false, null);

        long durationMs = info.getDuration();
        if (durationMs <= 0) {
            return;
        }

        long totalSeconds = durationMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        String duration = hours > 0
                ? String.format("%02d:%02d:%02d", hours, minutes, seconds)
                : String.format("%02d:%02d", minutes, seconds);
        entries.add(new MetadataEntry(category, "Duration", duration, false, null));
    }

    private static void addVideoStreamMetadata(VideoInfo video, List<MetadataEntry> entries) {
        if (isNull(video)) {
            return;
        }

        String category = "Video Stream";
        addIfNotEmpty(entries, category, "Video Codec", video.getDecoder(), false, null);
        if (nonNull(video.getSize())) {
            entries.add(new MetadataEntry(category, "Resolution",
                    video.getSize().getWidth() + "x" + video.getSize().getHeight(), false, null));
        }
        if (video.getFrameRate() > 0) {
            entries.add(new MetadataEntry(category, "Frame Rate",
                    String.format("%.2f FPS", video.getFrameRate()), false, null));
        }
        if (video.getBitRate() > 0) {
            entries.add(new MetadataEntry(category, "Bitrate", (video.getBitRate() / 1000) + " kbps", false, null));
        }
    }

    private static void addAudioStreamMetadata(AudioInfo audio, List<MetadataEntry> entries) {
        if (isNull(audio)) {
            return;
        }

        String category = "Audio Stream";
        addIfNotEmpty(entries, category, "Audio Codec", audio.getDecoder(), false, null);
        if (audio.getBitRate() > 0) {
            entries.add(new MetadataEntry(category, "Bitrate", (audio.getBitRate() / 1000) + " kbps", false, null));
        }
        if (audio.getSamplingRate() > 0) {
            entries.add(new MetadataEntry(category, "Sample Rate", audio.getSamplingRate() + " Hz", false, null));
        }
        if (audio.getChannels() > 0) {
            String channels;
            if (audio.getChannels() == 2) {
                channels = "Stereo (2)";
            } else if (audio.getChannels() == 1) {
                channels = "Mono (1)";
            } else {
                channels = audio.getChannels() + " channels";
            }
            entries.add(new MetadataEntry(category, "Channels", channels, false, null));
        }
    }

    public static boolean deleteSingleTag(File file, MetadataEntry entry, File outputDir) {
        if (isNull(file) || isNull(entry) || !entry.isCanDelete()) {
            return false;
        }

        String ext = ImageFormatDetector.getExtensionByString(file.getName()).toLowerCase(Locale.ROOT);

        if (isAudioFormat(ext)) {
            return deleteSingleAudioTag(file, entry);
        } else if (isPdfFormat(ext)) {
            return deleteSinglePdfTag(file, entry, outputDir);
        } else if (isImageFormat(ext)) {
            return deleteImageMetadataTag(file, entry);
        }

        return false;
    }

    private static boolean deleteImageMetadataTag(File file, MetadataEntry entry) {
        String cat = entry.getCategory();

        try {
            return switch (cat) {
                case "GPS & Location"                     -> removeGpsFromImage(file);
                case "Camera & EXIF", "Camera MakerNotes" -> removeExifFromImage(file);
                case "IPTC & Rights"                      -> removeIptcXmpFromImage(file);
                case null, default                        -> removeExifFromImage(file);
            };
        } catch (Exception e) {
            ErrorLogger.error("Failed to delete image metadata tag: " + e.getMessage());
            return false;
        }
    }

    private static boolean removeGpsFromImage(File file) throws IOException {
        String ext = ImageFormatDetector.getExtensionByString(file.getName()).toLowerCase(Locale.ROOT);

        switch (ext) {
            case "jpg", "jpeg" -> {
                byte[] modified = removeGpsFromJpegBytes(Files.readAllBytes(file.toPath()));
                if (nonNull(modified)) {
                    Files.write(file.toPath(), modified);
                    return true;
                }
            }
            case "png" -> {
                byte[] modified = removeChunksFromPngBytes(Files.readAllBytes(file.toPath()), Set.of("eXIf", "tIME"));
                Files.write(file.toPath(), modified);
                return true;
            }
            case "webp" -> {
                byte[] modified = removeChunksFromWebpBytes(Files.readAllBytes(file.toPath()), Set.of("EXIF"));
                Files.write(file.toPath(), modified);
                return true;
            }
        }

        return reSaveCleanImage(file);
    }

    private static boolean removeExifFromImage(File file) throws IOException {
        String ext = ImageFormatDetector.getExtensionByString(file.getName()).toLowerCase(Locale.ROOT);

        return switch (ext) {
            case "jpg", "jpeg" -> {
                byte[] modified = removeJpegAppSegments(Files.readAllBytes(file.toPath()), Set.of((byte) 0xE1));
                Files.write(file.toPath(), modified);
                yield true;
            }
            case "png" -> {
                byte[] modified = removeChunksFromPngBytes(Files.readAllBytes(file.toPath()), Set.of("eXIf", "tEXt", "zTXt", "iTXt", "tIME"));
                Files.write(file.toPath(), modified);
                yield true;
            }
            case "webp" -> {
                byte[] modified = removeChunksFromWebpBytes(Files.readAllBytes(file.toPath()), Set.of("EXIF", "XMP "));
                Files.write(file.toPath(), modified);
                yield true;
            }
            default -> reSaveCleanImage(file);
        };
    }

    private static boolean removeIptcXmpFromImage(File file) throws IOException {
        String ext = ImageFormatDetector.getExtensionByString(file.getName()).toLowerCase(Locale.ROOT);

        return switch (ext) {
            case "jpg", "jpeg" -> {
                byte[] modified = removeJpegAppSegments(Files.readAllBytes(file.toPath()), Set.of((byte) 0xED));
                Files.write(file.toPath(), modified);
                yield true;
            }
            case "png" -> {
                byte[] modified = removeChunksFromPngBytes(Files.readAllBytes(file.toPath()), Set.of("tEXt", "zTXt", "iTXt"));
                Files.write(file.toPath(), modified);
                yield true;
            }
            case "webp" -> {
                byte[] modified = removeChunksFromWebpBytes(Files.readAllBytes(file.toPath()), Set.of("XMP "));
                Files.write(file.toPath(), modified);
                yield true;
            }
            default -> reSaveCleanImage(file);
        };
    }

    private static byte[] removeGpsFromJpegBytes(byte[] data) {
        if (data.length < 4 || data[0] != (byte) 0xFF || data[1] != (byte) 0xD8) {
            return null;
        }

        byte[] result = data.clone();
        int offset = 2;

        while (offset + 4 <= result.length) {
            if (result[offset] != (byte) 0xFF) break;
            byte marker = result[offset + 1];

            if (marker == (byte) 0xDA || marker == (byte) 0xD9) break; // SOS or EOI

            int len = ((result[offset + 2] & 0xFF) << 8) | (result[offset + 3] & 0xFF);
            if (offset + 2 + len > result.length) break;

            if (marker == (byte) 0xE1) { // APP1 (EXIF)
                int payloadStart = offset + 4;
                if (payloadStart + 6 <= result.length &&
                        result[payloadStart] == 'E' && result[payloadStart + 1] == 'x' &&
                        result[payloadStart + 2] == 'i' && result[payloadStart + 3] == 'f' &&
                        result[payloadStart + 4] == 0 && result[payloadStart + 5] == 0) {

                    int tiffStart = payloadStart + 6;
                    if (tiffStart + 8 <= result.length) {
                        boolean littleEndian = result[tiffStart] == 'I' && result[tiffStart + 1] == 'I';
                        ByteOrder order = littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;

                        int ifd0Offset = ByteBuffer.wrap(result, tiffStart + 4, 4).order(order).getInt();
                        int currentIfd = tiffStart + ifd0Offset;

                        if (currentIfd + 2 <= result.length) {
                            int numEntries = ByteBuffer.wrap(result, currentIfd, 2).order(order).getShort() & 0xFFFF;
                            int entryOffset = currentIfd + 2;

                            for (int i = 0; i < numEntries && entryOffset + 12 <= result.length; i++) {
                                int tag = ByteBuffer.wrap(result, entryOffset, 2).order(order).getShort() & 0xFFFF;
                                if (tag == 0x8825) {
                                    Arrays.fill(result, entryOffset, entryOffset + 12, (byte) 0);
                                    return result;
                                }
                                entryOffset += 12;
                            }
                        }
                    }
                }
            }
            offset += 2 + len;
        }

        return removeJpegAppSegments(data, Set.of((byte) 0xE1));
    }

    private static byte[] removeJpegAppSegments(byte[] data, Set<Byte> markersToRemove) {
        if (data.length < 4 || data[0] != (byte) 0xFF || data[1] != (byte) 0xD8) {
            return data;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
        baos.write(0xFF);
        baos.write(0xD8);

        int offset = 2;
        while (offset < data.length) {
            if (data[offset] != (byte) 0xFF) {
                baos.write(data, offset, data.length - offset);
                break;
            }

            if (offset + 1 >= data.length) {
                baos.write(data[offset]);
                break;
            }

            byte marker = data[offset + 1];

            if (marker == (byte) 0xDA || marker == (byte) 0xD9) {
                baos.write(data, offset, data.length - offset);
                break;
            }

            if (marker == (byte) 0x00 || (marker >= (byte) 0xD0 && marker <= (byte) 0xD7)) {
                baos.write(data[offset]);
                baos.write(marker);
                offset += 2;
                continue;
            }

            if (offset + 3 >= data.length) {
                baos.write(data, offset, data.length - offset);
                break;
            }

            int len = ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
            if (offset + 2 + len > data.length) {
                baos.write(data, offset, data.length - offset);
                break;
            }

            if (!markersToRemove.contains(marker)) {
                baos.write(data, offset, 2 + len);
            }

            offset += 2 + len;
        }

        return baos.toByteArray();
    }

    private static byte[] removeChunksFromPngBytes(byte[] data, Set<String> chunkTypesToRemove) {
        if (data.length < 8) return data;

        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
        baos.write(data, 0, 8);

        int offset = 8;
        while (offset + 8 <= data.length) {
            int length = ByteBuffer.wrap(data, offset, 4).getInt();
            String type = new String(data, offset + 4, 4, StandardCharsets.US_ASCII);

            int totalChunkLength = 4 + 4 + length + 4;
            if (offset + totalChunkLength > data.length) {
                baos.write(data, offset, data.length - offset);
                break;
            }

            if (!chunkTypesToRemove.contains(type)) {
                baos.write(data, offset, totalChunkLength);
            }

            offset += totalChunkLength;
        }

        return baos.toByteArray();
    }

    private static byte[] removeChunksFromWebpBytes(byte[] data, Set<String> chunkTypesToRemove) {
        if (data.length < 12) return data;
        String riff = new String(data, 0, 4, StandardCharsets.US_ASCII);
        String webp = new String(data, 8, 4, StandardCharsets.US_ASCII);
        if (!"RIFF".equals(riff) || !"WEBP".equals(webp)) return data;

        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
        baos.write(data, 0, 12);

        int offset = 12;
        while (offset + 8 <= data.length) {
            String fourCC = new String(data, offset, 4, StandardCharsets.US_ASCII);
            int chunkSize = ByteBuffer.wrap(data, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int paddedSize = (chunkSize % 2 == 1) ? chunkSize + 1 : chunkSize;

            int totalChunk = 8 + paddedSize;
            if (offset + totalChunk > data.length) {
                baos.write(data, offset, data.length - offset);
                break;
            }

            if (!chunkTypesToRemove.contains(fourCC)) {
                baos.write(data, offset, totalChunk);
            }

            offset += totalChunk;
        }

        byte[] result = baos.toByteArray();
        int newRiffSize = result.length - 8;
        ByteBuffer.wrap(result, 4, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(newRiffSize);
        return result;
    }

    private static boolean reSaveCleanImage(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (isNull(image)) return false;
            String ext = ImageFormatDetector.getExtensionByString(file.getName()).toLowerCase(Locale.ROOT);
            String formatName = ext.isEmpty() ? "png" : ext;
            if (formatName.equalsIgnoreCase("jpg") || formatName.equalsIgnoreCase("jpeg")) {
                BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                rgbImage.createGraphics().drawImage(image, 0, 0, java.awt.Color.WHITE, null);
                ImageIO.write(rgbImage, "jpg", file);
            } else {
                ImageIO.write(image, formatName, file);
            }
            return true;
        } catch (Exception e) {
            ErrorLogger.error("Failed to re-save clean image: " + e.getMessage());
            return false;
        }
    }

    private static boolean deleteSingleAudioTag(File file, MetadataEntry entry) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            org.jaudiotagger.tag.Tag tag = audioFile.getTag();
            if (isNull(tag)) return false;

            if (entry.getRawTagKey() instanceof FieldKey fk) {
                tag.deleteField(fk);
                audioFile.commit();
                return true;
            } else if ("ARTWORK".equals(entry.getRawTagKey())) {
                tag.deleteArtworkField();
                audioFile.commit();
                return true;
            } else if (entry.getRawTagKey() instanceof String keyStr) {
                for (FieldKey fk : FieldKey.values()) {
                    if (fk.name().equalsIgnoreCase(keyStr) || formatTagKey(fk.name()).equalsIgnoreCase(keyStr)) {
                        tag.deleteField(fk);
                        audioFile.commit();
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            ErrorLogger.error("Failed to delete audio tag: " + e.getMessage());
        }
        return false;
    }

    private static boolean deleteSinglePdfTag(File file, MetadataEntry entry, File outputDir) {
        File target = (outputDir != null && outputDir.isDirectory())
                ? new File(outputDir, file.getName())
                : file;

        try (PDDocument document = Loader.loadPDF(file)) {
            PDDocumentInformation info = document.getDocumentInformation();
            if (nonNull(info)) {
                String key = String.valueOf(entry.getRawTagKey());
                switch (key.toLowerCase(Locale.ROOT)) {
                    case "title"             -> info.setTitle(null);
                    case "author"            -> info.setAuthor(null);
                    case "subject"           -> info.setSubject(null);
                    case "keywords"          -> info.setKeywords(null);
                    case "creator"           -> info.setCreator(null);
                    case "producer"          -> info.setProducer(null);
                    case "creation date"     -> info.setCreationDate(null);
                    case "modification date" -> info.setModificationDate(null);
                    case "trapped"           -> info.setTrapped(null);
                    default                  -> info.setCustomMetadataValue(key, null);
                }
                document.save(target);
                return true;
            }
        } catch (Exception e) {
            ErrorLogger.error("Failed to delete PDF tag: " + e.getMessage());
        }
        return false;
    }

    public static File removeAllMetadata(File inputFile, File outputDirectory)
            throws IOException, MetadataProcessingException {
        if (isNull(inputFile) || !inputFile.exists()) {
            throw new IllegalArgumentException("Input file does not exist");
        }

        String ext = ImageFormatDetector.getExtensionByString(inputFile.getName()).toLowerCase(Locale.ROOT);
        if (isAudioFormat(ext)) {
            return removeAllAudioMetadata(inputFile, createCleanOutputFile(inputFile, outputDirectory));
        }
        if (isPdfFormat(ext)) {
            return removeAllPdfMetadata(inputFile, createCleanOutputFile(inputFile, outputDirectory));
        }
        if (isImageFormat(ext)) {
            return removeAllImageMetadata(inputFile, ext, createCleanOutputFile(inputFile, outputDirectory));
        }
        if (isVideoFormat(ext)) {
            return removeAllVideoMetadata(inputFile, ext, createCleanOutputFile(inputFile, outputDirectory));
        }

        throw new UnsupportedOperationException("Unsupported format for metadata removal: " + ext);
    }

    private static File createCleanOutputFile(File inputFile, File outputDirectory) {
        File targetDirectory = nonNull(outputDirectory) && outputDirectory.isDirectory()
                ? outputDirectory
                : inputFile.getParentFile();
        String baseName = getBaseName(inputFile.getName());
        String extension = ImageFormatDetector.getExtensionByString(inputFile.getName()).toLowerCase(Locale.ROOT);
        return new File(targetDirectory, baseName + "_clean." + extension);
    }

    private static File removeAllAudioMetadata(File inputFile, File outputFile) throws MetadataProcessingException {
        try {
            Files.copy(inputFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            AudioFile audioFile = AudioFileIO.read(outputFile);
            org.jaudiotagger.tag.Tag tag = audioFile.getTag();
            if (nonNull(tag)) {
                tag.deleteArtworkField();
                for (FieldKey fieldKey : FieldKey.values()) {
                    try {
                        tag.deleteField(fieldKey);
                    } catch (Exception _) {
                        // Ignored
                    }
                }
                audioFile.commit();
            }
            return outputFile;
        } catch (IOException | CannotReadException | TagException | ReadOnlyFileException
                 | InvalidAudioFrameException | CannotWriteException e) {
            throw new MetadataProcessingException("Could not remove audio metadata from " + inputFile.getName(), e);
        }
    }

    private static File removeAllPdfMetadata(File inputFile, File outputFile) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputFile)) {
            document.setDocumentInformation(new PDDocumentInformation());
            if (nonNull(document.getDocumentCatalog())) {
                document.getDocumentCatalog().setMetadata(null);
            }
            document.save(outputFile);
            return outputFile;
        }
    }

    private static File removeAllImageMetadata(File inputFile, String extension, File outputFile) throws IOException {
        BufferedImage image = ImageIO.read(inputFile);
        if (isNull(image)) {
            throw new IOException("Unable to decode image");
        }

        String formatName = extension.isEmpty() ? "png" : extension;
        if (formatName.equalsIgnoreCase("jpg") || formatName.equalsIgnoreCase("jpeg")) {
            BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgbImage.createGraphics().drawImage(image, 0, 0, java.awt.Color.WHITE, null);
            ImageIO.write(rgbImage, "jpg", outputFile);
        } else {
            ImageIO.write(image, formatName, outputFile);
        }
        return outputFile;
    }

    private static File removeAllVideoMetadata(File inputFile, String extension, File outputFile)
            throws MetadataProcessingException {
        try {
            MultimediaObject multimediaObject = new MultimediaObject(inputFile);
            MultimediaInfo info = multimediaObject.getInfo();

            EncodingAttributes attributes = new EncodingAttributes();
            attributes.setOutputFormat(extension);
            if (nonNull(info) && nonNull(info.getVideo())) {
                VideoAttributes videoAttributes = new VideoAttributes();
                videoAttributes.setCodec("copy");
                attributes.setVideoAttributes(videoAttributes);
            }
            if (nonNull(info) && nonNull(info.getAudio())) {
                AudioAttributes audioAttributes = new AudioAttributes();
                audioAttributes.setCodec("copy");
                attributes.setAudioAttributes(audioAttributes);
            }

            new Encoder().encode(multimediaObject, outputFile, attributes);
            return outputFile;
        } catch (EncoderException e) {
            throw new MetadataProcessingException("Could not remove video metadata from " + inputFile.getName(), e);
        }
    }

    public static String exportMetadataToText(File file, List<MetadataEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== METADATA REPORT ===\n");
        sb.append("File: ").append(nonNull(file) ? file.getName() : "Unknown").append("\n");
        sb.append("Path: ").append(nonNull(file) ? file.getAbsolutePath() : "").append("\n");
        sb.append("Generated on: ")
                .append(LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(PATTERN_DATE)))
                .append("\n\n");

        String currentCategory = "";
        for (MetadataEntry entry : entries) {
            if (!entry.getCategory().equals(currentCategory)) {
                currentCategory = entry.getCategory();
                sb.append("[").append(currentCategory).append("]\n");
            }
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    private static String formatDateTime(Calendar calendar, DateTimeFormatter formatter) {
        return LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.systemDefault()).format(formatter);
    }

    private static boolean isAudioFormat(String ext) {
        return List.of("mp3", "wav", "flac", "ogg", "m4a", "aac", "wma", "aiff", "alac", "ape", "wv").contains(ext);
    }

    private static boolean isPdfFormat(String ext) {
        return "pdf".equals(ext);
    }

    private static boolean isVideoFormat(String ext) {
        return List.of("mp4", "avi", "mkv", "mov", "webm", "flv", "wmv", "3gp", "ts", "m4v").contains(ext);
    }

    private static boolean isImageFormat(String ext) {
        return List.of("jpg", "jpeg", "png", "webp", "tiff", "tif", "bmp", "gif", "ico", "svg", "ppm", "pgm").contains(ext);
    }

    private static void addIfNotEmpty(List<MetadataEntry> list, String cat, String key, String val, boolean canDelete, Object rawKey) {
        if (nonNull(val) && !val.trim().isEmpty()) {
            list.add(new MetadataEntry(cat, key, val, canDelete, rawKey));
        }
    }

    private static boolean isStandardPdfKey(String key) {
        return List.of("Title", "Author", "Subject", "Keywords", "Creator", "Producer", "CreationDate", "ModDate", "Trapped").contains(key);
    }

    private static String formatTagKey(String raw) {
        if (isNull(raw)) return "";
        String lower = raw.replace('_', ' ').toLowerCase(Locale.ROOT);
        StringBuilder res = new StringBuilder();
        for (String word : lower.split(" ")) {
            if (!word.isEmpty()) {
                res.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return res.toString().trim();
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.2f %cB (%d bytes)", bytes / Math.pow(1024, exp), pre, bytes);
    }

    private static String getBaseName(String fileName) {
        if (isNull(fileName)) return "output";
        int dot = fileName.lastIndexOf('.');
        return dot == -1 ? fileName : fileName.substring(0, dot);
    }
}
