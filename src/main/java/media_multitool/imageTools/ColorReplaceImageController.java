package media_multitool.imageTools;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import media_multitool.AbstractImageToolController;
import model.helper.images.ColorReplaceHelper;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.utility.Global;
import model.utility.ResetContext;
import org.jspecify.annotations.NonNull;
import viewHelp.InfoAlert;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.DoubleConsumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;
import static viewHelp.Utility.setTextToTextField;

public class ColorReplaceImageController extends AbstractImageToolController {
    private final ImageProperties imageProperties = new ImageProperties();

    @FXML private Button btnSubmit;
    @FXML private ToggleButton toggleJPEG, togglePNG;
    @FXML private ImageView imageViewPreview;
    @FXML private StackPane previewContainer;
    @FXML private Spinner<Double> spinnerIntensity;
    @FXML private Spinner<Integer> spinnerSmoothing, spinnerEnhancement;
    @FXML private ComboBox<String> comboSourceColor, comboTargetColor;
    @FXML private CheckBox checkBoxReplaceAllColors;
    @FXML private TextField textSourceColorHex, textTargetColorHex;

    private List<Control> listControls;

    @Override
    protected ImageProperties getImageProperties() {
        return imageProperties;
    }

    @Override
    protected ImageView getImageView() {
        return imageViewPreview;
    }

    @FXML
    public void initialize() {
        listControls = List.of(
            comboSourceColor, comboTargetColor, textSourceColorHex,
            textTargetColorHex, spinnerIntensity, spinnerSmoothing,
            spinnerEnhancement, toggleJPEG, togglePNG, btnSubmit,
            checkBoxReplaceAllColors, btnSubmitAndCopy, btnReset
        );

        btnChooseSaveDirectory.setTooltip(new Tooltip("Default directory: Desktop"));
        imageProperties.setOutput(getSavedPath());

        togglePNG.setSelected(true);
        imageProperties.setTypeImage("png");

        setupTooltips();
        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(() -> processedImage, "Color-replaced");
        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);

