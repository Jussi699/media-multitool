package media_multitool.watermarks.viewController;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import media_multitool.watermarks.WatermarkImageController;
import media_multitool.watermarks.WatermarkPdfController;
import media_multitool.watermarks.WatermarkVideoController;
import model.helper.watermarks.WatermarkSettings;
import viewHelp.WorkColors;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.util.function.DoubleConsumer;

public class WatermarkTextController {
    @FXML private Label labelTitle;
    @FXML private TextField fieldText;
    @FXML private ComboBox<String> comboBoxFont, comboBoxEffect;
    @FXML private Slider sliderSizeText, sliderSpacing, sliderOpacity, sliderRotation;
    @FXML private Label labelSizeX, labelSpacingX, labelOpacityPercent, labelRotationDegree;
    @FXML private ToggleButton tileSingle, tileEvenGrid, tileDiamondMesh;
    @FXML private Button btnColorPicker;

    @Getter private WatermarkSettings settings;
    @Setter private WatermarkImageController mainImageController;
    @Setter private WatermarkPdfController mainPdfController;
    @Setter private WatermarkVideoController mainVideoController;

    private static final String DEFAULT_FONT = "Arial";
    private static final String DEFAULT_PATTERN = "single";

    private Color selectedColorFX = Color.WHITE;
    private JDialog swingDialog;
    private com.bric.colorpicker.ColorPicker swingColorPicker;

    @FXML
    public void initialize() {
        settings = new WatermarkSettings();
        settings.setType(WatermarkSettings.WatermarkType.TEXT);

        sliderSizeText.setMin(0.5);
        sliderSizeText.setValue(2.5);
        sliderSizeText.setMax(10);

        setupFontComboBox();
        setupEffectComboBox();
        setupSliders();
        setupTileButtons();
        setupDefaults();
        setupLiveUpdate();
    }

    /** Sets the title label text inside the window (Variant A shared-FXML approach). */
    public void setWindowTitle(String title) {
        if (labelTitle != null) {
            labelTitle.setText(title);
        }
    }

    public void loadSettings(WatermarkSettings settings) {
        if (settings == null || settings.getType() != WatermarkSettings.WatermarkType.TEXT) {
            return;
        }

        this.settings = settings.copy();

        fieldText.setText(settings.getText());
        comboBoxFont.setValue(settings.getFontName());
        sliderSizeText.setValue(settings.getFontSize());
        sliderSpacing.setValue(settings.getSpacing());
        sliderOpacity.setValue(settings.getOpacity());
        sliderRotation.setValue(settings.getRotation());

        java.awt.Color awtColor = settings.getTextColor();
        if (awtColor != null) {
            this.selectedColorFX = WorkColors.toFxColor(awtColor);
            WorkColors.updateColorView(awtColor, btnColorPicker);
            if (swingColorPicker != null) {
                swingColorPicker.setColor(awtColor);
            }
        }

        updateEffectCombo(settings.getEffect());
        updateTilePattern(settings.getTilePattern());
    }

    private void updateEffectCombo(String effect) {
        if (effect != null && !effect.isEmpty()) {
            String capitalized = effect.substring(0, 1).toUpperCase() + effect.substring(1).toLowerCase();
            comboBoxEffect.setValue(capitalized);
        }
    }

    private void updateTilePattern(String pattern) {
        switch (pattern) {
            case DEFAULT_PATTERN  -> tileSingle.setSelected(true);
            case "grid"    -> tileEvenGrid.setSelected(true);
            case "diamond" -> tileDiamondMesh.setSelected(true);
            default -> tileSingle.setSelected(true);
        }
    }

    private void setupFontComboBox() {
        String[] fontFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        comboBoxFont.getItems().addAll(fontFamilies);
        comboBoxFont.setValue(DEFAULT_FONT);
    }

    private void setupEffectComboBox() {
        comboBoxEffect.getItems().addAll("None", "Shadow", "Outline", "Glow");
        comboBoxEffect.setValue("None");
    }

    private void bindSlider(Slider slider, Label label, String format, DoubleConsumer setter) {
        slider.valueProperty().addListener((_, _, newVal) -> {
            label.setText(String.format(format, newVal.doubleValue()));
            settings.updatePreservingPosition(_ -> setter.accept(newVal.doubleValue()));
            updatePreview();
        });
    }

    private void setupSliders() {
        bindSlider(sliderSizeText, labelSizeX, "%.1fx", val -> settings.setFontSize(val));
        bindSlider(sliderSpacing, labelSpacingX, "%.1fx", val -> settings.setSpacing(val));
        bindSlider(sliderOpacity, labelOpacityPercent, "%.0f%%", val -> settings.setOpacity(val));
        bindSlider(sliderRotation, labelRotationDegree, "%.0f°", val -> settings.setRotation(val));
    }

