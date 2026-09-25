package media_multitool.mediaTools;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.NonNull;
import media_multitool.AbstractMediaController;
import model.preprocessing.AudioPreprocessing;
import model.preprocessing.MediaTagPreprocessing;
import model.logger.ErrorLogger;
import model.properties.MediaProperties;
import model.properties.VideoAndAudioProperties;
import model.select.SelectFile;
import model.utility.*;
import org.jaudiotagger.tag.FieldKey;
import model.helper.TableViewHelper;
import viewHelp.Alerts;
import viewHelp.audioEditor.AudioEditor;
import viewHelp.audioEditor.SetupScrollPane;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.UnaryOperator;

import static model.utility.PathWorker.*;
import static viewHelp.Message.*;

public class MediaTagEditorController extends AbstractMediaController {
    private final VideoAndAudioProperties audioProperties = new VideoAndAudioProperties();
    private final ObservableList<DetailsAudioFile> masterFile = FXCollections.observableArrayList();
    private final FilteredList<DetailsAudioFile> filteredFile = new FilteredList<>(masterFile, _ -> true);

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

    @FXML private Button btnChangeIcon, btnSelectImage, btnChoiceDirForSave, btnSelectMultipleFile, btnSaveChanges;
    @FXML private ImageView imageViewPreview;
    @FXML private StackPane dropZone;
    @FXML private Label labelSelectImageName, textDragZone;
    @FXML private ComboBox<String> genreComboBox;

    @FXML private TextField titleField, artistField, albumField, albumArtistField, composerField, trackField,
            discNumberField, commentField, yearField, descriptionField, keywordsField, copyrightField,
            ratingField, textFieldFindFile;

    private File chosenDir;
    private List<TextField> textFields;
    private List<Button> listBtn;
    private List<Control> listControls;
    private final ScrollBar[] tableInternalVBarRef = new ScrollBar[1];

    @FXML
    public void initialize() {
        tableViewAudio.setItems(filteredFile);

        initLists();
        initTextFieldFind();
        initComboBoxes();

        genreComboBox.showingProperty().addListener((_, _, isShowing) -> {
            if (Boolean.TRUE.equals(isShowing)) {
                bindComboBoxPopupWidth(genreComboBox);
            }
        });

        audioProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, audioProperties.getHideSuccessMessageTimer(), true);

