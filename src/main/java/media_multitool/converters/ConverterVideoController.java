package media_multitool.converters;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import media_multitool.AbstractMediaController;
import model.converterVideo.ConverterVideoAudioFile;
import model.converterVideo.ConvertVideoAudioTask;
import model.enums.TypeMedia;
import model.helper.MediaHelper;
import model.logger.ErrorLogger;
import model.properties.MediaProperties;
import model.properties.VideoAndAudioProperties;
import model.select.SelectFile;
import model.utility.Global;
import model.utility.Item;
import model.utility.ResetContext;
import org.jspecify.annotations.NonNull;
import viewHelp.Alerts;
import viewHelp.ComboBoxes;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static viewHelp.Message.*;
import static model.utility.Parsers.*;
import static model.utility.PathWorker.*;
import static viewHelp.Utility.getMetadata;

public class ConverterVideoController extends AbstractMediaController {
    private static final ToggleGroup toggleGroup = new ToggleGroup();
    private final VideoAndAudioProperties videoProperties = new VideoAndAudioProperties();
    private ConvertVideoAudioTask currentTask;

    @FXML private Label labelSelectFile, textDragZone;
    @FXML private Button btnSubmitAndDownload, btnSelectFile, btnChoiceDirForSaveFile, btnCancelConversion;
    @FXML private ToggleButton btnToMP4, btnToAVI, btnToMKV, btnToWEBM, btnToMOV, btnToFLV, btnToWMV, btnTo3GP;
    @FXML private ComboBox<Item> comboBoxChoiceVideoBitRate, comboBoxChoiceAudioBitRate, comboBoxChoiceChannels, comboBoxChoiceSamplingRate, comboBoxChoiceFPS;
    @FXML private ComboBox<String> comboBoxChoiceResolution;
    @FXML private CheckBox checkBoxGPU;
    @FXML private StackPane dropZone;

    private List<Control> listControls;

    @Override
    protected MediaProperties getProperties() {
        return videoProperties;
    }

    @FXML
    public void initialize() {
        initLists();
        initComboBoxes();

        videoProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, videoProperties.getHideSuccessMessageTimer(), true);

        resetToDefaults();

