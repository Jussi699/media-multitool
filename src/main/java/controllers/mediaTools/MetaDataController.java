package controllers.mediaTools;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import controllers.abstractControllers.AbstractMediaController;
import model.helper.TableViewHelper;
import model.logger.ErrorLogger;
import model.metadata.MetadataEntry;
import model.metadata.MetadataHelper;
import model.properties.MediaProperties;
import model.properties.MetaDataProperties;
import model.utility.format.SupportedFormat;
import model.utility.io.PathWorker;
import model.utility.context.ResetContext;
import org.jspecify.annotations.NonNull;
import visual.dialogs.Alerts;
import visual.dialogs.InfoAlert;

import java.io.File;
import java.util.List;
import java.util.Locale;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static visual.util.Clipboards.copyToClipboard;
import static model.utility.io.PathWorker.getSavedPath;
import static visual.controls.Cells.setCellFactoryTableColumn;
import static visual.controls.Message.setupClearMessageTimer;
import static visual.controls.Message.showErrorMessage;
import static visual.controls.Message.showSuccessText;

public class MetaDataController extends AbstractMediaController {
    private record MetadataLoadResult(File file, List<MetadataEntry> entries) {}

    private final ObservableList<MetadataEntry> masterMetadataList = FXCollections.observableArrayList();
    private final MetaDataProperties metaDataProperties = new MetaDataProperties();
    private final InvalidationListener updateScrollBarVisibility =
            _ -> updateExternalVerticalScrollBarVisibility();
    private final InvalidationListener alignColumnsOnScrollBarVisibilityChange =
            _ -> alignTableColumns();

    @FXML private Button btnSelectFile, btnChoiceDirForSaveFile, btnCopyAll;
    @FXML private Button btnDeleteTag, btnRemoveAllMetadata;
    @FXML private StackPane dropZone;
    @FXML private ScrollBar metadataVerticalScrollBar;
    @FXML private TextField txtSearch;
    @FXML private Label textDragZone, labelSelectFile;
    @FXML private TableView<MetadataEntry> tableViewMetadata;
    @FXML private TableColumn<MetadataEntry, String> colCategory, colProperty, colValue;
    @FXML private TableColumn<MetadataEntry, Boolean> colCanDelete;

    private FilteredList<MetadataEntry> filteredMetadataList;
    private ScrollBar internalMetadataScrollBar;
    private boolean metadataLoadInProgress;

    @Override
    protected MediaProperties getProperties() {
        return metaDataProperties;
    }

