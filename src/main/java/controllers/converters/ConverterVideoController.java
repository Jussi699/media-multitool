package controllers.converters;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import controllers.abstractControllers.AbstractAudioVideoConverterController;
import model.mediaTools.converterVideo.ConverterVideoAudioFile;
import model.mediaTools.converterVideo.ConvertVideoAudioTask;
import model.enums.MediaFormat;
import model.enums.MediaType;
import model.helper.media.MediaHelper;
import model.logger.ErrorLogger;
import model.utility.format.SupportedFormat;
import visual.controls.ComboBoxItem;
import model.utility.context.ResetContext;
import visual.dialogs.Alerts;
import visual.controls.ComboBoxes;
import visual.dialogs.InfoAlert;
import ws.schild.jave.info.MultimediaInfo;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.media.MediaParsers.*;
import static model.utility.io.PathWorker.getSavedPath;
import static visual.controls.ComboBoxes.createItems;
import static visual.controls.Message.hideSuccessMessage;
import static visual.controls.Message.setupClearMessageTimer;

public class ConverterVideoController extends AbstractAudioVideoConverterController {
    private static final Set<Integer> UNSUPPORTED_WEBM_RATES = Set.of(11025, 22050, 32000, 44100);

    @FXML private Button btnSelectFile, btnChoiceDirForSaveFile;
    @FXML private ToggleButton btnToMP4, btnToAVI, btnToMKV;
    @FXML private ToggleButton btnToWEBM, btnToMOV, btnToFLV;
    @FXML private ToggleButton btnToWMV, btnTo3GP, btnToGIF;
    @FXML private Label gifInfoLabel;
    @FXML private ComboBox<ComboBoxItem> videoBitRateComboBox, audioBitRateComboBox, channelsComboBox;
    @FXML private ComboBox<ComboBoxItem> samplingRateComboBox, fpsComboBox;
    @FXML private ComboBox<String> resolutionComboBox;
    @FXML private CheckBox gpuCheckBox;
    @FXML private VBox parametersContainer;

    @Override
    protected List<String> getSupportedInputFormats() {
        return Stream.concat(SupportedFormat.getAllSupportedVideoFormats().stream(), Stream.of(".gif")).toList();
    }

    @Override
    protected List<String> getSupportedFileChooserFormats() {
        return Stream.concat(SupportedFormat.getSupportedVideoFormatsForFileChooser().stream(), Stream.of("*.gif")).toList();
    }

    @Override
    protected String getFileFilterDescription() {
        return "Video or animated GIF";
    }

    @Override
    protected MediaType getFileCategoryName() {
        return MediaType.VIDEO;
    }

    @FXML
    public void initialize() {
        initToggleGroup();
        initComboBoxes();

        properties.setOutput(getSavedPath());
        setupClearMessageTimer(labelSuccess, progressBar, properties.getHideSuccessMessageTimer(), true);

        isPressedReset();

        setupDragAndDrop(dropZone, getSupportedInputFormats(), this::loadFile);
    }

