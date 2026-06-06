package model.worker;

import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import model.utility.DetailsAudioFile;
import model.utility.Global;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class TableViewWorker {
    public static final double TABLE_ROW_HEIGHT = 28.0;
    public static final double TABLE_HEADER_HEIGHT = 28.0;

    public static List<DetailsAudioFile> loadFilesFromDir(Path inputDirectory) {
        return loadFilesFromDir(inputDirectory, null, () -> false);
    }

    public static List<DetailsAudioFile> loadFilesFromDir(
            Path inputDirectory,
            BiConsumer<Integer, Integer> onProgress,
            BooleanSupplier isCancelled) {
        List<String> formats = Global.getAllSupportedAudioFormats();
        List<DetailsAudioFile> detailsList = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(inputDirectory)) {
            List<Path> foundPaths = paths.filter(Files::isRegularFile)
                    .filter(path -> formats.stream().anyMatch(ext -> path.toString().toLowerCase().endsWith(ext.toLowerCase())))
                    .toList();

            reportProgress(onProgress, 0, foundPaths.size());

            for (int i = 0; i < foundPaths.size(); i++) {
                if (isCancelled != null && isCancelled.getAsBoolean()) {
                    break;
                }
                detailsList.add(createDetails(foundPaths.get(i)));
                reportProgress(onProgress, i + 1, foundPaths.size());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading files from directory", e);
        }
        return detailsList;
    }

    private static void reportProgress(BiConsumer<Integer, Integer> onProgress, int processed, int total) {
        if (onProgress != null) {
            onProgress.accept(processed, total);
        }
    }

    public static DetailsAudioFile createDetails(Path path) {
        DetailsAudioFile details = new DetailsAudioFile();
        File file = path.toFile();
        details.setFileName(file.getName());
        details.setPath(file.getAbsolutePath());

        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            if (tag != null) {
                details.setTag(tag.getClass().getSimpleName().replace("Tag", ""));
                details.setTitle(tag.getFirst(FieldKey.TITLE));
                details.setArtist(tag.getFirst(FieldKey.ARTIST));
                details.setAlbumArtist(tag.getFirst(FieldKey.ALBUM_ARTIST));
                details.setAlbum(tag.getFirst(FieldKey.ALBUM));
                details.setTrack(tag.getFirst(FieldKey.TRACK));
                details.setDiscNumber(tag.getFirst(FieldKey.DISC_NO));
                details.setYear(tag.getFirst(FieldKey.YEAR));
                details.setGenre(tag.getFirst(FieldKey.GENRE));
                details.setComment(tag.getFirst(FieldKey.COMMENT));
            }

            AudioHeader header = audioFile.getAudioHeader();
            if (header != null) {
                details.setCodec(header.getEncodingType());
                details.setBitrate(String.valueOf(header.getBitRate()));
                details.setFrequency(String.valueOf(header.getSampleRate()));
                int lengthSeconds = header.getTrackLength();
                details.setLength(LocalTime.ofSecondOfDay(lengthSeconds));
            }

            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
            details.setModified(attr.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

        } catch (Exception e) {
            // If some file fails to load, we still return basic info
        }
        return details;
    }

    public static double getTableHeaderHeight(TableView<?> tableView) {
        Node header = tableView.lookup(".column-header-background");
        if (header != null) {
            return header.getBoundsInLocal().getHeight();
        }
        return TABLE_HEADER_HEIGHT;
    }

    public static int getVisibleRowCount(TableView<?> tableView, double viewportHeight) {
        if (viewportHeight <= 0) {
            return 0;
        }
        return Math.max(0, (int) Math.floor(
                (viewportHeight - getTableHeaderHeight(tableView)) / tableView.getFixedCellSize()
        ));
    }

    public static void setCellValueFactoryCol(
            List<TableColumn<DetailsAudioFile, ?>> columns,
            List<String> properties) {

        for (int i = 0; i < columns.size() && i < properties.size(); i++) {
            columns.get(i).setCellValueFactory(
                    new PropertyValueFactory<>(properties.get(i))
            );
        }
    }
}