        setupDragAndDrop(dropZone, Global.getAllSupportedVideoFormats(), this::loadFile);
        isPressedReset();
    }

    private void initLists() {
        List<ToggleButton> listToggleBtn = List.of(
                btnToMP4, btnToAVI, btnToMKV, btnToWEBM, btnToMOV, btnToFLV, btnToWMV, btnTo3GP
        );

        listToggleBtn.forEach(tb -> tb.setToggleGroup(toggleGroup));

        listControls = new ArrayList<>(listToggleBtn);
        listControls.addAll(List.of(comboBoxChoiceVideoBitRate, comboBoxChoiceAudioBitRate, comboBoxChoiceChannels, comboBoxChoiceSamplingRate,
                comboBoxChoiceFPS, comboBoxChoiceResolution, checkBoxGPU, btnSubmitAndDownload, btnCancelConversion, btnReset));
    }

    private void initComboBoxes() {
        ComboBoxes.setupComboBox(comboBoxChoiceVideoBitRate, Item::title);
        ComboBoxes.setupComboBox(comboBoxChoiceAudioBitRate, Item::title);
        ComboBoxes.setupComboBox(comboBoxChoiceChannels,     Item::title);
        ComboBoxes.setupComboBox(comboBoxChoiceSamplingRate, Item::title);
        ComboBoxes.setupComboBox(comboBoxChoiceFPS,          Item::title);

        comboBoxChoiceVideoBitRate.getItems().addAll(
                new Item(-1, "V: Match source"),
                new Item(1000, "V: 1000 kbps (SD)"), new Item(2500, "V: 2500 kbps (720p)"),
                new Item(5000, "V: 5000 kbps (1080p)"), new Item(8000, "V: 8000 kbps (High)")
        );

        comboBoxChoiceAudioBitRate.getItems().addAll(
                new Item(-1, "A: Match source"),
                new Item(96, "A: 96 kbps"), new Item(128, "A: 128 kbps"),
                new Item(192, "A: 192 kbps"), new Item(256, "A: 256 kbps"),
                new Item(320, "A: 320 kbps")
        );

        comboBoxChoiceChannels.getItems().addAll(
                new Item(-1, "Match source"),
                new Item(1, "1 Channels"), new Item(2, "2 Channels")
        );

        comboBoxChoiceSamplingRate.getItems().addAll(
                new Item(-1, "Match source"),
                new Item(8000, "8000 Hz"),
                new Item(11025, "11025 Hz"), new Item(12000, "12000 Hz"),
                new Item(16000, "16000 Hz"), new Item(22050, "22050 Hz"),
                new Item(24000, "24000 Hz"), new Item(32000, "32000 Hz"),
                new Item(44100, "44100 Hz"), new Item(48000, "48000 Hz")
        );

        comboBoxChoiceFPS.getItems().addAll(
                new Item(-1, "Match source"),
                new Item(24, "24 fps"), new Item(30, "30 fps"), new Item(60, "60 fps")
        );

        comboBoxChoiceResolution.getItems().addAll(
                "Match source",
                "1280x720", "1920x1080", "3840x2160"
        );
    }

    private void resetToDefaults() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, null,
                dropZone, null, progressBar, true, "video"
        );
        reset(videoProperties, ctx, "Selected video file: none");

        videoProperties.setVideoBitRate(5000);
        videoProperties.setAudioBitRate(192);
        videoProperties.setChannel(2);
        videoProperties.setSamplingRate(48000);
        videoProperties.setFps(30);
        videoProperties.setResolution("1920x1080");

        comboBoxChoiceVideoBitRate.setValue(new Item(5000, "V: 5000 kbps (1080p)"));
        comboBoxChoiceAudioBitRate.setValue(new Item(192, "A: 192 kbps"));
        comboBoxChoiceChannels.setValue(new Item(2, "2 Channels"));
        comboBoxChoiceSamplingRate.setValue(new Item(48000, "48000 Hz"));
        comboBoxChoiceFPS.setValue(new Item(30, "30 fps"));
        comboBoxChoiceResolution.setValue("1920x1080");
        checkBoxGPU.setSelected(false);

        progressBar.setProgress(0);

        toggleGroup.selectToggle(null);
    }

    @Override
    protected void lockUI() {
        disableControls();
        btnReset.setDisable(true);
        btnSelectFile.setDisable(true);
        btnChoiceDirForSaveFile.setDisable(true);
        btnCancelConversion.setDisable(false);
    }

    @Override
    protected void unlockUI() {
        enableControls();
        btnReset.setDisable(false);
        btnSelectFile.setDisable(false);
        btnChoiceDirForSaveFile.setDisable(false);
    }

    @Override
    protected void disableControls() {
        listControls.forEach(c -> c.setDisable(true));
    }

    @Override protected void enableControls() {
        listControls.forEach(c -> c.setDisable(false));
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);

        if (Boolean.TRUE.equals(result)) {
            showSuccessMessage(labelSuccess, videoProperties.getTargetFormat(), videoProperties.getHideSuccessMessageTimer());
            showProgressBar(progressBar, videoProperties.getHideSuccessMessageTimer());
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

    @FXML
    public void onSelectVideoPressed() {
        SelectFile selectFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Video", Global.getSupportedVideoFormatsForFileChooser()))
                .ifPresent(this::loadFile);
    }

    private void loadFile(File selectedFile) {
        enableControls();
        videoProperties.setSrcFile(selectedFile);
        labelSelectFile.setText("Selected file: " + videoProperties.getSrcFile().getName() + " (Loading info...)");
        
        CompletableFuture.supplyAsync(() -> getMetadata(videoProperties.getSrcFile()))
            .thenAccept(infoOpt -> Platform.runLater(() -> updateLabelFromMetadata(infoOpt.orElse(null))));
        
        hideSuccessMessage(labelSuccess, videoProperties.getHideSuccessMessageTimer(), true);

        textDragZone.setText("Selected: " + videoProperties.getSrcFile().getName());

        if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }
    }

    private void updateLabelFromMetadata(MultimediaInfo info) {
        if (isNull(info) || isNull(videoProperties.getSrcFile())) return;

        String res = parseResolution(info).orElse("N/A");
        int fps = parseFps(info);
        int vbr = parseVideoBitrate(info);
        int abr = parseAudioBitrate(info);

        String infoText = String.format("Selected file: %s [%s, %d fps, V:%d kbps, A:%d kbps]",
                videoProperties.getSrcFile().getName(),
                res, fps, vbr, abr);

        labelSelectFile.setText(infoText);
    }

    @FXML
    public void onSelectOutputDirectoryPressed() {
        selectOutputDirectory(btnChoiceDirForSaveFile, videoProperties.getOutput(), videoProperties::setOutput, "Select directory for save video");
    }

    @FXML
    private void onChoiceParameters(ActionEvent event) {
        if (!(event.getSource() instanceof ComboBox<?> comboBox)) {
            return;
        }

        Item selectedItem;
        switch (comboBox.getId()) {
            case "comboBoxChoiceVideoBitRate" -> {
                selectedItem = comboBoxChoiceVideoBitRate.getValue();
                videoProperties.setVideoBitRate((nonNull(selectedItem)) ? (int) selectedItem.id() : -1);
            }
            case "comboBoxChoiceAudioBitRate" -> {
                selectedItem = comboBoxChoiceAudioBitRate.getValue();
                videoProperties.setAudioBitRate((nonNull(selectedItem)) ? (int) selectedItem.id() : -1);
            }
            case "comboBoxChoiceChannels" -> {
                selectedItem = comboBoxChoiceChannels.getValue();
                videoProperties.setChannel((nonNull(selectedItem)) ? (int) selectedItem.id() : -1);
            }
            case "comboBoxChoiceSamplingRate" -> {
                selectedItem = comboBoxChoiceSamplingRate.getValue();
                videoProperties.setSamplingRate((nonNull(selectedItem)) ? (int) selectedItem.id() : -1);
            }
            case "comboBoxChoiceFPS" -> {
                selectedItem = comboBoxChoiceFPS.getValue();
                videoProperties.setFps((nonNull(selectedItem)) ? (int) selectedItem.id() : -1);
            }
        }
    }

    @FXML
    public void onChoiceResolution() {
        videoProperties.setResolution(comboBoxChoiceResolution.getValue());
    }

    @FXML
    private void onGPUSelected() {
        videoProperties.setUseGPU(checkBoxGPU.isSelected());
    }

    @FXML
    private void onActionClickToggleBtnFormat(ActionEvent e) {
        viewHelp.Message.hideSuccessMessage(labelSuccess, getProperties().getHideSuccessMessageTimer(), true);

        MediaHelper.selectFormatVideo(e).ifPresentOrElse(
                format -> selectFormat(format, videoProperties::setTargetFormat),
                () -> videoProperties.setTargetFormat(null)
        );
    }

    private boolean checks() {
        if(isNull(videoProperties.getSrcFile())) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Missing selection", "Select video file!");
            return false;
        }

        if(isNull(videoProperties.getOutput())) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Missing selection", "Select output directory!");
            return false;
        }

        if(isNull(videoProperties.getTargetFormat())) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Missing selection", "Select target format!");
            return false;
        }

        return true;
    }

    @FXML
    public void onStartConversionPressed() {
       if(!checks()) {
           return;
       }

        CompletableFuture.supplyAsync(() -> getMetadata(videoProperties.getSrcFile()), IO_EXECUTOR)
            .thenAccept(sourceInfoOpt -> {
                MultimediaInfo sourceInfo = sourceInfoOpt.orElse(null);
                if (nonNull(sourceInfo) && isNull(sourceInfo.getAudio())) {
                    Platform.runLater(() -> {
                        boolean proceed = Alerts.confirmationDialog(
                                "No Audio Track Detected",
                                "The selected file does not appear to have an audio track.",
                                "Do you want to proceed anyway? (The output will be silent)"
                        );
                        if (proceed) continueWithConversion(sourceInfo);
                    });
                } else {
                    int originalChannels = parseChannels(sourceInfo);
                    if (originalChannels == 1 && videoProperties.getChannel() == 2) {
                        Platform.runLater(() -> {
                            boolean proceed = Alerts.confirmationDialog(
                                    "Mono to Stereo Confirmation",
                                    "The source file is mono (1 channel).",
                                    "Do you want to convert it to stereo (2 channels) anyway?"
                            );
                            if (proceed) continueWithConversion(sourceInfo);
                        });
                    } else {
                        Platform.runLater(() -> continueWithConversion(sourceInfo));
                    }
                }
            });
    }

    private void continueWithConversion(MultimediaInfo sourceInfo) {
        int videoBitrate = videoProperties.getVideoBitRate();
        int audioBitrate = videoProperties.getAudioBitRate();
        int channel = videoProperties.getChannel();
        int samplingRate = videoProperties.getSamplingRate();
        int fps = videoProperties.getFps();

        int finalVideoBitrate = (videoBitrate == -1) ? parseVideoBitrate(sourceInfo) : videoBitrate;
        int finalAudioBitrate = (audioBitrate == -1) ? parseAudioBitrate(sourceInfo) : audioBitrate;
        int finalChannels = (channel == -1) ? parseChannels(sourceInfo) : channel;
        int finalSamplingRate = (samplingRate == -1) ? parseSamplingRate(sourceInfo) : samplingRate;
        int finalFps = (fps == -1) ? parseFps(sourceInfo) : fps;

        if (finalVideoBitrate <= 0) finalVideoBitrate = parseBitrate(sourceInfo);
        if (finalVideoBitrate <= 0) finalVideoBitrate = 5000;
        if (finalAudioBitrate <= 0) finalAudioBitrate = 192;
        if (finalSamplingRate <= 0) finalSamplingRate = 48000;
        if (finalChannels <= 0)     finalChannels = 2;
        if (finalFps <= 0)          finalFps = 30;

        ErrorLogger.info("Video conversion parameters: V-BR=" + finalVideoBitrate
                + ", A-BR=" + finalAudioBitrate + ", CH=" + finalChannels 
                + ", SR=" + finalSamplingRate + ", FPS=" + finalFps);

        if ("webm".equalsIgnoreCase(videoProperties.getTargetFormat()) &&
                finalSamplingRate == 11025 || finalSamplingRate == 22050 ||
                finalSamplingRate == 32000 || finalSamplingRate == 44100) {
                Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Sampling rate not supported",
                        "The sampling rate (" + finalSamplingRate + " Hz) is not supported for WEBM video format. Please choose another rate or format.");
                return;
        }

        String finalResolution = ("Match source".equalsIgnoreCase(videoProperties.getResolution())) ? parseResolution(sourceInfo).orElse(null) : videoProperties.getResolution();

        videoProperties.setUseGPU(nonNull(checkBoxGPU) && checkBoxGPU.isSelected());
        boolean useGPU = videoProperties.isUseGPU();

        String format = videoProperties.getTargetFormat();
        videoProperties.setVideoBitRate(finalVideoBitrate);

        videoProperties.setAudioBitRate(finalAudioBitrate);
        videoProperties.setChannel(finalChannels);
        videoProperties.setSamplingRate(finalSamplingRate);
        videoProperties.setFps(finalFps);
        videoProperties.setResolution(finalResolution);
        videoProperties.setVideoCodec(MediaHelper.getVideoCodec(format, useGPU));
        videoProperties.setAudioCodec(MediaHelper.getAudioCodec(format, true));
        videoProperties.setFfmpegFormat(MediaHelper.getFFmpegFormat(format));
        videoProperties.setTypeMedia(TypeMedia.VIDEO);

        ConverterVideoAudioFile converter = new ConverterVideoAudioFile();
        currentTask = new ConvertVideoAudioTask(converter, videoProperties, TypeMedia.VIDEO);
        
        executeMediaTask(currentTask);
    }

    @FXML
    public void isPressedReset() {
        onCancelConversion();
        resetToDefaults();
        hideSuccessMessage(labelSuccess, videoProperties.getHideSuccessMessageTimer(), true);
        disableControls();
    }

    @FXML
    public void onCancelConversion() {
        if (nonNull(currentTask)) currentTask.cancelConversion();
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Converter Video",
                """
                        How to use:
                        1. Select a video or audio file using "Select video" or drag and drop it into the dash-bordered zone;

                        2. (Optional) Select where you want to save the result by clicking on "Directory for save".
                            (Default directory: Desktop);

                        3. Select the target video format (MP4, AVI, MKV, etc.);

                        4. Configure video and audio settings (Bitrate, FPS, Resolution, etc.);

                        5. (Optional) Enable GPU Acceleration if your hardware supports it;

                        6. Click "Convert and Download".

                        You can cancel the conversion at any time using the "Cancel Conversion" button.

                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }
}