    private void setupTileButtons() {
        ToggleGroup tileGroup = new ToggleGroup();
        tileSingle.setToggleGroup(tileGroup);
        tileEvenGrid.setToggleGroup(tileGroup);
        tileDiamondMesh.setToggleGroup(tileGroup);

        tileSingle.setSelected(true);

        tileGroup.selectedToggleProperty().addListener((_, _, newVal) -> {
            boolean isTiled = newVal != tileSingle;
            sliderSpacing.setDisable(!isTiled);
            labelSpacingX.setDisable(!isTiled);

            settings.updatePreservingPosition(s -> {
                if      (newVal == tileSingle)      s.setTilePattern(DEFAULT_PATTERN);
                else if (newVal == tileEvenGrid)    s.setTilePattern("grid");
                else if (newVal == tileDiamondMesh) s.setTilePattern("diamond");
            });

            updatePreview();
        });
    }

    private void setupDefaults() {
        fieldText.setText("Watermark");
        sliderSizeText.setValue(2.5);
        sliderSpacing.setValue(0);
        sliderOpacity.setValue(100);
        sliderRotation.setValue(0);
        sliderSpacing.setDisable(true);

        selectedColorFX = Color.WHITE;
        WorkColors.updateColorView(java.awt.Color.WHITE, btnColorPicker);
        if (swingColorPicker != null) {
            swingColorPicker.setColor(java.awt.Color.WHITE);
        }

        settings.setText("Watermark");
        settings.setFontName(DEFAULT_FONT);
        settings.setFontSize(2.5);
        settings.setTextColor(java.awt.Color.WHITE);
        settings.setOpacity(100);
        settings.setRotation(0);
        settings.setSpacing(0);
        settings.setTilePattern(DEFAULT_PATTERN);
        settings.setEffect("none");
    }

    private void setupLiveUpdate() {
        fieldText.textProperty().addListener((_, _, newVal) -> {
            settings.updatePreservingPosition(s -> s.setText(newVal));
            updatePreview();
        });

        comboBoxFont.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                settings.updatePreservingPosition(s -> s.setFontName(newVal));
                updatePreview();
            }
        });

        comboBoxEffect.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                settings.updatePreservingPosition(s -> s.setEffect(newVal.toLowerCase()));
                updatePreview();
            }
        });
    }

    private void updatePreview() {
        if (mainImageController != null) {
            mainImageController.updateWatermarkPreview(settings);
        }
        if (mainPdfController != null) {
            mainPdfController.updateWatermarkPreview(settings);
        }
        if (mainVideoController != null) {
            mainVideoController.updateWatermarkPreview(settings);
        }
    }

    @FXML
    private void handleResetSettings() {
        setupDefaults();
        tileSingle.setSelected(true);
        updatePreview();
    }

    public void resetToDefaults() {
        settings = new WatermarkSettings();
        settings.setType(WatermarkSettings.WatermarkType.TEXT);
        setupDefaults();
        tileSingle.setSelected(true);
        comboBoxFont.setValue(DEFAULT_FONT);
        comboBoxEffect.setValue("None");
    }

    @FXML
    private void handleColorChange() {
        if (swingDialog == null) {
            initSwingColorPicker();
            bindSwingDialogToStage();
        }
        if (swingColorPicker != null) {
            swingColorPicker.setColor(WorkColors.toAwtColor(selectedColorFX));
        }
        if (!swingDialog.isVisible()) {
            swingDialog.setVisible(true);
        } else {
            swingDialog.toFront();
        }
    }

    private void initSwingColorPicker() {
        swingDialog = new JDialog();
        swingDialog.setTitle("Select Color");
        swingDialog.setModal(false);
        swingDialog.setAlwaysOnTop(true);
        swingDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        swingColorPicker = new com.bric.colorpicker.ColorPicker(true, true);
        swingColorPicker.setColor(WorkColors.toAwtColor(selectedColorFX));

        swingColorPicker.addColorListener(colorModel -> {
            java.awt.Color newColor = colorModel.getColor();
            Platform.runLater(() -> {
                updateColorModel(newColor);
                WorkColors.updateColorView(newColor, btnColorPicker);
            });
        });

        swingDialog.add(swingColorPicker);
        swingDialog.pack();
    }

    private void bindSwingDialogToStage() {
        Platform.runLater(() -> {
            if (btnColorPicker.getScene() != null && btnColorPicker.getScene().getWindow() != null) {
                btnColorPicker.getScene().getWindow().addEventHandler(
                        javafx.stage.WindowEvent.WINDOW_HIDING,
                        _ -> {
                            if (swingDialog != null) swingDialog.dispose();
                        }
                );
            }
        });
    }

    private void updateColorModel(java.awt.Color awtColor) {
        this.selectedColorFX = WorkColors.toFxColor(awtColor);
        settings.updatePreservingPosition(s -> s.setTextColor(awtColor));
        updatePreview();
    }
}