    private void initToggleGroup() {
        configureToggleGroup(toggleGroup, btnToMP4, btnToAVI, btnToMKV, btnToWEBM,
                btnToMOV, btnToFLV, btnToWMV, btnTo3GP, btnToGIF);
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "Converter Video",
                "1. Select a video or animated GIF using \"Select video/GIF\" or drag and drop it into the dash-bordered zone",
                """
                        3. Select the target format (MP4, AVI, MKV, GIF, etc.);

                        4. Configure video settings (FPS, Resolution, etc.). GIF is silent and supports up to 256 colors;

                        5. (Optional) Enable GPU Acceleration if your hardware supports it;

                        6. Click "Convert and Download".

                        You can cancel the conversion at any time using the "Cancel Conversion" button.
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        onCancelConversion();

        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, null,
                dropZone, null, progressBar, true, "video"
        );
        reset(properties, ctx, "Selected video file: none");

        resetPropertiesToDefault();
        hideSuccessMessage(labelSuccess, properties.getHideSuccessMessageTimer(), true);
        disableControls();
        resetComboBox();

        gpuCheckBox.setSelected(false);

        progressBar.setProgress(0);
        clearToggleGroup(toggleGroup);
        updateGifControls(false);
    }

    @Override
    protected void onMetadataLoaded(MultimediaInfo info) {
        if (isNull(info) || isNull(properties.getSrcFile())) return;

        String res = parseResolution(info).orElse("N/A");
        int fps = parseFps(info);
        int vbr = parseVideoBitrate(info);
        int abr = parseAudioBitrate(info);

        String infoText = String.format("Selected file: %s [%s, %d fps, V:%d kbps, A:%d kbps]",
                properties.getSrcFile().getName(),
                res, fps, vbr, abr);

        labelSelectFile.setText(infoText);
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
        btnCancelConversion.setDisable(true);
    }

    @Override
    protected void enableControls() {
        toggleControls(false);
        btnCancelConversion.setDisable(true);
    }

    @Override
    protected void toggleUI(boolean flag) {
        toggleControls(flag);
        setControlsDisabled(flag, parametersContainer, btnSelectFile, btnChoiceDirForSaveFile);
        dropZone.setDisable(flag);
        btnCancelConversion.setDisable(!flag);
    }

    @Override
    protected void toggleControls(boolean flag) {
        setControlsDisabled(flag, parametersContainer, btnSubmitAndDownload, btnReset);
    }

    @Override
    protected void executeConversion(MultimediaInfo sourceInfo) {
        ConversionParams params = resolveConversionParams(sourceInfo);
        if (isNull(params)) {
            return;
        }

        applyConversionParams(params);

        ConverterVideoAudioFile converter = new ConverterVideoAudioFile();
        currentTask = new ConvertVideoAudioTask(converter, properties, MediaType.VIDEO);

        executeMediaTask(currentTask);
    }

    @FXML
    private void onChoiceParameters(ActionEvent event) {
        ComboBox<?> comboBox = (ComboBox<?>) event.getSource();

        ComboBoxItem selectedComboBoxItem;
        switch (comboBox.getId()) {
            case "videoBitRateComboBox" -> {
                selectedComboBoxItem = videoBitRateComboBox.getValue();
                properties.setVideoBitRate((nonNull(selectedComboBoxItem)) ? (int) selectedComboBoxItem.id() : -1);
            }
            case "audioBitRateComboBox" -> {
                selectedComboBoxItem = audioBitRateComboBox.getValue();
                properties.setAudioBitRate((nonNull(selectedComboBoxItem)) ? (int) selectedComboBoxItem.id() : -1);
            }
            case "channelsComboBox" -> {
                selectedComboBoxItem = channelsComboBox.getValue();
                properties.setChannel((nonNull(selectedComboBoxItem)) ? (int) selectedComboBoxItem.id() : -1);
            }
            case "samplingRateComboBox" -> {
                selectedComboBoxItem = samplingRateComboBox.getValue();
                properties.setSamplingRate((nonNull(selectedComboBoxItem)) ? (int) selectedComboBoxItem.id() : -1);
            }
            case "fpsComboBox" -> {
                selectedComboBoxItem = fpsComboBox.getValue();
                properties.setFps((nonNull(selectedComboBoxItem)) ? (int) selectedComboBoxItem.id() : -1);
            }
            default -> throw new IllegalArgumentException("Unknown id: " + comboBox.getId());
        }
    }

    @FXML
    public void onChoiceResolution() {
        properties.setResolution(resolutionComboBox.getValue());
    }

    @FXML
    private void onGPUSelected() {
        properties.setUseGPU(gpuCheckBox.isSelected());
    }

    @FXML
    private void onActionClickToggleBtnFormat(ActionEvent e) {
        hideSuccessMessage(labelSuccess, properties.getHideSuccessMessageTimer(), true);

        MediaHelper.selectFormat(e).ifPresentOrElse(
                format -> selectFormat(format.getExtension(), properties::setTargetFormat),
                () -> properties.setTargetFormat(null)
        );
        updateGifControls("gif".equalsIgnoreCase(properties.getTargetFormat()));
    }

    private int getSelectedItemValue(ComboBox<ComboBoxItem> comboBox) {
        return nonNull(comboBox) && nonNull(comboBox.getValue()) ? (int) comboBox.getValue().id() : -1;
    }

    private void resetPropertiesToDefault() {
        properties.setVideoBitRate(5000);
        properties.setAudioBitRate(192);
        properties.setChannel(2);
        properties.setSamplingRate(48000);
        properties.setFps(30);
        properties.setResolution("1920x1080");
        properties.setOutput(getSavedPath());
    }

    private void resetComboBox() {
        videoBitRateComboBox.setValue(new ComboBoxItem(5000, "V: 5000 kbps (1080p)"));
        audioBitRateComboBox.setValue(new ComboBoxItem(192, "A: 192 kbps"));
        channelsComboBox.setValue(new ComboBoxItem(2, "2 Channels"));
        samplingRateComboBox.setValue(new ComboBoxItem(48000, "48000 Hz"));
        fpsComboBox.setValue(new ComboBoxItem(30, "30 fps"));
        resolutionComboBox.setValue("1920x1080");
    }

    private void initComboBoxes() {
        ComboBoxes.setupComboBox(videoBitRateComboBox, ComboBoxItem::title);
        ComboBoxes.setupComboBox(audioBitRateComboBox, ComboBoxItem::title);
        ComboBoxes.setupComboBox(channelsComboBox,     ComboBoxItem::title);
        ComboBoxes.setupComboBox(samplingRateComboBox, ComboBoxItem::title);
        ComboBoxes.setupComboBox(fpsComboBox,          ComboBoxItem::title);

        samplingRateComboBox.getItems().addAll(createItems("Match source", " Hz", 8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000));
        fpsComboBox.getItems().addAll(createItems("Match source", " fps", 24, 30, 60));
        channelsComboBox.getItems().addAll(createItems("Match source", " Channels", 1, 2));

        videoBitRateComboBox.getItems().addAll(
                new ComboBoxItem(-1, "V: Match source"),
                new ComboBoxItem(1000, "V: 1000 kbps (SD)"),
                new ComboBoxItem(2500, "V: 2500 kbps (720p)"),
                new ComboBoxItem(5000, "V: 5000 kbps (1080p)"),
                new ComboBoxItem(8000, "V: 8000 kbps (High)")
        );

        audioBitRateComboBox.getItems().addAll(createItems("A: Match source",  "A: ","kbps", 96, 128, 192, 256, 320));
        resolutionComboBox.getItems().addAll("Match source", "1280x720", "1920x1080", "3840x2160"
        );
    }

    private record ConversionParams(
            int videoBitrate,
            int audioBitrate,
            int channels,
            int samplingRate,
            int fps,
            String resolution,
            boolean useGPU
    ) {}

    private void applyConversionParams(ConversionParams params) {
        MediaFormat targetFormat = MediaFormat.fromExtension(properties.getTargetFormat())
                .orElse(MediaFormat.MP4);

        properties.setVideoBitRate(params.videoBitrate());
        properties.setAudioBitRate(params.audioBitrate());
        properties.setChannel(params.channels());
        properties.setSamplingRate(params.samplingRate());
        properties.setFps(params.fps());
        properties.setResolution(params.resolution());
        properties.setUseGPU(params.useGPU());
        properties.setVideoCodec(MediaHelper.getVideoCodec(targetFormat, params.useGPU()));
        properties.setAudioCodec(targetFormat == MediaFormat.GIF ? null : MediaHelper.getAudioCodec(targetFormat, true));
        properties.setFfmpegFormat(MediaHelper.getFFmpegFormat(targetFormat));
        properties.setMediaType(MediaType.VIDEO);
    }

    private void updateGifControls(boolean gifSelected) {
        videoBitRateComboBox.setDisable(gifSelected);
        audioBitRateComboBox.setDisable(gifSelected);
        channelsComboBox.setDisable(gifSelected);
        samplingRateComboBox.setDisable(gifSelected);
        gpuCheckBox.setDisable(gifSelected);
        gifInfoLabel.setVisible(gifSelected);
        gifInfoLabel.setManaged(gifSelected);
    }

    private ConversionParams resolveConversionParams(MultimediaInfo sourceInfo) {
        int videoBitrate = getSelectedItemValue(videoBitRateComboBox);
        int audioBitrate = getSelectedItemValue(audioBitRateComboBox);
        int channel      = getSelectedItemValue(channelsComboBox);
        int samplingRate = getSelectedItemValue(samplingRateComboBox);
        int fps          = getSelectedItemValue(fpsComboBox);
        String resolution = nonNull(resolutionComboBox) ? resolutionComboBox.getValue() : properties.getResolution();

        int finalVideoBitrate = (videoBitrate == -1) ? parseVideoBitrate(sourceInfo) : videoBitrate;
        int finalAudioBitrate = (audioBitrate == -1) ? parseAudioBitrate(sourceInfo) : audioBitrate;
        int finalChannels     = (channel == -1) ? parseChannels(sourceInfo) : channel;
        int finalSamplingRate = (samplingRate == -1) ? parseSamplingRate(sourceInfo) : samplingRate;
        int finalFps          = (fps == -1) ? parseFps(sourceInfo) : fps;

        if (finalVideoBitrate <= 0) finalVideoBitrate = parseBitrate(sourceInfo);
        if (finalVideoBitrate <= 0) finalVideoBitrate = 5000;
        if (finalAudioBitrate <= 0) finalAudioBitrate = 192;
        if (finalSamplingRate <= 0) finalSamplingRate = 48000;
        if (finalChannels <= 0)     finalChannels = 2;
        if (finalFps <= 0)          finalFps = 30;

        boolean isWebm = "webm".equalsIgnoreCase(properties.getTargetFormat());
        if (isWebm && UNSUPPORTED_WEBM_RATES.contains(finalSamplingRate)) {
            Alerts.alertDialog(Alert.AlertType.WARNING,
                    "WARN",
                    "Sampling rate not supported",
                    "The sampling rate (" + finalSamplingRate + " Hz) is not supported for WEBM video format. Please choose another rate or format.");
            return null;
        }

        ErrorLogger.info(getClass(), String.format("Video conversion parameters: V-BR=%d, A-BR=%d, CH=%d, SR=%d, FPS=%d",
                finalVideoBitrate, finalAudioBitrate, finalChannels, finalSamplingRate, finalFps));

        String finalResolution = "Match source".equalsIgnoreCase(resolution)
                ? parseResolution(sourceInfo).orElse(null)
                : resolution;

        boolean useGPU = !"gif".equalsIgnoreCase(properties.getTargetFormat())
                && nonNull(gpuCheckBox) && gpuCheckBox.isSelected();

        return new ConversionParams(finalVideoBitrate, finalAudioBitrate, finalChannels, finalSamplingRate, finalFps, finalResolution, useGPU);
    }
}
