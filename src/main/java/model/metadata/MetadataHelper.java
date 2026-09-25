package model.metadata;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.GpsDirectory;
import model.logger.ErrorLogger;
import model.utility.DetermineType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import ws.schild.jave.Encoder;
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
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MetadataHelper {
    private MetadataHelper() {}

    private static final String PATTERN_DATE = "yyyy-MM-dd HH:mm:ss";

    public static List<MetadataEntry> extractMetadata(File file) {
        List<MetadataEntry> entries = new ArrayList<>();
        if (file == null || !file.exists()) {
            return entries;
        }

        addGeneralAttributes(file, entries);

        String ext = DetermineType.getExtensionByString(file.getName()).toLowerCase(Locale.ROOT);

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
            if (mimeType != null && !mimeType.isBlank()) {
                entries.add(new MetadataEntry(category, "MIME / Content Type", mimeType, false, null));
            }
        } catch (Exception _) {}

        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_DATE);

            String created = attrs.creationTime().toInstant().atZone(ZoneId.systemDefault()).format(formatter);
            entries.add(new MetadataEntry(category, "Creation Time", created, false, null));

            String modified = attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()).format(formatter);
            entries.add(new MetadataEntry(category, "Last Modified", modified, false, null));
        } catch (Exception _) {}
    }

    private static boolean extractMediaAndGpsMetadata(File file, List<MetadataEntry> entries) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            if (metadata == null) return false;

            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDirectory != null) {
                String catGps = "GPS & Location";
                GeoLocation geoLocation = gpsDirectory.getGeoLocation();
                if (geoLocation != null && !geoLocation.isZero()) {
                    double lat = geoLocation.getLatitude();
                    double lon = geoLocation.getLongitude();
                    String decCoords = String.format(Locale.US, "%.6f, %.6f", lat, lon);
                    entries.add(new MetadataEntry(catGps, "GPS Coordinates (Decimal)", decCoords, true, "GPS_COORDS"));
                    entries.add(new MetadataEntry(catGps, "Google Maps Link", "https://www.google.com/maps?q=" + String.format(Locale.US, "%.6f,%.6f", lat, lon), true, "GPS_LINK"));
                    entries.add(new MetadataEntry(catGps, "OpenStreetMap Link", "https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lon, true, "GPS_LINK"));
                }

                for (Tag tag : gpsDirectory.getTags()) {
                    String desc = tag.getDescription();
                    if (desc != null && !desc.isBlank()) {
                        entries.add(new MetadataEntry(catGps, tag.getTagName(), desc, true, tag.getTagName()));
                    }
                }
            }

            for (Directory directory : metadata.getDirectories()) {
                if (directory instanceof GpsDirectory) continue;

                String dirName = directory.getName();
                String category = categorizeDirectoryName(dirName);
                boolean canDelete = isDirectoryDeletable(category);

                for (Tag tag : directory.getTags()) {
                    String desc = tag.getDescription();
                    if (desc != null && !desc.isBlank()) {
                        entries.add(new MetadataEntry(category, tag.getTagName(), desc, canDelete, tag.getTagName()));
                    }
                }
            }

            return true;
        } catch (Exception e) {
            ErrorLogger.warn("Metadata extractor could not parse file: " + e.getMessage());
            return false;
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
        if (dirName == null) return "Metadata";
        String lower = dirName.toLowerCase(Locale.ROOT);
        if (lower.contains("gps")) {
            return "GPS & Location";
        } else if (lower.contains("exif") || lower.contains("camera") || lower.contains("subifd") || lower.contains("ifd0")) {
            return "Camera & EXIF";
        } else if (lower.contains("iptc") || lower.contains("xmp") || lower.contains("photoshop")) {
            return "IPTC & Rights";
        } else if (lower.contains("makernote") || lower.contains("canon") || lower.contains("nikon") || lower.contains("sony") || lower.contains("apple") || lower.contains("samsung")) {
            return "Camera MakerNotes";
        } else if (lower.contains("quicktime") || lower.contains("mp4") || lower.contains("video") || lower.contains("avi")) {
            return "Video / Audio Tracks";
        } else if (lower.contains("png") || lower.contains("jpeg") || lower.contains("webp") || lower.contains("gif") || lower.contains("bmp") || lower.contains("jfif")) {
            return "Image Format Details";
        }
        return dirName;
    }

    private static void extractAudioMetadata(File file, List<MetadataEntry> entries) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader header = audioFile.getAudioHeader();

            if (header != null) {
                String catHeader = "Audio Technical";
                addIfNotEmpty(entries, catHeader, "Encoding / Format", header.getEncodingType(), false, null);
                addIfNotEmpty(entries, catHeader, "Bitrate", header.getBitRate() + " kbps", false, null);
                addIfNotEmpty(entries, catHeader, "Sample Rate", header.getSampleRate() + " Hz", false, null);
                addIfNotEmpty(entries, catHeader, "Channels", header.getChannels(), false, null);

                int sec = header.getTrackLength();
                String duration = String.format("%02d:%02d", sec / 60, sec % 60);
                addIfNotEmpty(entries, catHeader, "Duration", duration, false, null);
            }

            org.jaudiotagger.tag.Tag tag = audioFile.getTag();
            if (tag != null) {
                String catTag = "Audio Tag";
                for (FieldKey key : FieldKey.values()) {
                    try {
                        String val = tag.getFirst(key);
                        if (val != null && !val.trim().isEmpty()) {
                            entries.add(new MetadataEntry(catTag, formatTagKey(key.name()), val, true, key));
                        }
                    } catch (Exception _) {}
                }

                if (tag.getFirstArtwork() != null) {
                    entries.add(new MetadataEntry(catTag, "Embedded Artwork",
                            tag.getFirstArtwork().getMimeType() + " (" + tag.getFirstArtwork().getBinaryData().length + " bytes)",
                            true, "ARTWORK"));
                }
            }
        } catch (Exception e) {
            ErrorLogger.warn("Could not read audio metadata: " + e.getMessage());
        }
    }

    private static void extractPdfMetadata(File file, List<MetadataEntry> entries) {
        try (PDDocument document = Loader.loadPDF(file)) {
            String catDoc = "PDF Information";
            PDDocumentInformation info = document.getDocumentInformation();

            if (info != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_DATE);

                addIfNotEmpty(entries, catDoc, "Title", info.getTitle(), true, "Title");
                addIfNotEmpty(entries, catDoc, "Author", info.getAuthor(), true, "Author");
                addIfNotEmpty(entries, catDoc, "Subject", info.getSubject(), true, "Subject");
                addIfNotEmpty(entries, catDoc, "Keywords", info.getKeywords(), true, "Keywords");
                addIfNotEmpty(entries, catDoc, "Creator", info.getCreator(), true, "Creator");
                addIfNotEmpty(entries, catDoc, "Producer", info.getProducer(), true, "Producer");

                if (info.getCreationDate() != null) {
                    entries.add(new MetadataEntry(catDoc, "Creation Date",
                            formatDateTime(info.getCreationDate(), formatter), true, "Creation Date"));
                }
                if (info.getModificationDate() != null) {
                    entries.add(new MetadataEntry(catDoc, "Modification Date",
                            formatDateTime(info.getModificationDate(), formatter), true, "Modification Date"));
                }
                addIfNotEmpty(entries, catDoc, "Trapped", info.getTrapped(), true, "Trapped");

                for (String customKey : info.getMetadataKeys()) {
                    if (!isStandardPdfKey(customKey)) {
                        String customVal = info.getCustomMetadataValue(customKey);
                        if (customVal != null && !customVal.isBlank()) {
                            entries.add(new MetadataEntry(catDoc, customKey, customVal, true, customKey));
                        }
                    }
                }
            }

            String catTech = "PDF Technical";
            entries.add(new MetadataEntry(catTech, "Number of Pages", String.valueOf(document.getNumberOfPages()), false, null));
            entries.add(new MetadataEntry(catTech, "PDF Version", String.valueOf(document.getVersion()), false, null));
            entries.add(new MetadataEntry(catTech, "Is Encrypted", document.isEncrypted() ? "Yes" : "No", false, null));

        } catch (Exception e) {
            ErrorLogger.warn("Could not read PDF metadata: " + e.getMessage());
        }
    }

    private static void extractVideoTechnicalMetadata(File file, List<MetadataEntry> entries) {
        try {
            MultimediaObject multimediaObject = new MultimediaObject(file);
            MultimediaInfo info = multimediaObject.getInfo();

            if (info == null) {
                return;
            }

            String catFormat = "Video / Media Container";
            addIfNotEmpty(entries, catFormat, "Container Format", info.getFormat(), false, null);

            long durationMs = info.getDuration();
            if (durationMs > 0) {
                long totalSec = durationMs / 1000;
                long hours = totalSec / 3600;
                long minutes = (totalSec % 3600) / 60;
                long seconds = totalSec % 60;
                String durStr = (hours > 0)
                        ? String.format("%02d:%02d:%02d", hours, minutes, seconds)
                        : String.format("%02d:%02d", minutes, seconds);
                entries.add(new MetadataEntry(catFormat, "Duration", durStr, false, null));
            }

            VideoInfo video = info.getVideo();
            if (video != null) {
                String catVideo = "Video Stream";
                addIfNotEmpty(entries, catVideo, "Video Codec", video.getDecoder(), false, null);
                if (video.getSize() != null) {
                    entries.add(new MetadataEntry(catVideo, "Resolution", video.getSize().getWidth() + "x" + video.getSize().getHeight(), false, null));
                }
                if (video.getFrameRate() > 0) {
                    entries.add(new MetadataEntry(catVideo, "Frame Rate", String.format("%.2f FPS", video.getFrameRate()), false, null));
                }
                if (video.getBitRate() > 0) {
                    entries.add(new MetadataEntry(catVideo, "Bitrate", (video.getBitRate() / 1000) + " kbps", false, null));
                }
            }

            AudioInfo audio = info.getAudio();
            if (audio != null) {
                String catAudio = "Audio Stream";
                addIfNotEmpty(entries, catAudio, "Audio Codec", audio.getDecoder(), false, null);
                if (audio.getBitRate() > 0) {
                    entries.add(new MetadataEntry(catAudio, "Bitrate", (audio.getBitRate() / 1000) + " kbps", false, null));
                }
                if (audio.getSamplingRate() > 0) {
                    entries.add(new MetadataEntry(catAudio, "Sample Rate", audio.getSamplingRate() + " Hz", false, null));
                }
                if (audio.getChannels() > 0) {
                    String chan = audio.getChannels() == 2 ? "Stereo (2)" : (audio.getChannels() == 1 ? "Mono (1)" : audio.getChannels() + " channels");
                    entries.add(new MetadataEntry(catAudio, "Channels", chan, false, null));
                }
            }

        } catch (Exception e) {
            ErrorLogger.warn("Could not read video metadata: " + e.getMessage());
        }
    }

    public static boolean deleteSingleTag(File file, MetadataEntry entry, File outputDir) {
        if (file == null || entry == null || !entry.isCanDelete()) {
            return false;
        }

        String ext = DetermineType.getExtensionByString(file.getName()).toLowerCase(Locale.ROOT);

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
        String ext = DetermineType.getExtensionByString(file.getName()).toLowerCase(Locale.ROOT);
        byte[] bytes = Files.readAllBytes(file.toPath());

        switch (ext) {
            case "jpg", "jpeg" -> {
                byte[] modified = removeGpsFromJpegBytes(bytes);
                if (modified != null) {
                    Files.write(file.toPath(), modified);
                    return true;
                }
            }
            case "png" -> {
                byte[] modified = removeChunksFromPngBytes(bytes, Set.of("eXIf", "tIME"));
                Files.write(file.toPath(), modified);
                return true;
            }
            case "webp" -> {
                byte[] modified = removeChunksFromWebpBytes(bytes, Set.of("EXIF"));
                Files.write(file.toPath(), modified);
                return true;
            }
        }

        return reSaveCleanImage(file);
    }

    private static boolean removeExifFromImage(File file) throws IOException {
        String ext = DetermineType.getExtensionByString(file.getName()).toLowerCase(Locale.ROOT);
        byte[] bytes = Files.readAllBytes(file.toPath());

        switch (ext) {
            case "jpg", "jpeg" -> {
                byte[] modified = removeJpegAppSegments(bytes, Set.of((byte) 0xE1));
                Files.write(file.toPath(), modified);
                return true;
            }
            case "png" -> {
                byte[] modified = removeChunksFromPngBytes(bytes, Set.of("eXIf", "tEXt", "zTXt", "iTXt", "tIME"));
                Files.write(file.toPath(), modified);
                return true;
            }
            case "webp" -> {
                byte[] modified = removeChunksFromWebpBytes(bytes, Set.of("EXIF", "XMP "));
                Files.write(file.toPath(), modified);
                return true;
            }
        }

        return reSaveCleanImage(file);
    }

    private static boolean removeIptcXmpFromImage(File file) throws IOException {
        String ext = DetermineType.getExtensionByString(file.getName()).toLowerCase(Locale.ROOT);
        byte[] bytes = Files.readAllBytes(file.toPath());

        switch (ext) {
            case "jpg", "jpeg" -> {
                byte[] modified = removeJpegAppSegments(bytes, Set.of((byte) 0xED));
                Files.write(file.toPath(), modified);
                return true;
            }
            case "png" -> {
                byte[] modified = removeChunksFromPngBytes(bytes, Set.of("tEXt", "zTXt", "iTXt"));
                Files.write(file.toPath(), modified);
                return true;
            }
            case "webp" -> {
                byte[] modified = removeChunksFromWebpBytes(bytes, Set.of("XMP "));
                Files.write(file.toPath(), modified);
                return true;
            }
        }

        return reSaveCleanImage(file);
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
            if (image == null) return false;
            String ext = DetermineType.getExtensionByString(file.getName()).toLowerCase(Locale.ROOT);
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
            if (tag == null) return false;

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
            if (info != null) {
                String key = String.valueOf(entry.getRawTagKey());
                switch (key.toLowerCase(Locale.ROOT)) {
                    case "title" -> info.setTitle(null);
                    case "author" -> info.setAuthor(null);
                    case "subject" -> info.setSubject(null);
                    case "keywords" -> info.setKeywords(null);
                    case "creator" -> info.setCreator(null);
                    case "producer" -> info.setProducer(null);
                    case "creation date" -> info.setCreationDate(null);
                    case "modification date" -> info.setModificationDate(null);
                    case "trapped" -> info.setTrapped(null);
                    default -> info.setCustomMetadataValue(key, null);
                }
                document.save(target);
                return true;
            }
        } catch (Exception e) {
            ErrorLogger.error("Failed to delete PDF tag: " + e.getMessage());
        }
        return false;
    }

    public static File removeAllMetadata(File inputFile, File outputDirectory) throws Exception {
        if (inputFile == null || !inputFile.exists()) {
            throw new IllegalArgumentException("Input file does not exist");
        }

        File targetDir = (outputDirectory != null && outputDirectory.isDirectory())
                ? outputDirectory
                : inputFile.getParentFile();

        String ext = DetermineType.getExtensionByString(inputFile.getName()).toLowerCase(Locale.ROOT);
        String baseName = getBaseName(inputFile.getName());
        File outputFile = new File(targetDir, baseName + "_clean." + ext);

        if (isAudioFormat(ext)) {
            Files.copy(inputFile.toPath(), outputFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            AudioFile audioFile = AudioFileIO.read(outputFile);
            org.jaudiotagger.tag.Tag tag = audioFile.getTag();
            if (tag != null) {
                tag.deleteArtworkField();
                for (FieldKey fk : FieldKey.values()) {
                    try {
                        tag.deleteField(fk);
                    } catch (Exception _) {}
                }
                audioFile.commit();
            }
            return outputFile;
        } else if (isPdfFormat(ext)) {
            try (PDDocument document = Loader.loadPDF(inputFile)) {
                document.setDocumentInformation(new PDDocumentInformation());
                if (document.getDocumentCatalog() != null) {
                    document.getDocumentCatalog().setMetadata(null);
                }
                document.save(outputFile);
                return outputFile;
            }
        } else if (isImageFormat(ext)) {
            BufferedImage image = ImageIO.read(inputFile);
            if (image == null) {
                throw new IOException("Unable to decode image");
            }
            String formatName = ext.isEmpty() ? "png" : ext;
            if (formatName.equalsIgnoreCase("jpg") || formatName.equalsIgnoreCase("jpeg")) {
                BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                rgbImage.createGraphics().drawImage(image, 0, 0, java.awt.Color.WHITE, null);
                ImageIO.write(rgbImage, "jpg", outputFile);
            } else {
                ImageIO.write(image, formatName, outputFile);
            }
            return outputFile;
        } else if (isVideoFormat(ext)) {
            MultimediaObject mo = new MultimediaObject(inputFile);
            MultimediaInfo info = mo.getInfo();

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat(ext);

            if (info != null && info.getVideo() != null) {
                VideoAttributes va = new VideoAttributes();
                va.setCodec("copy");
                attrs.setVideoAttributes(va);
            }
            if (info != null && info.getAudio() != null) {
                AudioAttributes aa = new AudioAttributes();
                aa.setCodec("copy");
                attrs.setAudioAttributes(aa);
            }

            Encoder encoder = new Encoder();
            encoder.encode(mo, outputFile, attrs);
            return outputFile;
        }

        throw new UnsupportedOperationException("Unsupported format for metadata removal: " + ext);
    }

    public static String exportMetadataToText(File file, List<MetadataEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== METADATA REPORT ===\n");
        sb.append("File: ").append(file != null ? file.getName() : "Unknown").append("\n");
        sb.append("Path: ").append(file != null ? file.getAbsolutePath() : "").append("\n");
        sb.append("Generated on: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern(PATTERN_DATE)))
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
        if (val != null && !val.trim().isEmpty()) {
            list.add(new MetadataEntry(cat, key, val, canDelete, rawKey));
        }
    }

    private static boolean isStandardPdfKey(String key) {
        return List.of("Title", "Author", "Subject", "Keywords", "Creator", "Producer", "CreationDate", "ModDate", "Trapped").contains(key);
    }

    private static String formatTagKey(String raw) {
        if (raw == null) return "";
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
        if (fileName == null) return "output";
        int dot = fileName.lastIndexOf('.');
        return dot == -1 ? fileName : fileName.substring(0, dot);
    }
}