        onResetPressed();
        setupDragAndDrop(dropZone, Global.getAllSupportedMediaFormats(), this::loadFile);
    }

    private void initLists() {
        textFields = List.of(titleField, artistField, albumField, albumArtistField, composerField,
                trackField, discNumberField, commentField, yearField, descriptionField, keywordsField,
                copyrightField, ratingField, textFieldFindFile);


        listControls = new ArrayList<>();
        listControls.addAll(textFields);
        List<Control> tempListControl = List.of(genreComboBox, btnSaveChanges, btnChangeIcon, btnReset);

        listControls.addAll(tempListControl);

        List<TableColumn<DetailsAudioFile, ?>> allTableCol = List.of(colFileName, colPath, colTag, colTitle, colArtist, colAlbumArtist, colAlbum, colTrack,
                colDiscnumber, colYear, colGenre, colComment, colCodec, colBitrate, colFrequency, colModified, colLength);

        List<String> property = List.of(
                "fileName", "path", "tag", "title", "artist", "albumArtist", "album", "track",
                "discNumber", "year", "genre", "comment", "codec", "bitrate", "frequency", "modified", "length"
        );

        listBtn = List.of(btnChangeIcon, btnSelectImage, btnChoiceDirForSave, btnSelectMultipleFile, btnSaveChanges);

        initTableViewAndScrollPane(allTableCol, property);
    }

    private void initTextFieldFind() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*") && newText.length() <= 4) {
                return change;
            }
            return null;
        };

        TextFormatter<String> textFormatter = new TextFormatter<>(filter);
        yearField.setTextFormatter(textFormatter);

        Timeline searchDebounce = createSearchDebounce();

        textFieldFindFile.textProperty().addListener((_, _, _) -> {
            searchDebounce.stop();
            searchDebounce.playFromStart();
        });
    }

    private @NonNull Timeline createSearchDebounce() {
        Timeline searchDebounce = new Timeline(new KeyFrame(Duration.millis(300), _ -> {
            String query = textFieldFindFile.getText().toLowerCase();
            filteredFile.setPredicate(file -> {
                if (file == null) return true;
                if (query.isEmpty()) return true;
                return file.getFileName().toLowerCase().contains(query);
            });
            SetupScrollPane.fillPlaceholderRows(tableViewAudio, masterFile, tableScrollPane.getViewportBounds().getHeight());
        }));
        searchDebounce.setCycleCount(1);
        return searchDebounce;
    }

    private void initTableViewAndScrollPane(@NonNull List<TableColumn<DetailsAudioFile, ?>> allTableCol, List<String> property) {
        tableViewAudio.setTableMenuButtonVisible(false);
        tableViewAudio.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        allTableCol.forEach(column -> {
            if (column.getPrefWidth() > 0) {
                column.setMinWidth(column.getPrefWidth());
            }
        });

        SetupScrollPane.configureTableHorizontalScroll(
                tableScrollPane, tableViewAudio, masterFile, allTableCol, tableVerticalScrollBar, tableInternalVBarRef);

        TableViewHelper.setCellValueFactoryCol(allTableCol, property);

        tableViewAudio.getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> {
            if (newValue != null) {
                loadFile(new File(newValue.getPath()));
            }
        });
    }

    private void initComboBoxes() {
        genreComboBox.getItems().addAll(
                "Rock", "Pop", "Jazz", "Classical", "Hip Hop", "Electronic", "Metal", "Blues", "Country", "Reggae", "Other"
        );
        genreComboBox.setEditable(true);
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Media Tag Editor",
                """
                        How to use:
                        1. Select an audio, photo or video file using 'Select media file' or drag and drop.
                        2. (Optional) Choose a directory for saving the output.
                        3. Fill in the tag fields (Title, Artist, Description, Keywords, etc.).
                        4. (Optional) Change the cover image for audio files using 'Change Icon'.
                        5. Click 'Save Changes' to apply all changes.
                        
                        This tool allows you to edit audio, photo and video tags.
                        
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

    @Override
    protected void disableControls() {
        listControls.forEach(c -> c.setDisable(true));
    }

    @Override
    protected void enableControls() {
        listControls.forEach(c -> c.setDisable(false));
    }

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectAudioFile = new SelectFile();
        Stage stage = (Stage) btnSelectImage.getScene().getWindow();
        selectAudioFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Media Files", Global.getSupportedMediaFormatsForFileChooser())).ifPresent(this::loadFile);
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

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                updateProgress(10, 100);
                if (isAudioFile(audioProperties.getSrcFile())) {
                    Map<FieldKey, String> tags = collectTags();
                    String imagePath = audioProperties.getPathToImage() != null
                            ? audioProperties.getPathToImage().getPath()
                            : null;
                    AudioPreprocessing.applyTags(audioProperties.getSrcFile(), tags, imagePath);
                } else {
                    MediaTagPreprocessing.applyTags(audioProperties.getSrcFile(), collectMediaTags());
                }
                updateProgress(100, 100);
                return true;
            }
        };

        executeMediaTask(task);
        labelSuccess.setManaged(true);
    }

    private Map<String, String> collectMediaTags() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("title", titleField.getText());
        tags.put("artist", artistField.getText());
        tags.put("album", albumField.getText());
        tags.put("albumArtist", albumArtistField.getText());
        tags.put("composer", composerField.getText());
        tags.put("track", trackField.getText());
        tags.put("discNumber", discNumberField.getText());
        tags.put("year", yearField.getText());
        tags.put("genre", genreComboBox.getEditor().getText());
        tags.put("comment", commentField.getText());
        tags.put("description", descriptionField.getText());
        tags.put("keywords", keywordsField.getText());
        tags.put("copyright", copyrightField.getText());
        tags.put("rating", ratingField.getText());
        return tags;
    }

    private @NonNull Map<FieldKey, String> collectTags() {
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
                List<DetailsAudioFile> audioFiles = list.stream()
                        .filter(DetailsAudioFile.class::isInstance)
                        .map(DetailsAudioFile.class::cast)
                        .toList();
                masterFile.setAll(audioFiles);
                ErrorLogger.info("Loaded " + audioFiles.size() + " files to table.");
                enableControls();
            }
            return;
        }

        super.handleTaskSuccess(result);
        if (result instanceof Boolean && Boolean.FALSE.equals(result)) {
            return;
        }
        ErrorLogger.info("Media tags changed successfully!");

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Tags saved successfully!", audioProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            if (isAudioFile(audioProperties.getSrcFile())) {
                AudioEditor.updatePreview(audioProperties, imageViewPreview);
            }
            
            DetailsAudioFile selected = tableViewAudio.getSelectionModel().getSelectedItem();
            if (selected != null && isAudioFile(audioProperties.getSrcFile())) {
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
                dropZone, imageViewPreview, progressBar, true, "audio"
        );
        reset(audioProperties, ctx, "Selected audio file: none");
        AudioEditor.loadDefaultPreview(imageViewPreview);
        AudioEditor.clearFields(textFields, genreComboBox);
        masterFile.clear();
        if (!SetupScrollPane.hasRealTableData(masterFile)) {
            SetupScrollPane.fillPlaceholderRows(tableViewAudio, masterFile, tableScrollPane.getViewportBounds().getHeight());
        }
        disableControls();

        if(audioProperties.getSrcFile() != null) {
            ErrorLogger.error("For some reason, the file is not null when resetting!");
            AudioEditor.loadDefaultPreview(imageViewPreview);
            AudioEditor.clearFields(textFields, genreComboBox);
        }
    }

    private void loadFile(File selectedFile) {
        enableControls();
        audioProperties.setSrcFile(selectedFile);
        labelSelectImageName.setText("Selected audio: " + selectedFile.getName());

        try {
            if (isAudioFile(selectedFile)) {
                AudioPreprocessing.getIconMp3(audioProperties.getSrcFile())
                        .ifPresentOrElse(file -> AudioEditor.setPreview(file, imageViewPreview),
                                () -> AudioEditor.loadDefaultPreview(imageViewPreview));
                populateFields(AudioPreprocessing.getTags(selectedFile));
            } else {
                populateMediaFields(MediaTagPreprocessing.getTags(selectedFile));
                AudioEditor.loadDefaultPreview(imageViewPreview);
            }

        } catch (Exception e) {
            ErrorLogger.error("Failed to load tags: " + e.getMessage());
        }

        textDragZone.setText("Selected: " + selectedFile.getName());

        if (dropZone != null && !dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }
    }

    private void populateFields(@NonNull Map<FieldKey, String> tags) {
        titleField.setText(tags.getOrDefault(FieldKey.TITLE, ""));
        artistField.setText(tags.getOrDefault(FieldKey.ARTIST, ""));
        albumField.setText(tags.getOrDefault(FieldKey.ALBUM, ""));
        albumArtistField.setText(tags.getOrDefault(FieldKey.ALBUM_ARTIST, ""));
        composerField.setText(tags.getOrDefault(FieldKey.COMPOSER, ""));
        trackField.setText(tags.getOrDefault(FieldKey.TRACK, ""));
        discNumberField.setText(tags.getOrDefault(FieldKey.DISC_NO, ""));
        commentField.setText(tags.getOrDefault(FieldKey.COMMENT, ""));
        yearField.setText(tags.getOrDefault(FieldKey.YEAR, ""));
        descriptionField.clear();
        keywordsField.clear();
        copyrightField.clear();
        ratingField.clear();
        AudioEditor.setGenreValue(genreComboBox, tags.get(FieldKey.GENRE));
    }

    private void populateMediaFields(@NonNull Map<String, String> tags) {
        titleField.setText(tags.getOrDefault("title", ""));
        artistField.setText(tags.getOrDefault("artist", ""));
        albumField.setText(tags.getOrDefault("album", ""));
        albumArtistField.setText(tags.getOrDefault("albumArtist", ""));
        composerField.setText(tags.getOrDefault("composer", ""));
        trackField.setText(tags.getOrDefault("track", ""));
        discNumberField.setText(tags.getOrDefault("discNumber", ""));
        commentField.setText(tags.getOrDefault("comment", ""));
        yearField.setText(tags.getOrDefault("year", ""));
        descriptionField.setText(tags.getOrDefault("description", ""));
        keywordsField.setText(tags.getOrDefault("keywords", ""));
        copyrightField.setText(tags.getOrDefault("copyright", ""));
        ratingField.setText(tags.getOrDefault("rating", ""));
        AudioEditor.setGenreValue(genreComboBox, tags.get("genre"));
    }

    private boolean isAudioFile(File file) {
        return file != null && Global.getAllSupportedAudioFormats().stream()
                .anyMatch(format -> file.getName().toLowerCase(Locale.ROOT).endsWith(format));
    }

    public void onActionChangeIcon() {
        if (audioProperties.getSrcFile() == null) {
            Alerts.alertDialog(Alert.AlertType.INFORMATION, "Audio file not selected!", "Audio file not selected!", "First select audio file!");
            return;
        }

        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnChangeIcon.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Image Files", Global.getSupportedImageFormatsForFileChooser())).ifPresent(file -> {
            audioProperties.setPathToImage(file);
            AudioEditor.updatePreviewWithPath(audioProperties, imageViewPreview);
        });
    }

    public void onActionBtnSelectMultipleFile() {
        Stage stage = (Stage) btnChoiceDirForSave.getScene().getWindow();
        ChoiceDialog<String> mediaTypeDialog = new ChoiceDialog<>(
                "All media",
                "Photos",
                "Videos",
                "Audio",
                "All media"
        );
        mediaTypeDialog.initOwner(stage);
        mediaTypeDialog.setTitle("Batch File");
        mediaTypeDialog.setHeaderText("Choose files to search for");
        mediaTypeDialog.setContentText("Media type:");
        mediaTypeDialog.setGraphic(Alerts.createQuestionGraphic());
        mediaTypeDialog.setOnShown(_ -> {
            mediaTypeDialog.getDialogPane().setGraphic(Alerts.createQuestionGraphic());
            ComboBox<?> comboBox = (ComboBox<?>) mediaTypeDialog.getDialogPane().lookup(".combo-box");
            if (comboBox != null) {
                bindComboBoxPopupWidth(comboBox);
            }
        });

        mediaTypeDialog.showAndWait().ifPresent(mediaType ->
                directoryChooser(stage, audioProperties.getOutput(), "Select directory")
                .ifPresent(dir -> {
                    disableControls();
                    chosenDir = dir;
                    audioProperties.setOutput(dir);
                    List<String> formats = getFormatsForMediaType(mediaType);

                    Task<List<DetailsAudioFile>> task = new Task<>() {
                        @Override
                        protected List<DetailsAudioFile> call() {
                            return TableViewHelper.loadFilesFromDir(
                                    chosenDir.toPath(),
                                    formats,
                                    (processed, total) -> {
                                        updateProgress(processed, Math.max(total, 1));
                                        updateMessage("Read files: " + processed + " / " + total);
                                    },
                                    this::isCancelled
                            );
                        }
                    };

                    Alerts.showProgressDialog(stage, task, "Loading files", "Reading " + mediaType.toLowerCase(Locale.ROOT) + " from directory");
                    executeMediaTask(task);
                }));
    }

    private List<String> getFormatsForMediaType(String mediaType) {
        return switch (mediaType) {
            case "Photos" -> Global.getAllSupportedImageFormats();
            case "Videos" -> Global.getAllSupportedVideoFormats();
            case "Audio" -> Global.getAllSupportedAudioFormats();
            case "All media" -> Global.getAllSupportedMediaFormats();
            default -> throw new IllegalArgumentException("Unsupported media type: " + mediaType);
        };
    }

    private void bindComboBoxPopupWidth(ComboBox<?> comboBox) {
        Platform.runLater(() -> {
            if (comboBox.getSkin() == null) {
                return;
            }

            var popupListView = (ListView<?>) comboBox.getSkin().getNode().lookup(".list-view");
            if (popupListView != null) {
                popupListView.minWidthProperty().bind(comboBox.widthProperty());
                popupListView.prefWidthProperty().bind(comboBox.widthProperty());
                popupListView.maxWidthProperty().bind(comboBox.widthProperty());
            }
        });
    }
}
