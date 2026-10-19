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
import model.utility.format.SupportedFormat;
import model.utility.context.ResetContext;
import visual.controls.ComboBoxes;
import visual.dialogs.InfoAlert;
import ws.schild.jave.info.MultimediaInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static model.utility.media.MediaParsers.*;
import static model.utility.io.PathWorker.getSavedPath;
import static visual.controls.Message.hideSuccessMessage;
import static visual.controls.Message.setupClearMessageTimer;

public class ConverterAudioController extends AbstractAudioVideoConverterController {
    @FXML private Button btnSelectAudioVideoFile, btnChoiceDirForSave;
    @FXML private ToggleButton btnToMP3, btnToAAC, btnToOggVorbis;
    @FXML private ToggleButton btnToOPUS, btnToFLAC, btnToALAC;
    @FXML private ToggleButton btnToWAV, btnToAIFF;
    @FXML private ComboBox<String> bitRateComboBox, channelComboBox, samplingRateComboBox;
    @FXML private CheckBox lossyCompresionCheckBox;
    @FXML private VBox parametersContainer;

    @Override
    protected String getFileFilterDescription() {
        return "All Media Files";
    }

    @Override
    protected MediaType getFileCategoryName() {
        return MediaType.AUDIO;
    }

    @Override
    protected List<String> getSupportedInputFormats() {
        return Stream.of(SupportedFormat.getAllSupportedAudioFormats(), SupportedFormat.getAllSupportedVideoFormats())
                .flatMap(Collection::stream)
                .toList();
    }

    @Override
    protected List<String> getSupportedFileChooserFormats() {
        List<String> allFilters = new ArrayList<>(SupportedFormat.getSupportedAudioFormatsForFileChooser());
        allFilters.addAll(SupportedFormat.getSupportedVideoFormatsForFileChooser());
        return allFilters;
    }

