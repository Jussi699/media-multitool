package model.preprocessing;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import model.logger.ErrorLogger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v23Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.jaudiotagger.tag.reference.ID3V2Version;
import visual.dialogs.Alerts;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import static java.util.Objects.nonNull;

public class AudioPreprocessing {
    private AudioPreprocessing() {
        /* This utility class should not be instantiated */
    }

    static {
        TagOptionSingleton.getInstance().setID3V2Version(ID3V2Version.ID3_V23);
    }

    public static void applyTags(File file, Map<FieldKey, String> tags, String pathToPhoto) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);

            if (audioFile instanceof MP3File mp3File) {
                if (mp3File.hasID3v2Tag()) {
                    AbstractID3v2Tag existingTag = mp3File.getID3v2Tag();
                    if (!(existingTag instanceof ID3v23Tag)) {
                        mp3File.setID3v2Tag(new ID3v23Tag(existingTag));
                    }
                } else {
                    mp3File.setID3v2Tag(new ID3v23Tag());
                }
            }

            Tag tag = audioFile.getTagOrCreateAndSetDefault();

            for (Map.Entry<FieldKey, String> entry : tags.entrySet()) {
                tag.deleteField(entry.getKey());
                if (nonNull(entry.getValue()) && !entry.getValue().isBlank()) {
                    tag.setField(entry.getKey(), entry.getValue());
                }
            }

            if (nonNull(pathToPhoto) && !pathToPhoto.isEmpty()) {
                Artwork artwork = ArtworkFactory.createArtworkFromFile(new File(pathToPhoto));
                tag.deleteArtworkField();
                tag.setField(artwork);
            }

            audioFile.commit();
        }
        catch (Exception e) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Something wrong!",
                    "Something wrong!",
                    "Check log file for more details for error!");
            ErrorLogger.error("193 | " + e.getMessage());
        }
    }

    public static Map<FieldKey, String> getTags(File file) {
        Map<FieldKey, String> tagMap = new HashMap<>();
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            if (nonNull(tag)) {
                for (FieldKey key : FieldKey.values()) {
                    try {
                        String value = tag.getFirst(key);
                        if (nonNull(value) && !value.isEmpty()) {
                            tagMap.put(key, value);
                        }
                    } catch (Exception _) {
                        // Ignore
                    }
                }
            }
        } catch (Exception e) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Error", "Something wrong!",
                    "Check log file for more details for error!");
            ErrorLogger.error("190 | " + e.getMessage());
        }
        return tagMap;
    }

    public static Optional<Image> getIconMp3(File file) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();

            if (nonNull(tag)) {
                Artwork artwork = tag.getFirstArtwork();
                if (nonNull(artwork)) {
                    byte[] imageData = artwork.getBinaryData();
                    return Optional.of(new Image(new ByteArrayInputStream(imageData)));
                }
            }
        } catch (Exception e) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Something wrong!", "Something wrong!",
                    "Check log file for more details for error!");
            ErrorLogger.error("191 | " + e.getMessage());
        }
        return Optional.empty();
    }
}
