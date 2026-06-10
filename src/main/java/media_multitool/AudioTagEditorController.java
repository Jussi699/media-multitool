package media_multitool;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.utility.DetailsAudioFile;
import model.preprocessing.AudioPreprocessing;
import model.logger.ErrorLogger;
import model.properties.MediaProperties;
import model.properties.VideoAndAudioProperties;
import model.select.SelectFile;
import model.utility.*;
import org.jaudiotagger.tag.FieldKey;
import model.worker.TableViewWorker;
import viewHelp.Alerts;
import viewHelp.audioEditor.AudioEditor;
import viewHelp.audioEditor.SetupScrollPane;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.UnaryOperator;

import static model.utility.Util.directoryChooser;
import static model.utility.Util.getSavedPath;
import static viewHelp.Message.*;

public class AudioTagEditorController extends AbstractMediaController {
    private final VideoAndAudioProperties audioProperties = new VideoAndAudioProperties();

    @Override
    protected MediaProperties getProperties() {
        return audioProperties;
    }

    @FXML private TableView<DetailsAudioFile> tableViewAudio;
    @FXML private ScrollPane tableScrollPane;
    @FXML private ScrollBar tableVerticalScrollBar;

    @FXML private TableColumn<DetailsAudioFile, String> colFileName, colPath, colTag, colTitle, colArtist, colAlbumArtist, colAlbum, colTrack,
        colDiscnumber, colYear, colGenre, colComment, colCodec, colBitrate, colFrequency;

    @FXML private TableColumn<DetailsAudioFile, LocalDate> colModified;
    @FXML private TableColumn<DetailsAudioFile, LocalTime> colLength;

    @FXML private Button btnChangeIcon,  btnSelectPhoto, btnChoiceDirForSave, btnSelectMultipleFile, btnSaveTag;
    @FXML private ImageView imageViewPreview;
    @FXML private StackPane dropZone;
    @FXML private Label labelSelectImageName, textDragZone;
    @FXML private ComboBox<String> genreComboBox;

    @FXML private TextField titleField, artistField, albumField, albumArtistField, composerField, trackField, discNumberField, commentField, yearField;

    private File chosenDir;
    private List<TextField> textFields;
    private List<Button> listBtn;
    private final ScrollBar[] tableInternalVBarRef = new ScrollBar[1];