        initializeColorCombos();
        initializeSpinners();
        initializeToggleGroup();
        initializeCheckBox();

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfo(
                "Color Replace",
                """
                        3. Choose target color from the list or enter a HEX code;

                        4. (Optional) Enable "Replace All Colors" to shift all colors, or configure source color;

                        5. Adjust Intensity, Smoothing, and Enhancement settings;

                        6. Select output format (JPEG or PNG);

                        7. Click "Replace and Download";
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, progressBar, true, "image"
        );
        reset(imageProperties, ctx, "Selected image file: none");

        processedImage = null;
        originalImage = null;

        comboSourceColor.setValue("Red");
        comboTargetColor.setValue("Blue");
        setTextToTextField(textSourceColorHex, "#FF0000");
        setTextToTextField(textTargetColorHex, "#0000FF");

        spinnerIntensity.getValueFactory().setValue(50.0);
        spinnerSmoothing.getValueFactory().setValue(5);
        spinnerEnhancement.getValueFactory().setValue(50);

        togglePNG.setSelected(true);
        toggleJPEG.setSelected(false);
        checkBoxReplaceAllColors.setSelected(false);

        disableControls();
    }

    @Override
    protected void generatePreview() {
        if (isNull(originalImage)) {
            return;
        }

        if (isNull(processedImage)) {
            processedImage = originalImage;
            setImagePreview(processedImage, imageViewPreview);
        }

        String targetHex = getTargetColorHex();
        if (!ColorReplaceHelper.isValidHex(targetHex)) {
            return;
        }

        executeReplaceColor();
    }

    @Override
    protected BufferedImage getFinalImageForDownload(DoubleConsumer progressUpdater) {
        BufferedImage finalImage = processColorReplacement();
        if (isNull(finalImage)) {
            throw new IllegalStateException("Original image is null or color replacement failed");
        }
        return finalImage;
    }

    @Override
    protected void lockUI() {
        btnSelectFile.setDisable(true);
        btnChooseSaveDirectory.setDisable(true);
        btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSelectFile.setDisable(false);
        btnChooseSaveDirectory.setDisable(false);
        btnReset.setDisable(false);
    }

    @Override
    protected void disableControls() {
        listControls.forEach(c -> c.setDisable(true));
    }

    @Override
    protected void enableControls() {
        listControls.forEach(c -> c.setDisable(false));
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info(getClass(), "Color replacement successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Color replaced and saved!", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @Override
    protected void handleTaskFailure(@NonNull Throwable exception) {
        super.handleTaskFailure(exception);
        Platform.runLater(() -> {
            showErrorMessage(labelSuccess, "Error: " + exception.getMessage(), imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    private void executeReplaceColor() {
        CompletableFuture.runAsync(() -> {
            try {
                BufferedImage result = processColorReplacement();
                if (nonNull(result)) {
                    processedImage = result;
                    Platform.runLater(() -> setImagePreview(processedImage, imageViewPreview));
                }
            } catch (Exception e) {
                ErrorLogger.error("Error updating preview: " + e.getMessage());
            }
        });
    }

    private BufferedImage processColorReplacement() {
        if (isNull(originalImage)) {
            return null;
        }

        String targetHex = getTargetColorHex();
        if (!ColorReplaceHelper.isValidHex(targetHex)) {
            return originalImage;
        }

        double intensity = getSpinnerValueIntensity();
        int enhancement = getSpinnerValueEnhancement();

        if (checkBoxReplaceAllColors.isSelected()) {
            return ColorReplaceHelper.replaceAllColors(originalImage, targetHex, intensity, enhancement);
        } else {
            String sourceHex = textSourceColorHex.getText();
            if (!ColorReplaceHelper.isValidHex(sourceHex)) {
                return originalImage;
            }
            int smoothing = spinnerSmoothing.getValue();
            return ColorReplaceHelper.replaceColor(originalImage, sourceHex, targetHex, intensity, smoothing, enhancement);
        }
    }

    private void initializeColorCombos() {
        String[] colors = ColorReplaceHelper.getAvailableColorNames();

        comboSourceColor.getItems().addAll(colors);
        comboTargetColor.getItems().addAll(colors);

        comboSourceColor.setValue("Red");
        comboTargetColor.setValue("Blue");

        setTextToTextField(textSourceColorHex, "#FF0000");
        setTextToTextField(textTargetColorHex, "#0000FF");

        comboSourceColor.setOnAction(_ -> {
            String color = comboSourceColor.getValue();
            if (nonNull(color)) {
                textSourceColorHex.setText(ColorReplaceHelper.getHexFromColorName(color));
            }
        });

        comboTargetColor.setOnAction(_ -> {
            String color = comboTargetColor.getValue();
            if (nonNull(color)) {
                textTargetColorHex.setText(ColorReplaceHelper.getHexFromColorName(color));
            }
        });

        textSourceColorHex.textProperty().addListener((_, _, newValue) -> {
            if (ColorReplaceHelper.isValidHex(newValue)) {
                generatePreview();
            }
        });

        textTargetColorHex.textProperty().addListener((_, _, newValue) -> {
            if (ColorReplaceHelper.isValidHex(newValue)) {
                generatePreview();
            }
        });
    }

    private void initializeSpinners() {
        SpinnerValueFactory<Double> intensityFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(1.0, 100.0, 50.0, 1.0);
        spinnerIntensity.setValueFactory(intensityFactory);
        spinnerIntensity.setEditable(true);

        SpinnerValueFactory<Integer> smoothingFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50, 5, 1);
        spinnerSmoothing.setValueFactory(smoothingFactory);
        spinnerSmoothing.setEditable(true);

        SpinnerValueFactory<Integer> enhancementFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 50, 1);
        spinnerEnhancement.setValueFactory(enhancementFactory);
        spinnerEnhancement.setEditable(true);

        spinnerIntensity.valueProperty().addListener((_, _, _) -> generatePreview());
        spinnerSmoothing.valueProperty().addListener((_, _, _) -> generatePreview());
        spinnerEnhancement.valueProperty().addListener((_, _, _) -> generatePreview());
    }

    private void initializeToggleGroup() {
        ToggleGroup formatGroup = new ToggleGroup();
        toggleJPEG.setToggleGroup(formatGroup);
        togglePNG.setToggleGroup(formatGroup);
        togglePNG.setSelected(true);

        formatGroup.selectedToggleProperty().addListener((_, _, newToggle) -> {
            if (newToggle == toggleJPEG) {
                imageProperties.setTypeImage("jpeg");
            } else if (newToggle == togglePNG) {
                imageProperties.setTypeImage("png");
            }
        });
    }

    private void initializeCheckBox() {
        checkBoxReplaceAllColors.setSelected(false);
        checkBoxReplaceAllColors.selectedProperty().addListener((_, _, newValue) -> {
            comboSourceColor.setDisable(newValue);
            textSourceColorHex.setDisable(newValue);
            spinnerSmoothing.setDisable(newValue);
            generatePreview();
        });
    }

    private String getTargetColorHex() {
        return textTargetColorHex.getText();
    }

    private double getSpinnerValueIntensity() {
        return spinnerIntensity.getValue();
    }

    private int getSpinnerValueEnhancement() {
        return spinnerEnhancement.getValue();
    }
}