    @FXML
    public void initialize() {
        properties.setOutput(getSavedPath());

        initToggleGroup();
        initComboBoxes();

        setupClearMessageTimer(labelSuccess, progressBar, properties.getHideSuccessMessageTimer(), true);

        lossyCompresionCheckBox.setSelected(false);
        lossyCompresionCheckBox.setDisable(true);

        isPressedReset();

        setupDragAndDrop(dropZone, getSupportedInputFormats(), this::loadFile);
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "Converter Audio",
                "1. Select an audio or video file using \"Select audio/video\" or drag and drop it into the dash-bordered zone;",
                """
                        3. Select the target audio format (MP3, AAC, OGG, etc.);

                        4. Configure quality settings (Bitrate, Channels, Sampling Rate);

                        5. (Optional) Enable "Use Lossy Compression (AAC)" if needed;

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
                dropZone, null, progressBar, true, "audio/video"
        );
        reset(properties, ctx, "Selected media file: none");

        hideSuccessMessage(labelSuccess, properties.getHideSuccessMessageTimer(), true);
        disableControls();

        bitRateComboBox.setValue("320 kbps");
        channelComboBox.setValue("2 Channels");
        samplingRateComboBox.setValue("48000 Hz");
        properties.setAudioBitRate(320);
        properties.setChannel(2);
        properties.setSamplingRate(48000);
        progressBar.setProgress(0);

        clearToggleGroup(toggleGroup);

        lossyCompresionCheckBox.setSelected(false);
        lossyCompresionCheckBox.setDisable(true);
    }

    @FXML
    public void onChoiceComboBox(ActionEvent event) {
        ComboBox<?> comboBox = (ComboBox<?>) event.getSource();

        switch (comboBox.getId()) {
            case "bitRateComboBox"      -> properties.setAudioBitRate(parseComboBoxStringToInt(bitRateComboBox));
            case "channelComboBox"     -> properties.setChannel(parseComboBoxStringToInt(channelComboBox));
            case "samplingRateComboBox" -> properties.setSamplingRate(parseComboBoxStringToInt(samplingRateComboBox));
            default -> throw new IllegalArgumentException("Unknown id: " + comboBox);
        }
    }

    @FXML
    private void onActionClickToggleBtnFormat(ActionEvent e) {
        hideSuccessMessage(labelSuccess, properties.getHideSuccessMessageTimer(), true);

        MediaHelper.selectFormat(e).ifPresentOrElse(format -> {
            selectFormat(format.getExtension(), properties::setTargetFormat);

            boolean supportsChoice = MediaHelper.supportsCodecChoice(format);
            lossyCompresionCheckBox.setDisable(!supportsChoice);
            if (supportsChoice) {
                lossyCompresionCheckBox.setSelected(false);
            }
        }, () -> {
            properties.setTargetFormat(null);
            lossyCompresionCheckBox.setDisable(true);
        });
    }

    @Override
    protected void lockUI() {
        toggleUI(true);
    }

    @Override
    protected void unlockUI() {
        toggleUI(false);
        updateLossyCheckboxState();
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
        updateLossyCheckboxState();
    }

    @Override
    protected void toggleUI(boolean flag) {
        toggleControls(flag);
        setControlsDisabled(flag, parametersContainer, btnSelectAudioVideoFile, btnChoiceDirForSave);
        dropZone.setDisable(flag);
        btnCancelConversion.setDisable(!flag);
    }

    @Override
    protected void toggleControls(boolean flag) {
        setControlsDisabled(flag, parametersContainer, btnSubmitAndDownload, btnReset);
        btnCancelConversion.setDisable(true);
    }


    @Override
    protected void executeConversion(MultimediaInfo sourceInfo) {
        MediaFormat targetFormat = MediaFormat.fromExtension(properties.getTargetFormat())
                .orElse(MediaFormat.MP3);

        int finalAudioBitrate = properties.getAudioBitRate();
        int finalChannels     = properties.getChannel();
        int finalSamplingRate = properties.getSamplingRate();

        if (finalAudioBitrate <= 0) finalAudioBitrate = parseAudioBitrate(sourceInfo);
        if (finalAudioBitrate <= 0) finalAudioBitrate = 320;

        if (finalChannels <= 0) finalChannels = nonNull(sourceInfo) ? parseChannels(sourceInfo) : 2;
        if (finalChannels <= 0) finalChannels = 2;

        if (finalSamplingRate <= 0) finalSamplingRate = nonNull(sourceInfo) ? parseSamplingRate(sourceInfo) : 48000;
        if (finalSamplingRate <= 0) finalSamplingRate = 48000;

        properties.setAudioBitRate(finalAudioBitrate);
        properties.setChannel(finalChannels);
        properties.setSamplingRate(finalSamplingRate);

        properties.setAudioCodec(MediaHelper.getAudioCodec(targetFormat, lossyCompresionCheckBox.isSelected()));
        properties.setFfmpegFormat(MediaHelper.getFFmpegFormat(targetFormat));
        properties.setMediaType(MediaType.AUDIO);

        ConverterVideoAudioFile converter = new ConverterVideoAudioFile();
        currentTask = new ConvertVideoAudioTask(converter, properties, MediaType.AUDIO);

        executeMediaTask(currentTask);
    }

    private void updateLossyCheckboxState() {
        MediaFormat.fromExtension(properties.getTargetFormat()).ifPresentOrElse(
                format -> lossyCompresionCheckBox.setDisable(!MediaHelper.supportsCodecChoice(format)),
                () -> lossyCompresionCheckBox.setDisable(true)
        );
    }

    private void initToggleGroup() {
        configureToggleGroup(toggleGroup, btnToMP3, btnToAAC, btnToOggVorbis, btnToOPUS,
                btnToFLAC, btnToALAC, btnToWAV, btnToAIFF);
    }

    private void initComboBoxes() {
        ComboBoxes.setupStringComboBox(bitRateComboBox);
        ComboBoxes.setupStringComboBox(channelComboBox);
        ComboBoxes.setupStringComboBox(samplingRateComboBox);

        bitRateComboBox.getItems().addAll("128 kbps", "192 kbps", "256 kbps", "320 kbps");
        channelComboBox.getItems().addAll("1 Channels", "2 Channels");
        samplingRateComboBox.getItems().addAll(
                "8000 Hz", "11025 Hz", "12000 Hz",
                "16000 Hz", "22050 Hz", "24000 Hz",
                "32000 Hz", "44100 Hz", "48000 Hz");
    }
}