    @FXML
    public void initialize() {
        textFields = List.of(titleField, artistField, albumField, albumArtistField, composerField, trackField, discNumberField, commentField, yearField);
        List<TableColumn<DetailsAudioFile, ?>> allTableCol = List.of(colFileName, colPath, colTag, colTitle, colArtist, colAlbumArtist, colAlbum, colTrack,
                colDiscnumber, colYear, colGenre, colComment, colCodec, colBitrate, colFrequency, colModified, colLength);

        List<String> property = List.of(
                "fileName", "path", "tag", "title", "artist", "albumArtist", "album", "track",
                "discNumber", "year", "genre", "comment", "codec", "bitrate", "frequency", "modified", "length"
        );

        listBtn = List.of(btnChangeIcon,  btnSelectPhoto, btnChoiceDirForSave, btnSelectMultipleFile, btnSaveTag);

        tableViewAudio.setTableMenuButtonVisible(false);
        tableViewAudio.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        allTableCol.forEach(column -> {
            if (column.getPrefWidth() > 0) {
                column.setMinWidth(column.getPrefWidth());
            }
        });

        SetupScrollPane.configureTableHorizontalScroll(
                tableScrollPane, tableViewAudio, allTableCol, tableVerticalScrollBar, tableInternalVBarRef);

        TableViewWorker.setCellValueFactoryCol(allTableCol, property);
        audioProperties.setOutput(getSavedPath());

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*") && newText.length() <= 4) {
                return change;
            }
            return null;
        };

        TextFormatter<String> textFormatter = new TextFormatter<>(filter);
        yearField.setTextFormatter(textFormatter);

        setupClearMessageTimer(labelSuccess, progressBar, audioProperties.getHideSuccessMessageTimer(), true);

        genreComboBox.getItems().addAll(
                "Rock", "Pop", "Jazz", "Classical", "Hip Hop", "Electronic", "Metal", "Blues", "Country", "Reggae", "Other"
        );
        genreComboBox.setEditable(true);

        tableViewAudio.getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> {
            if (newValue != null) {
                loadFile(new File(newValue.getPath()));
            }
        });

        onResetPressed();
        setupDragAndDrop(dropZone, textDragZone, Global.getAllSupportedAudioFormats(), this::loadFile);
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "MP3 Tag Editor",
                """
                        How to use:
                        1. Select an audio file using 'Select audio file' or drag and drop.
                        2. (Optional) Choose a directory for saving the output.
                        3. Fill in the metadata fields (Title, Artist, Album, etc.).
                        4. (Optional) Change the icon using 'Change Icon'.
                        5. Click 'Save Tags' to apply all changes.
                        
                        This tool allows you to edit MP3 tags and album art.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    @Override
    protected void lockUI() {
        listBtn.forEach(button -> button.setDisable(true));
    }

    @Override
    protected void unlockUI() {
        listBtn.forEach(button -> button.setDisable(false));
    }

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectAudioFile = new SelectFile();
        Stage stage = (Stage) btnSelectPhoto.getScene().getWindow();
        selectAudioFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Audio Files", Global.getSupportedAudioFormatsForFileChooser()),
                "Choice audio"
        ).ifPresent(this::loadFile);
    }

    @FXML
    public void btnChoiceDirForSaveImage() {
        selectOutputDirectory(btnChoiceDirForSave, audioProperties.getOutput(), audioProperties::setOutput, "Select directory for save audio");
    }

    @FXML
    public void submitAndDownload() {
        if (audioProperties.getSrcFile() == null) {
            ErrorLogger.error("Audio file not selected!");
            return;
        }

        Map<FieldKey, String> tags = collectTags();
        String imagePath = audioProperties.getPathToImage() != null ? audioProperties.getPathToImage().getPath() : null;

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                updateProgress(10, 100);
                AudioPreprocessing.applyTags(audioProperties.getSrcFile(), tags, imagePath);
                updateProgress(100, 100);
                return true;
            }
        };

        executeMediaTask(task);
        labelSuccess.setManaged(true);
    }

    private Map<FieldKey, String> collectTags() {
        Map<FieldKey, String> tags = new EnumMap<>(FieldKey.class);
        tags.put(FieldKey.TITLE, titleField.getText());
        tags.put(FieldKey.ARTIST, artistField.getText());
        tags.put(FieldKey.ALBUM, albumField.getText());
        tags.put(FieldKey.ALBUM_ARTIST, albumArtistField.getText());
        tags.put(FieldKey.COMPOSER, composerField.getText());
        tags.put(FieldKey.TRACK, trackField.getText());
        tags.put(FieldKey.DISC_NO, discNumberField.getText());
        tags.put(FieldKey.COMMENT, commentField.getText());
        tags.put(FieldKey.YEAR, yearField.getText());

        String genre = genreComboBox.getEditor().getText();
        if (genre != null && !genre.isEmpty()) {
            tags.put(FieldKey.GENRE, genre);
        }

        return tags;
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        if (result instanceof List<?> list) {
            if (!list.isEmpty() && list.getFirst() instanceof DetailsAudioFile) {
                tableViewAudio.getItems().setAll((Collection<? extends DetailsAudioFile>) list);
                ErrorLogger.info("Loaded " + list.size() + " files to table.");
            }
            return;
        }

        super.handleTaskSuccess(result);
        if (result instanceof Boolean && Boolean.FALSE.equals(result)) {
            return;
        }
        ErrorLogger.info("MP3 tags and icon changed successfully!");

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Tags saved successfully!", audioProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            AudioEditor.updatePreview(audioProperties, imageViewPreview);
            
            DetailsAudioFile selected = tableViewAudio.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Map<FieldKey, String> newTags = collectTags();
                selected.setTitle(newTags.get(FieldKey.TITLE));
                selected.setArtist(newTags.get(FieldKey.ARTIST));
                selected.setAlbum(newTags.get(FieldKey.ALBUM));
                selected.setAlbumArtist(newTags.get(FieldKey.ALBUM_ARTIST));
                selected.setTrack(newTags.get(FieldKey.TRACK));
                selected.setDiscNumber(newTags.get(FieldKey.DISC_NO));
                selected.setYear(newTags.get(FieldKey.YEAR));
                selected.setGenre(genreComboBox.getEditor().getText());
                selected.setComment(newTags.get(FieldKey.COMMENT));
                tableViewAudio.refresh();
            }
        });
    }

    @Override
    protected void handleTaskFailure(Throwable exception) {
        super.handleTaskFailure(exception);
        Platform.runLater(() -> {
            showErrorMessage(labelSuccess, "Error: " + exception.getMessage(), audioProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @FXML
    public void onResetPressed() {
        ResetContext ctx = new ResetContext(
                labelSelectImageName, labelSuccess, textDragZone, null,
                dropZone, imageViewPreview, progressBar, true
        );
        Util.reset(audioProperties, ctx, "Selected audio file: none");
        AudioEditor.loadDefaultPreview(imageViewPreview);
        AudioEditor.clearFields(textFields, genreComboBox);
        tableViewAudio.getItems().clear();
        SetupScrollPane.fillPlaceholderRows(tableViewAudio, tableScrollPane.getViewportBounds().getHeight());
    }

    private void loadFile(File selectedFile) {
        audioProperties.setSrcFile(selectedFile);
        labelSelectImageName.setText("Selected audio: " + selectedFile.getName());

        try {
            AudioPreprocessing.getIconMp3(audioProperties.getSrcFile()).ifPresentOrElse(file -> AudioEditor.setPreview(file, imageViewPreview),
                    () -> AudioEditor.loadDefaultPreview(imageViewPreview));

            populateFields(AudioPreprocessing.getTags(selectedFile));

        } catch (Exception e) {
            ErrorLogger.error("Failed to load metadata: " + e.getMessage());
        }

        textDragZone.setText("Selected: " + selectedFile.getName());

        if (dropZone != null && !dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }
    }

    private void populateFields(Map<FieldKey, String> tags) {
        titleField.setText(tags.getOrDefault(FieldKey.TITLE, ""));
        artistField.setText(tags.getOrDefault(FieldKey.ARTIST, ""));
        albumField.setText(tags.getOrDefault(FieldKey.ALBUM, ""));
        albumArtistField.setText(tags.getOrDefault(FieldKey.ALBUM_ARTIST, ""));
        composerField.setText(tags.getOrDefault(FieldKey.COMPOSER, ""));
        trackField.setText(tags.getOrDefault(FieldKey.TRACK, ""));
        discNumberField.setText(tags.getOrDefault(FieldKey.DISC_NO, ""));
        commentField.setText(tags.getOrDefault(FieldKey.COMMENT, ""));
        yearField.setText(tags.getOrDefault(FieldKey.YEAR, ""));
        AudioEditor.setGenreValue(genreComboBox, tags.get(FieldKey.GENRE));
    }

    public void onActionChangeIcon() {
        if (audioProperties.getSrcFile() == null) {
            Alerts.alertDialog(Alert.AlertType.INFORMATION, "Audio file not selected!", "Audio file not selected!", "First select audio file!");
            return;
        }

        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnChangeIcon.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Image Files", Global.getSupportedImageFormatsForFileChooser()),
                "Select image"
        ).ifPresent(file -> {
            audioProperties.setPathToImage(file);
            AudioEditor.updatePreviewWithPath(audioProperties, imageViewPreview);
        });
    }

    public void onActionBtnSelectMultipleFile() {
        Stage stage = (Stage) btnChoiceDirForSave.getScene().getWindow();
        directoryChooser(stage, audioProperties.getOutput(), "Select directory")
                .ifPresent(dir -> {
                    chosenDir = dir;
                    audioProperties.setOutput(dir);

                    Task<List<DetailsAudioFile>> task = new Task<>() {
                        @Override
                        protected List<DetailsAudioFile> call() {
                            return TableViewWorker.loadFilesFromDir(
                                    chosenDir.toPath(),
                                    (processed, total) -> {
                                        updateProgress(processed, Math.max(total, 1));
                                        updateMessage("Read files: " + processed + " / " + total);
                                    },
                                    this::isCancelled
                            );
                        }
                    };

                    Alerts.showProgressDialog(stage, task, "Loading files", "Reading audio files from directory");
                    executeMediaTask(task);
                });
    }
}
