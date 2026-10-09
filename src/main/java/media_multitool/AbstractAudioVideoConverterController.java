package media_multitool;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.NonNull;
import model.converterVideo.ConvertVideoAudioTask;
import model.enums.MediaType;
import model.logger.ErrorLogger;
import model.properties.MediaProperties;
import model.properties.VideoAndAudioProperties;
import model.select.SelectFile;
import viewHelp.Alerts;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.Parsers.parseChannels;
import static model.utility.PathWorker.*;
import static viewHelp.Message.hideSuccessMessage;
import static viewHelp.Message.showSuccessMessage;
import static viewHelp.Utility.getMetadata;

public abstract class AbstractAudioVideoConverterController extends AbstractMediaController {
    protected final VideoAndAudioProperties properties = new VideoAndAudioProperties();
    protected final ToggleGroup toggleGroup = new ToggleGroup();
    protected ConvertVideoAudioTask currentTask;

    @FXML protected StackPane dropZone;
    @FXML protected Label labelSelectFile, textDragZone;
    @FXML protected Button btnSubmitAndDownload, btnCancelConversion;

    @Override
    protected MediaProperties getProperties() {
        return properties;
    }

    protected abstract List<String> getSupportedInputFormats();
    protected abstract List<String> getSupportedFileChooserFormats();
    protected abstract String getFileFilterDescription();
    protected abstract MediaType getFileCategoryName();
    protected abstract void executeConversion(MultimediaInfo info);
    protected abstract void toggleUI(boolean flag);
    protected abstract void toggleControls(boolean flag);

    protected void onMetadataLoaded(MultimediaInfo info) {
        // Optional hook for subclasses to update UI based on metadata
    }

    @FXML
    public void initialize() {
        properties.setOutput(getSavedPath());
    }

    @FXML
    public void selectFile(ActionEvent event) {
        Node sourceNode = (Node) event.getSource();
        Stage stage = (Stage) sourceNode.getScene().getWindow();

        SelectFile selectFile = new SelectFile();
        selectFile.choiceFile(
                stage,
                new FileChooser.ExtensionFilter(getFileFilterDescription(), getSupportedFileChooserFormats())
        ).ifPresent(this::loadFile);
    }

    @FXML
    public void onSelectAudioVideoPressed(ActionEvent event) {
        selectFile(event);
    }

    @FXML
    public void onSelectVideoPressed(ActionEvent event) {
        selectFile(event);
    }

    @FXML
    public void chooseSaveDirectory(ActionEvent event) {
        Node sourceNode = (Node) event.getSource();
        Stage stage = (Stage) sourceNode.getScene().getWindow();
        directoryChooser(stage, properties.getOutput(), "Select directory for save " + getFileCategoryName().name().toLowerCase())
                .ifPresent(selectedPath -> {
                    properties.setOutput(selectedPath);
                    ErrorLogger.info(getClass(), "Output directory selected: " + selectedPath.getAbsolutePath());
                    hideSuccessMessage(labelSuccess, properties.getHideSuccessMessageTimer(), true);
                });
    }

    protected void loadFile(File selectedFile) {
        if (!validateSelectedFile(selectedFile)) {
            return;
        }

        enableControls();
        properties.setSrcFile(selectedFile);
        textDragZone.setText("Selected: " + selectedFile.getName());

        if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        labelSelectFile.setText("Selected " + getFileCategoryName().name().toLowerCase() + " file: " + selectedFile.getName());
        hideSuccessMessage(labelSuccess, properties.getHideSuccessMessageTimer(), true);

        CompletableFuture.supplyAsync(() -> getMetadata(properties.getSrcFile()), IO_EXECUTOR)
                .thenAccept(infoOpt -> Platform.runLater(() -> onMetadataLoaded(infoOpt.orElse(null))));
    }

    @FXML
    public void onCancelConversion() {
        if (nonNull(currentTask)) {
            currentTask.cancelConversion();
        }
        cancelCurrentTask(currentTask);
    }

    protected boolean checkCommonParameters() {
        if (isNull(properties.getSrcFile())) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "File missing!",
                    "Select " + getFileCategoryName().name().toLowerCase() + " file!");
            return false;
        }
        if (isNull(properties.getOutput())) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Output missing!", "Select output directory!");
            return false;
        }
        if (isNull(properties.getTargetFormat())) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Format missing!", "Select target format!");
            return false;
        }
        return true;
    }

    @FXML
    public void onStartConversionPressed() {
        if (!checkCommonParameters()) {
            return;
        }

        CompletableFuture.supplyAsync(() -> getMetadata(properties.getSrcFile()), IO_EXECUTOR)
                .thenAccept(sourceInfoOpt -> {
                    MultimediaInfo sourceInfo = sourceInfoOpt.orElse(null);

                    int originalChannels = parseChannels(sourceInfo);
                    if (originalChannels == 1 && properties.getChannel() == 2) {
                        Platform.runLater(() -> {
                            boolean proceed = Alerts.confirmationDialog(
                                    "Mono to Stereo Confirmation",
                                    "The source file is mono (1 channel).",
                                    "Do you want to convert it to stereo (2 channels) anyway?"
                            );
                            if (proceed) executeConversion(sourceInfo);
                        });
                    } else {
                        Platform.runLater(() -> executeConversion(sourceInfo));
                    }
                });
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.TRUE.equals(result)) {
            showSuccessMessage(labelSuccess, properties.getTargetFormat(), properties.getHideSuccessMessageTimer());
            showProgressBar(progressBar, properties.getHideSuccessMessageTimer());
        }
    }

    @Override
    protected void handleTaskFailure(@NonNull Throwable exception) {
        String msg = exception.getMessage();
        Throwable cause = exception.getCause();
        String causeMsg = nonNull(cause) ? cause.getMessage() : "";

        boolean isCancelled = (nonNull(msg) && (msg.contains("Encoding interrupted") || msg.contains("Stream Closed")))
                || (nonNull(causeMsg) && causeMsg.contains("Stream Closed"))
                || (nonNull(currentTask) && currentTask.isCancelled());

        if (isCancelled) {
            handleTaskCancelled();
            return;
        }
        super.handleTaskFailure(exception);
    }
}