    @FXML
    private void initialize() {
        metaDataProperties.setOutput(getSavedPath());

        setupTableColumns();
        setupContextMenu();
        setupSearchFilter();

        setupClearMessageTimer(labelSuccess, progressBar, metaDataProperties.getHideSuccessMessageTimer(), true);

        isPressedReset();
        setupDragAndDrop(dropZone, SupportedFormat.getAllSupportedMetadataFormats(), this::loadFile);

        Platform.runLater(this::setupExternalVerticalScrollBar);
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "Media Metadata",
                "1. Select a photo, PDF, video or audio file using \"Select file\" or drag and drop it into the dash-bordered zone;",
                """
                        3. View technical and embedded metadata in the table, or use the search field to filter properties;

                        4. (Optional) Double-click or right-click any row to copy values or delete specific tags;

                        5. (Optional) Click "Delete Selected Tag" to delete the selected metadata tag;

                        6. (Optional) Click "Copy All Metadata" to export all metadata to the clipboard;

                        7. (Optional) Click "Remove All Metadata" to strip all metadata and save a clean copy of the file.
                        """
        );
    }

    @FXML
    private void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, null,
                dropZone, null, progressBar, true, "photo, PDF, video or audio file"
        );
        reset(metaDataProperties, ctx, "Selected file: none");

        masterMetadataList.clear();
        txtSearch.clear();
        if (nonNull(metadataVerticalScrollBar)) {
            metadataVerticalScrollBar.setVisible(false);
            metadataVerticalScrollBar.setManaged(false);
        }
        disableControls();
        alignTableColumns();
    }

    @FXML
    private void selectFile() {
        selectInputFile(btnSelectFile,
                new FileChooser.ExtensionFilter("Media & Documents (*.jpg, *.mp4, *.mp3, *.pdf, ...)",
                        SupportedFormat.getSupportedMetadataFormatsForFileChooser()), this::loadFile);
    }

    @FXML
    private void chooseSaveDirectory() {
        selectOutputDirectory(btnChoiceDirForSaveFile, metaDataProperties.getOutput(),
                metaDataProperties::setOutput, "Select directory for saving cleaned files");
    }

    @FXML
    private void copyAllMetadata() {
        if (isNull(metaDataProperties.getCurrentFile()) || masterMetadataList.isEmpty()) {
            return;
        }

        String report = MetadataHelper.exportMetadataToText(metaDataProperties.getCurrentFile(), masterMetadataList);
        copyToClipboard(report);
        showSuccessMessage("All metadata copied to clipboard!");
    }

    @FXML
    private void deleteSelectedTag() {
        MetadataEntry selected = tableViewMetadata.getSelectionModel().getSelectedItem();
        File currentFile = metaDataProperties.getCurrentFile();

        if (isNull(selected) || isNull(currentFile)) {
            Alerts.alertDialog(Alert.AlertType.INFORMATION, "Selection", "No tag selected",
                    "Please select a metadata tag from the table to delete.");
            return;
        }

        if (!selected.isCanDelete()) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Not Editable", "Cannot delete property",
                    """
                    The selected property ('%s') is a technical file property and cannot be deleted individually.
                    Use 'Remove All Metadata' to strip file metadata."
                    """.formatted(selected.getKey()));
            return;
        }

        boolean success = MetadataHelper.deleteSingleTag(currentFile, selected, metaDataProperties.getOutput());
        if (success) {
            showSuccessMessage("Tag '" + selected.getKey() + "' deleted successfully!");
            reloadCurrentFileMetadata();
        } else {
            showErrorMessage(labelSuccess, "Failed to delete tag '%s'.".formatted(selected.getKey()), metaDataProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        }
    }

    @FXML
    private void removeAllMetadata() {
        File currentFile = metaDataProperties.getCurrentFile();
        if (isNull(currentFile)) {
            return;
        }

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);
                File cleanFile = MetadataHelper.removeAllMetadata(currentFile, metaDataProperties.getOutput());
                updateProgress(100, 100);
                return cleanFile;
            }
        };

        executeMediaTask(task);
        labelSuccess.setManaged(true);
    }

    @Override
    protected void lockUI() {
        toggleUI(true);
    }

    @Override
    protected void unlockUI() {
        toggleUI(false);
    }

    @Override
    protected void disableControls() {
        toggleControls(true);
    }

    @Override
    protected void enableControls() {
        toggleControls(false);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        if (result instanceof MetadataLoadResult loaded) {
            metadataLoadInProgress = false;
            hideProgressBar();
            if (loaded.file().equals(metaDataProperties.getCurrentFile())) {
                masterMetadataList.setAll(loaded.entries());
                txtSearch.clear();
                alignTableColumns();
            }
            return;
        }
        super.handleTaskSuccess(result);
        if (Boolean.FALSE.equals(result) || isNull(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info(getClass(), "Metadata stripped successfully! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Clean file saved to: " + outputFile.getName(), metaDataProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @Override
    protected void handleTaskFailure(@NonNull Throwable exception) {
        super.handleTaskFailure(exception);
        if (metadataLoadInProgress) {
            metadataLoadInProgress = false;
            hideProgressBar();
        }
        Platform.runLater(() -> {
            showErrorMessage(labelSuccess, "Error: " + exception.getMessage(), metaDataProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @Override
    protected void handleTaskCancelled() {
        super.handleTaskCancelled();
        if (metadataLoadInProgress) {
            metadataLoadInProgress = false;
            hideProgressBar();
        }
    }

    private void loadFile(File selectedFile) {
        if (!validateSelectedFile(selectedFile)) {
            return;
        }

        enableControls();
        metaDataProperties.setCurrentFile(selectedFile);
        PathWorker.saveInputPath(selectedFile);

        labelSelectFile.setText("Selected: " + selectedFile.getName());

        markDropZoneLoaded(dropZone, textDragZone, selectedFile.getName());
        reloadCurrentFileMetadata();
    }

    private void bindScrollBars(ScrollBar currentInternalBar) {
        if (internalMetadataScrollBar == currentInternalBar) {
            return;
        }

        if (nonNull(internalMetadataScrollBar)) {
            internalMetadataScrollBar.maxProperty().removeListener(updateScrollBarVisibility);
            internalMetadataScrollBar.visibleAmountProperty().removeListener(updateScrollBarVisibility);
            internalMetadataScrollBar.visibleProperty().removeListener(alignColumnsOnScrollBarVisibilityChange);
            metadataVerticalScrollBar.minProperty().unbind();
            metadataVerticalScrollBar.maxProperty().unbind();
            metadataVerticalScrollBar.visibleAmountProperty().unbind();
            metadataVerticalScrollBar.valueProperty()
                    .unbindBidirectional(internalMetadataScrollBar.valueProperty());
        }

        internalMetadataScrollBar = currentInternalBar;
        metadataVerticalScrollBar.minProperty().bind(currentInternalBar.minProperty());
        metadataVerticalScrollBar.maxProperty().bind(currentInternalBar.maxProperty());
        metadataVerticalScrollBar.visibleAmountProperty().bind(currentInternalBar.visibleAmountProperty());
        metadataVerticalScrollBar.valueProperty().bindBidirectional(currentInternalBar.valueProperty());

        currentInternalBar.maxProperty().addListener(updateScrollBarVisibility);
        currentInternalBar.visibleAmountProperty().addListener(updateScrollBarVisibility);
        currentInternalBar.visibleProperty().addListener(alignColumnsOnScrollBarVisibilityChange);
    }

    private void bindExternalScrollBarHeight() {
        if (metadataVerticalScrollBar.minHeightProperty().isBound()) {
            return;
        }

        metadataVerticalScrollBar.minHeightProperty().bind(tableViewMetadata.heightProperty());
        metadataVerticalScrollBar.prefHeightProperty().bind(tableViewMetadata.heightProperty());
        metadataVerticalScrollBar.maxHeightProperty().bind(tableViewMetadata.heightProperty());
    }

    private void setupTableColumns() {
        tableViewMetadata.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableViewMetadata.setFixedCellSize(28.0);

        colCategory.setCellValueFactory(data -> data.getValue().categoryProperty());
        colProperty.setCellValueFactory(data -> data.getValue().keyProperty());
        colValue.setCellValueFactory(data -> data.getValue().valueProperty());
        colCanDelete.setCellValueFactory(data -> data.getValue().canDeleteProperty());

        setCellFactoryTableColumn(colCanDelete);

        filteredMetadataList = new FilteredList<>(masterMetadataList, _ -> true);
        tableViewMetadata.setItems(filteredMetadataList);

        tableViewMetadata.setRowFactory(_ -> {
            TableRow<MetadataEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    MetadataEntry rowData = row.getItem();
                    copyToClipboard(rowData.getValue());
                    showSuccessMessage("Copied to clipboard: " + rowData.getValue());
                }
            });
            return row;
        });

        tableViewMetadata.skinProperty().addListener((_, _, newSkin) -> {
            if (nonNull(newSkin)) {
                Platform.runLater(this::setupExternalVerticalScrollBar);
            }
        });
    }

    private void setupExternalVerticalScrollBar() {
        if (isNull(metadataVerticalScrollBar) || isNull(tableViewMetadata)) {
            return;
        }

        ScrollBar currentInternalBar = TableViewHelper.findInternalVerticalScrollBar(tableViewMetadata);
        if (isNull(currentInternalBar)) {
            return;
        }

        hideInternalVerticalScrollBar(currentInternalBar);
        bindScrollBars(currentInternalBar);
        bindExternalScrollBarHeight();
        updateExternalVerticalScrollBarVisibility();
    }

    private void updateExternalVerticalScrollBarVisibility() {
        if (isNull(metadataVerticalScrollBar) || isNull(internalMetadataScrollBar)) {
            return;
        }

        if (masterMetadataList.isEmpty()) {
            metadataVerticalScrollBar.setVisible(false);
            metadataVerticalScrollBar.setManaged(false);
            return;
        }

        boolean hasVerticalOverflow = internalMetadataScrollBar.getMax() > internalMetadataScrollBar.getMin();

        metadataVerticalScrollBar.setVisible(hasVerticalOverflow);
        metadataVerticalScrollBar.setManaged(hasVerticalOverflow);
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem copyValueItem = new MenuItem("Copy Value");
        copyValueItem.setOnAction(_ -> {
            MetadataEntry selected = tableViewMetadata.getSelectionModel().getSelectedItem();
            if (nonNull(selected)) {
                copyToClipboard(selected.getValue());
                showSuccessMessage("Value copied to clipboard!");
            }
        });

        MenuItem copyKeyValueItem = new MenuItem("Copy Tag & Value");
        copyKeyValueItem.setOnAction(_ -> {
            MetadataEntry selected = tableViewMetadata.getSelectionModel().getSelectedItem();
            if (nonNull(selected)) {
                copyToClipboard(selected.getKey() + ": " + selected.getValue());
                showSuccessMessage("Tag & value copied to clipboard!");
            }
        });

        MenuItem deleteTagItem = new MenuItem("Delete This Tag");
        deleteTagItem.setOnAction(_ -> deleteSelectedTag());

        contextMenu.getItems().addAll(copyValueItem, copyKeyValueItem, new SeparatorMenuItem(), deleteTagItem);
        tableViewMetadata.setContextMenu(contextMenu);
    }

    private void setupSearchFilter() {
        txtSearch.textProperty().addListener((_, _, newVal) -> {
            if (isNull(newVal) || newVal.trim().isEmpty()) {
                filteredMetadataList.setPredicate(_ -> true);
            } else {
                String lower = newVal.trim().toLowerCase(Locale.ROOT);
                filteredMetadataList.setPredicate(entry ->
                        entry.getCategory().toLowerCase(Locale.ROOT).contains(lower) ||
                                entry.getKey().toLowerCase(Locale.ROOT).contains(lower) ||
                                entry.getValue().toLowerCase(Locale.ROOT).contains(lower)
                );
            }
            alignTableColumns();
        });
    }

    private void alignTableColumns() {
        Platform.runLater(() -> {
            if (isNull(tableViewMetadata)) return;

            setupExternalVerticalScrollBar();

            var policy = tableViewMetadata.getColumnResizePolicy();
            if (nonNull(policy)) {
                policy.call(new TableView.ResizeFeatures<>(tableViewMetadata, null, 0.0));
            }

            tableViewMetadata.requestLayout();
        });
    }

    private void hideInternalVerticalScrollBar(ScrollBar scrollBar) {
        scrollBar.setVisible(true);
        scrollBar.setManaged(true);
        scrollBar.setOpacity(0);
        scrollBar.setPrefWidth(0);
        scrollBar.setMinWidth(0);
        scrollBar.setMaxWidth(0);
    }

    private void reloadCurrentFileMetadata() {
        File current = metaDataProperties.getCurrentFile();
        if (isNull(current)) return;

        masterMetadataList.clear();
        txtSearch.clear();
        alignTableColumns();

        Task<MetadataLoadResult> task = new Task<>() {
            @Override
            protected MetadataLoadResult call() {
                return new MetadataLoadResult(current, MetadataHelper.extractMetadata(current));
            }
        };
        metadataLoadInProgress = true;
        executeMediaTask(task, "Load metadata");
    }

    private void hideProgressBar() {
        if (isNull(progressBar)) return;
        progressBar.setProgress(0);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
    }

    private void showSuccessMessage(String message) {
        Platform.runLater(() -> {
            showSuccessText(labelSuccess, message, metaDataProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    private void toggleUI(boolean disabled) {
        toggleControls(disabled);
        setControlsDisabled(disabled, btnSelectFile, btnChoiceDirForSaveFile);
        dropZone.setDisable(disabled);
    }

    private void toggleControls(boolean disabled) {
        setControlsDisabled(disabled, btnCopyAll, btnDeleteTag, btnRemoveAllMetadata, btnReset);
    }
}